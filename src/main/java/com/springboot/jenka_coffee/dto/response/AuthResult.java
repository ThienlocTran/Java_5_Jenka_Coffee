package com.springboot.jenka_coffee.dto.response;

import com.springboot.jenka_coffee.entity.Account;

/**
 * Wraps authentication outcome to avoid a second DB query in the controller.
 * Replaces the old pattern of returning null for both "wrong password" and "not activated".
 */
public record AuthResult(AuthStatus status, Account account) {

    public static AuthResult success(Account account) {
        return new AuthResult(AuthStatus.SUCCESS, account);
    }

    public static AuthResult invalidCredentials() {
        return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null);
    }

    public static AuthResult notActivated(Account account) {
        return new AuthResult(AuthStatus.NOT_ACTIVATED, account);
    }

    public boolean isSuccess() { return status == AuthStatus.SUCCESS; }
}
