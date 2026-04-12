package com.springboot.jenka_coffee.service;

/**
 * JWT Blacklist Service - Quản lý danh sách token bị vô hiệu hóa
 * VULN-TOKEN-001 FIX: Khi user logout, token phải được blacklist
 */
public interface JwtBlacklistService {
    
    /**
     * Thêm token vào blacklist
     * @param token JWT token cần blacklist
     * @param expirationTimeMs Thời gian hết hạn của token (milliseconds)
     */
    void blacklistToken(String token, long expirationTimeMs);
    
    /**
     * Kiểm tra token có bị blacklist không
     * @param token JWT token cần kiểm tra
     * @return true nếu token bị blacklist
     */
    boolean isBlacklisted(String token);
}
