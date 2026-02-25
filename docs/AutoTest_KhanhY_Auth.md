# 🤖 TEST SCRIPTS TỰ ĐỘNG - KHÁNH Ý (Authentication Module)

**Thành viên:** Khánh Ý  
**Module:** Authentication & Authorization  
**Số lượng:** 2 Test Scripts  
**Công nghệ:** Selenium WebDriver + JUnit 5

---

## 📋 DANH SÁCH TEST SCRIPTS

| ID | Tên Test Script | Mô tả | URL Test |
|---|---|---|---|
| **TS_AUTH_001** | LoginTest | Test đăng nhập, logout, remember me | `/auth/login` |
| **TS_AUTH_002** | SignupTest | Test đăng ký tài khoản mới | `/auth/signup` |

---

## ✅ TEST SCRIPT 1: LoginTest.java

```java
package com.springboot.jenka_coffee.selenium.auth;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Login")
public class LoginTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_AUTH_001: Đăng nhập thành công (Admin)")
    public void testLoginAsAdmin() {
        // Step 1: Vào trang login
        driver.get(baseUrl + "/auth/login");
        
        // Step 2: Verify form login hiển thị
        WebElement usernameInput = driver.findElement(By.name("username"));
        WebElement passwordInput = driver.findElement(By.name("password"));
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        
        assertNotNull(usernameInput, "Username input should exist");
        assertNotNull(passwordInput, "Password input should exist");
        assertNotNull(submitBtn, "Submit button should exist");
        
        // Step 3: Nhập thông tin đăng nhập
        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("123");
        
        System.out.println("🔐 Đăng nhập với: admin / 123");
        
        // Step 4: Click đăng nhập
        submitBtn.click();
        
        // Step 5: Wait for redirect
        wait.until(ExpectedConditions.urlContains("/admin"));
        
        // Step 6: Verify đã chuyển sang trang admin
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/admin"),
                "Should redirect to admin dashboard");
        
        // Step 7: Verify có hiển thị tên user
        WebElement userMenu = driver.findElement(By.cssSelector(".user-menu, .username"));
        String userText = userMenu.getText();
        assertTrue(userText.contains("admin"),
                "Should display username");
        
        System.out.println("✅ Đăng nhập admin thành công");
    }
    
    @Test
    @DisplayName("TS_AUTH_002: Đăng nhập thành công (User)")
    public void testLoginAsUser() {
        // Step 1: Vào trang login
        driver.get(baseUrl + "/auth/login");
        
        // Step 2: Đăng nhập với user thường
        driver.findElement(By.name("username")).sendKeys("user1");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        // Step 3: Wait for redirect
        wait.until(ExpectedConditions.urlContains("/home"));
        
        // Step 4: Verify chuyển sang trang chủ (không phải admin)
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/home") || currentUrl.equals(baseUrl + "/"),
                "Should redirect to home page");
        
        System.out.println("✅ Đăng nhập user thành công");
    }
    
    @Test
    @DisplayName("TS_AUTH_003: Đăng nhập sai mật khẩu")
    public void testLoginWithWrongPassword() {
        // Step 1: Vào trang login
        driver.get(baseUrl + "/auth/login");
        
        // Step 2: Nhập sai password
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("wrongpassword");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        // Step 3: Wait for error message
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".error-message, .alert-danger")));
        
        // Step 4: Verify vẫn ở trang login
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/auth/login"),
                "Should stay on login page");
        
        // Step 5: Verify có thông báo lỗi
        WebElement errorMsg = driver.findElement(
                By.cssSelector(".error-message, .alert-danger"));
        String errorText = errorMsg.getText();
        
        assertTrue(errorText.contains("sai") || errorText.contains("incorrect"),
                "Should show error message");
        
        System.out.println("❌ Lỗi: " + errorText);
        System.out.println("✅ Test sai password: PASSED");
    }
    
    @Test
    @DisplayName("TS_AUTH_004: Đăng nhập với Remember Me")
    public void testLoginWithRememberMe() {
        // Step 1: Vào trang login
        driver.get(baseUrl + "/auth/login");
        
        // Step 2: Check "Remember Me"
        WebElement rememberCheckbox = driver.findElement(By.name("remember"));
        if (!rememberCheckbox.isSelected()) {
            rememberCheckbox.click();
        }
        
        // Step 3: Đăng nhập
        driver.findElement(By.name("username")).sendKeys("user1");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/home"));
        
        // Step 4: Verify có cookie "rememberMe"
        Cookie rememberCookie = driver.manage().getCookieNamed("rememberMe");
        assertNotNull(rememberCookie, "Should have rememberMe cookie");
        
        System.out.println("🍪 Cookie: " + rememberCookie.getValue());
        System.out.println("✅ Test Remember Me: PASSED");
    }
    
    @Test
    @DisplayName("TS_AUTH_005: Đăng xuất")
    public void testLogout() {
        // Prerequisite: Đăng nhập trước
        driver.get(baseUrl + "/auth/login");
        driver.findElement(By.name("username")).sendKeys("user1");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/home"));
        
        // Step 1: Click nút Logout
        WebElement logoutBtn = driver.findElement(
                By.cssSelector("a[href*='/auth/logout']"));
        logoutBtn.click();
        
        // Step 2: Wait for redirect
        wait.until(ExpectedConditions.urlContains("/home"));
        
        // Step 3: Verify không còn hiển thị user menu
        boolean userMenuExists = driver.findElements(
                By.cssSelector(".user-menu")).size() > 0;
        
        assertFalse(userMenuExists, "User menu should not exist after logout");
        
        // Step 4: Verify có nút "Đăng nhập"
        WebElement loginBtn = driver.findElement(
                By.cssSelector("a[href*='/auth/login']"));
        assertNotNull(loginBtn, "Should show login button");
        
        System.out.println("✅ Test logout: PASSED");
    }
    
    @Test
    @DisplayName("TS_AUTH_006: User truy cập trang Admin (Chặn)")
    public void testUserAccessAdminPage() {
        // Step 1: Đăng nhập với user thường
        driver.get(baseUrl + "/auth/login");
        driver.findElement(By.name("username")).sendKeys("user1");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/home"));
        
        // Step 2: Cố truy cập trang admin
        driver.get(baseUrl + "/admin");
        
        // Step 3: Verify bị chặn
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/auth/unauthorized"),
                ExpectedConditions.urlContains("/auth/login")
        ));
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/unauthorized") || currentUrl.contains("/login"),
                "Should be blocked from accessing admin page");
        
        System.out.println("🚫 Đã chặn user truy cập admin");
        System.out.println("✅ Test authorization: PASSED");
    }
}
```

