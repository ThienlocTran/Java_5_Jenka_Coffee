package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Account;
import java.util.List;

public interface AccountService {
    Account findById(String username);

    List<Account> findAll();

    List<Account> getAdministrators();

    Account save(Account account);

    void delete(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Authentication
    Account authenticate(String username, String password);
}
