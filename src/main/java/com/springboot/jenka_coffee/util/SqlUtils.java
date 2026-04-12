package com.springboot.jenka_coffee.util;

/**
 * SQL Utility class - Security helpers
 */
public class SqlUtils {

    /**
     * Escape SQL LIKE wildcards để chống SQL injection
     * Escape: % _ [ ] ^ -
     * 
     * SECURITY: Ngăn hacker inject wildcards để bypass filters hoặc DoS
     * Example: "test%" → "test\%"
     */
    public static String escapeLikeWildcards(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Escape backslash first (để không escape các escape chars sau)
        String escaped = input.replace("\\", "\\\\");
        
        // Escape SQL LIKE wildcards
        escaped = escaped.replace("%", "\\%");
        escaped = escaped.replace("_", "\\_");
        escaped = escaped.replace("[", "\\[");
        escaped = escaped.replace("]", "\\]");
        escaped = escaped.replace("^", "\\^");
        escaped = escaped.replace("-", "\\-");
        
        return escaped;
    }

    /**
     * Sanitize input cho search queries
     * - Trim whitespace
     * - Escape wildcards
     * - Giới hạn length
     */
    public static String sanitizeSearchInput(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        
        // Trim
        String sanitized = input.trim();
        
        // Giới hạn length
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        
        // Escape wildcards
        sanitized = escapeLikeWildcards(sanitized);
        
        return sanitized;
    }
}
