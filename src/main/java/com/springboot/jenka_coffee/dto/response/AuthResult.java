package com.springboot.jenka_coffee.dto.response;

import com.springboot.jenka_coffee.entity.Account;

/**
 * Wraps authentication outcome to avoid a second DB query in the controller.
 * Replaces the old pattern of returning null for both "wrong password" and "not activated".
 */
public record AuthResult(Status status, Account account) {

    public enum Status { SUCCESS, INVALID_CREDENTIALS, NOT_ACTIVATED }

    public static AuthResult success(Account account) {
        return new AuthResult(Status.SUCCESS, account);
    }

    public static AuthResult invalidCredentials() {
        return new AuthResult(Status.INVALID_CREDENTIALS, null);
    }

    public static AuthResult notActivated(Account account) {
        return new AuthResult(Status.NOT_ACTIVATED, account);
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
}
