package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.repository.AccountDAO;
import com.springboot.jenka_coffee.service.AccountService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {
    final AccountDAO dao;


    public AccountServiceImpl(AccountDAO dao) {
        this.dao = dao;
    }

    @Override
    public Account findById(String username) {
        return dao.findById(username).orElse(null);
    }

    @Override
    public List<Account> findAll() {
        return dao.findAll();
    }

    @Override
    public List<Account> getAdministrators() {
        return dao.findAll().stream()
                .filter(acc -> acc.getAdmin() != null && acc.getAdmin())
                .toList();
    }

    @Override
    public Account save(Account account) {
        return dao.save(account);
    }

    @Override
    public void delete(String username) {
        dao.deleteById(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return dao.existsById(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return dao.findAll().stream()
                .anyMatch(acc -> acc.getEmail().equalsIgnoreCase(email));
    }
}