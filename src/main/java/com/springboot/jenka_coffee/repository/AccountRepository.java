package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        boolean existsByEmailIgnoreCase(String email);

        /**
         * Check if phone exists
         * Generated query: SELECT COUNT(*) FROM accounts WHERE phone = ?
         */
        boolean existsByPhone(String phone);

        /**
         * Find account by phone number
         */
        Optional<Account> findByPhone(String phone);

        /**
         * Find account by email
         */
        Optional<Account> findByEmailIgnoreCase(String email);

        /**
         * Find account by username
         */
        Optional<Account> findByUsername(String username);

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

        long countByCreateDateGreaterThanEqualAndCreateDateLessThan(LocalDateTime from, LocalDateTime to);

        // ===== CUSTOM @Query METHODS =====

        /**
         * Flexible login: Find account by username OR email OR phone
         * Enables login with any of the three identifiers
         */
        @Query("SELECT a FROM Account a WHERE a.username = :identifier " +
                        "OR LOWER(a.email) = LOWER(:identifier) OR a.phone = :identifier")
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
         * VULN-M01 FIX: Check account exists AND is active — dùng trong JwtAuthFilter
         * Lightweight query, không load toàn bộ entity
         */

}
