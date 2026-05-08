# JPA String @Id Bug Fix - Complete Summary

## Problem Statement

Tests TC-ACC-CTRL-007 and TC-ACC-CTRL-012 were failing with:
```
org.springframework.dao.DataIntegrityViolationException: could not execute statement 
[ERROR: null value in column "password_hash" of relation "accounts" violates not-null constraint]
[update Accounts set ... password_hash=null ... where Username=?]
```

The error shows Hibernate is executing **UPDATE** instead of **INSERT** for new accounts, and the password_hash is null.

## Root Cause Analysis

### 1. Spring Data JPA's `isNew()` Logic

For entities with String `@Id` (not auto-generated):
```java
// SimpleJpaRepository.save():
if (entityInformation.isNew(entity)) {
    em.persist(entity);   // INSERT
} else {
    return em.merge(entity);  // UPDATE or SELECT+UPDATE
}
```

**Problem**: String @Id that's not null → `isNew()` returns `false` → always calls `merge()`

### 2. `merge()` Behavior with REMOVED Entities

When a test:
1. Deletes "newuser" using `accountRepository.deleteById("newuser")`
2. Creates "newuser" again in the same transaction

Hibernate's session cache still has "newuser" in **REMOVED** state.

Calling `merge()` on a REMOVED entity:
- Hibernate tries to re-attach the entity
- Executes **UPDATE** instead of **INSERT**
- Uses data from the REMOVED entity (which has `password_hash=null` after controller modification)

### 3. Controller Modification After Persist

```java
// ApiAdminAccountController.createAccount()
Account account = accountService.createAccount(entity, photoFile);
account.setPasswordHash(null);  // For security - don't return hash in response
```

If the entity is still **managed** after `createAccount()` returns, setting `passwordHash=null` marks it as dirty. The next database query triggers auto-flush, executing UPDATE with `password_hash=null`, violating the NOT NULL constraint.

## Complete Fix (3 Layers)

### Layer 1: Entity - Implement `Persistable<String>`

**File**: `Java_5_Jenka_Coffee/src/main/java/com/springboot/jenka_coffee/entity/Account.java`

```java
@Entity
@Table(name = "Accounts")
public class Account implements Serializable, Persistable<String> {
    
    @Id
    @Column(name = "Username", length = 50)
    private String username;
    
    /**
     * JPA FIX: String @Id causes merge() instead of persist()
     * Implement Persistable to explicitly control isNew() behavior
     */
    @Transient
    private boolean isNew = false;
    
    @Override
    public String getId() {
        return username;
    }
    
    @Override
    public boolean isNew() {
        return isNew;
    }
    
    /**
     * Reset isNew flag after persist/load to prevent re-insertion
     */
    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
```

**Why**: This tells Spring Data JPA explicitly when an entity is new, overriding the default logic.

### Layer 2: Service - Use `entityManager.persist()` Directly

**File**: `Java_5_Jenka_Coffee/src/main/java/com/springboot/jenka_coffee/service/impl/AccountServiceImpl.java`

#### Fix 2A: `createAccount()` Method

```java
@Override
public Account createAccount(Account account, MultipartFile photoFile) {
    // JPA FIX: Mark entity as new FIRST, before any validation
    account.setNew(true);
    
    log.debug("Creating account '{}' with isNew={}", account.getUsername(), account.isNew());
    
    // ... validation and password hashing ...
    
    // JPA FIX: Use entityManager.persist() directly to force INSERT
    entityManager.persist(account);
    entityManager.flush();   // Execute INSERT immediately
    
    // CRITICAL: Detach to prevent controller's setPasswordHash(null) from triggering UPDATE
    entityManager.detach(account);
    
    // Verify detachment
    if (entityManager.contains(account)) {
        log.error("CRITICAL: Account '{}' is still managed after detach()!", account.getUsername());
    }
    
    return account;
}
```

#### Fix 2B: `save()` Method (for OAuth and other paths)

```java
@Override
public Account save(Account account) {
    // If account is marked as new, use persist() to ensure INSERT
    if (account.isNew()) {
        entityManager.persist(account);
        entityManager.flush();
        entityManager.detach(account);
        return account;
    }
    // Otherwise use normal save (merge for updates)
    return dao.save(account);
}
```

**Why**: 
- `persist()` forces INSERT, bypassing Spring Data's `isNew()` check
- `flush()` executes the INSERT immediately
- `detach()` removes entity from persistence context, preventing auto-flush on subsequent modifications

### Layer 3: Test - Aggressive Cleanup

**File**: `Java_5_Jenka_Coffee/src/test/java/com/springboot/jenka_coffee/api/admin/ApiAdminAccountControllerTest.java`

#### Fix 3A: `setUp()` Method

