package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

        // ===== DERIVED QUERY METHODS =====
        // Spring Data JPA auto-generates queries from method names

        /**
         * Check if email exists
         * Generated query: SELECT COUNT(*) FROM accounts WHERE email = ?
         */
        boolean existsByEmail(String email);

        /**
         * Find account by email
         */
        Optional<Account> findByEmail(String email);

        /**
         * Find account by phone number
         */
        Optional<Account> findByPhone(String phone);

        /**
         * Find all admin accounts (admin = true)
         * Generated query: SELECT * FROM accounts WHERE admin = true
         */
        List<Account> findByAdminTrue();

        /**
         * Count admin accounts
         * Generated query: SELECT COUNT(*) FROM accounts WHERE admin = true
         */
        long countByAdminTrue();

        // ===== CUSTOM @Query METHODS =====

        /**
         * Flexible login: Find account by username OR email OR phone
         * Enables login with any of the three identifiers
         */
        @Query("SELECT a FROM Account a WHERE a.username = :identifier " +
                        "OR a.email = :identifier OR a.phone = :identifier")
        Optional<Account> findByUsernameOrEmailOrPhone(@Param("identifier") String identifier);

        // ===== ACTIVATION & PASSWORD RESET =====

        /**
         * Find account by activation token
         */
        Optional<Account> findByActivationToken(String token);

        /**
         * Find account by password reset token
         */
        Optional<Account> findByResetToken(String token);

        /**
         * Find all activated accounts only
         * DSL method - Spring generates: SELECT * FROM accounts WHERE activated = true
         */
        List<Account> findByActivatedTrue();

        /**
         * Search accounts by keyword (searches in username, fullname, email)
         * JPQL required: multiple fields with LIKE + case-insensitive
         */
        @Query("SELECT a FROM Account a WHERE " +
                        "LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Account> searchAccounts(@Param("keyword") String keyword);
}