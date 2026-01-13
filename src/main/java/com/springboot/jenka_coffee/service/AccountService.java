package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Account;
import java.util.List;

public interface AccountService {
    Account findById(String username);
    List<Account> findAll();
    List<Account> getAdministrators();
}