```java
@BeforeEach
void setUp() {
    // AGGRESSIVE CLEANUP: Use native SQL to bypass Hibernate cache
    entityManager.createNativeQuery(
        "DELETE FROM Accounts WHERE Username IN ('user1', 'admin1', 'admin2', 'hacker', 'newuser', 'newuser2', 'brandnew')"
    ).executeUpdate();
    
    // CRITICAL: Flush and clear to remove REMOVED entities from cache
    entityManager.flush();
    entityManager.clear();
    
    // Verify "newuser" is deleted
    Long count = (Long) entityManager.createNativeQuery(
        "SELECT COUNT(*) FROM Accounts WHERE Username = 'newuser'"
    ).getSingleResult();
    assertEquals(0L, count, "newuser should be deleted before test");
    
    // Create test fixtures
    Account testUser = new Account();
    testUser.setUsername("user1");
    testUser.setFullname("Test User");
    testUser.setEmail("user1@test.com");
    testUser.setPasswordHash(passwordSecurity.hashPassword("Password@123"));
    testUser.setActivated(true);
    testUser.setAdmin(false);
    testUser.setNew(true);  // Mark as new
    accountRepository.save(testUser);
    
    // ... create admin1 and admin2 similarly ...
    
    entityManager.flush();
    entityManager.clear();  // Clear again after setup
}
```

#### Fix 3B: Test Assertions

```java
@Test
@WithMockUser(roles = "ADMIN")
@DisplayName("TC-ACC-CTRL-007: CREATE valid multipart data")
void test_createAccount_valid_returnsCreated() throws Exception {
    mockMvc.perform(multipart("/api/admin/accounts")
            .param("username", "newuser")
            .param("fullname", "New User")
            .param("email", "new@test.com")
            .param("password", "Pass@123"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.username").value("newuser"));

    // CRITICAL: Clear session before DB check
    entityManager.flush();
    entityManager.clear();
    
    // Verify account was created correctly
    Account saved = accountRepository.findById("newuser")
            .orElseThrow(() -> new AssertionError("Account not created"));
    
    assertEquals("newuser", saved.getUsername());
    assertEquals("New User", saved.getFullname());
    assertNotNull(saved.getPasswordHash(), "Password must be BCrypt hashed");
    assertTrue(saved.getPasswordHash().startsWith("$2a$"), "Password must be BCrypt format");
}
```

**Why**:
- Native SQL DELETE bypasses Hibernate cache completely
- `entityManager.clear()` removes all entities from session, including REMOVED ones
- Clearing before assertions prevents auto-flush of detached entities

## Why All Three Layers Are Required

| Layer | Without This Fix | Result |
|-------|------------------|--------|
| Entity only | `dao.save()` still calls `merge()` | UPDATE instead of INSERT |
| Service only | Entity not marked as new | Spring Data calls `merge()` |
| Service + Entity | Controller modifies managed entity | Auto-flush UPDATE with null password |
| All except test cleanup | REMOVED entity in cache | `merge()` re-attaches and UPDATEs |

## Other Affected Code Paths

### OAuth Account Creation

**File**: `Java_5_Jenka_Coffee/src/main/java/com/springboot/jenka_coffee/api/ApiAuthController.java`

```java
// Google OAuth login - create new account
account = new Account();
account.setUsername(username);
account.setEmail(email);
// ... set other fields ...
account.setNew(true);  // ✅ Already fixed
accountService.save(account);  // ✅ Now uses persist() when isNew=true
```

### Registration Flow

**File**: `Java_5_Jenka_Coffee/src/main/java/com/springboot/jenka_coffee/service/impl/AccountServiceImpl.java`

```java
@Override
public void register(String username, String fullname, String phone, String email, String password) {
    Account newAccount = new Account();
    newAccount.setUsername(username.trim());
    // ... set other fields ...
    createAccount(newAccount, null);  // ✅ Uses fixed createAccount()
}
```

## Testing Verification

To verify the fix works:

```bash
# Run the two failing tests
./mvnw test -Dtest=ApiAdminAccountControllerTest#test_createAccount_valid_returnsCreated
./mvnw test -Dtest=ApiAdminAccountControllerTest#test_createAccount_adminTrueInBody_forcesAdminFalse

# Run all account controller tests
./mvnw test -Dtest=ApiAdminAccountControllerTest

# Check logs for verification
# Should see:
# - "Creating account 'newuser' with isNew=true"
# - "Successfully persisted account 'newuser', detached=true"
# - No UPDATE statements in SQL logs
# - Only INSERT statements for new accounts
```

## Expected Behavior After Fix

1. **New account creation**: Executes `INSERT` statement, not `UPDATE`
2. **Password hash**: Saved correctly in database, not null
3. **Response**: Returns account without password_hash (security)
4. **Database state**: Account persisted with all fields correct
5. **Test isolation**: Each test starts with clean state, no REMOVED entities in cache

## References

- [Spring Data JPA - Persistable Interface](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/domain/Persistable.html)
- [Hibernate - Entity Lifecycle](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#pc-managed)
- [JPA - EntityManager.persist() vs merge()](https://www.baeldung.com/jpa-persist-vs-merge)

## Lessons Learned

1. **String @Id is a JPA trap**: Always implement `Persistable<String>` for String primary keys
2. **Test isolation matters**: Use native SQL and `clear()` to ensure clean state
3. **Detachment is critical**: Detach entities before returning them to controllers that modify them
4. **All layers must cooperate**: Entity, Service, and Test must all be fixed together
5. **Hibernate cache is persistent**: REMOVED entities stay in cache until `clear()` is called