---

## ✅ TEST SCRIPT 2: SignupTest.java

```java
package com.springboot.jenka_coffee.selenium.auth;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Signup")
public class SignupTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_AUTH_007: Đăng ký tài khoản thành công")
    public void testSignupSuccess() {
        // Step 1: Vào trang đăng ký
        driver.get(baseUrl + "/auth/signup");
        
        // Step 2: Điền form đăng ký
        String timestamp = String.valueOf(System.currentTimeMillis());
        String username = "user_" + timestamp;
        String email = "user" + timestamp + "@test.com";
        
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("fullname")).sendKeys("Nguyễn Test");
        driver.findElement(By.name("phone")).sendKeys("0901234567");
        driver.findElement(By.name("email")).sendKeys(email);
        driver.findElement(By.name("password")).sendKeys("123456");
        driver.findElement(By.name("confirmPassword")).sendKeys("123456");
        
        System.out.println("📝 Đăng ký: " + username + " / " + email);
        
        // Step 3: Submit form
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        
        // Step 4: Wait for redirect
        wait.until(ExpectedConditions.urlContains("/auth/login"));
        
        // Step 5: Verify chuyển sang trang login
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/auth/login"),
                "Should redirect to login page after signup");
        
        // Step 6: Verify có thông báo thành công
        WebElement successMsg = driver.findElement(
                By.cssSelector(".success-message, .alert-success"));
        String msgText = successMsg.getText();
        
        assertTrue(msgText.contains("thành công") || msgText.contains("success"),
                "Should show success message");
        
        System.out.println("✅ Đăng ký thành công: " + msgText);
    }
    
    @Test
    @DisplayName("TS_AUTH_008: Đăng ký với username đã tồn tại")
    public void testSignupWithExistingUsername() {
        // Step 1: Vào trang đăng ký
        driver.get(baseUrl + "/auth/signup");
        
        // Step 2: Nhập username đã tồn tại
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("fullname")).sendKeys("Test User");
        driver.findElement(By.name("phone")).sendKeys("0901234567");
        driver.findElement(By.name("email")).sendKeys("newuser@test.com");
        driver.findElement(By.name("password")).sendKeys("123456");
        driver.findElement(By.name("confirmPassword")).sendKeys("123456");
        
        // Step 3: Submit
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        // Step 4: Wait for error
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".error-message, .alert-danger")));
        
        // Step 5: Verify có thông báo lỗi
        WebElement errorMsg = driver.findElement(
                By.cssSelector(".error-message, .alert-danger"));
        String errorText = errorMsg.getText();
        
        assertTrue(errorText.contains("tồn tại") || errorText.contains("exists"),
                "Should show 'username exists' error");
        
        System.out.println("❌ Lỗi: " + errorText);
        System.out.println("✅ Test username trùng: PASSED");
    }
    
    @Test
    @DisplayName("TS_AUTH_009: Đăng ký với password không khớp")
    public void testSignupWithMismatchedPassword() {
        // Step 1: Vào trang đăng ký
        driver.get(baseUrl + "/auth/signup");
        
        // Step 2: Nhập password không khớp
        driver.findElement(By.name("username")).sendKeys("newuser123");
        driver.findElement(By.name("fullname")).sendKeys("Test User");
        driver.findElement(By.name("phone")).sendKeys("0901234567");
        driver.findElement(By.name("email")).sendKeys("newuser@test.com");
        driver.findElement(By.name("password")).sendKeys("123456");
        driver.findElement(By.name("confirmPassword")).sendKeys("654321"); // Khác
        
        // Step 3: Submit
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        // Step 4: Verify có lỗi validation
        WebElement errorMsg = driver.findElement(
                By.cssSelector(".error-message, .alert-danger, .invalid-feedback"));
        String errorText = errorMsg.getText();
        
        assertTrue(errorText.contains("không khớp") || errorText.contains("not match"),
                "Should show password mismatch error");
        
        System.out.println("❌ Lỗi: " + errorText);
        System.out.println("✅ Test password không khớp: PASSED");
    }
}
```

---

## 🚀 CÁCH CHẠY

```bash
mvn test -Dtest=LoginTest
mvn test -Dtest=SignupTest
```

---

## 📊 KẾT QUẢ MONG ĐỢI

```
✅ LoginTest
   ├─ TS_AUTH_001: Login admin ✓
   ├─ TS_AUTH_002: Login user ✓
   ├─ TS_AUTH_003: Sai password ✓
   ├─ TS_AUTH_004: Remember Me ✓
   ├─ TS_AUTH_005: Logout ✓
   └─ TS_AUTH_006: Chặn user vào admin ✓

✅ SignupTest
   ├─ TS_AUTH_007: Đăng ký thành công ✓
   ├─ TS_AUTH_008: Username trùng ✓
   └─ TS_AUTH_009: Password không khớp ✓

Total: 9 tests, 9 passed ✅
```

---

**Người tạo:** Khánh Ý  
**Ngày:** 2026-02-25
