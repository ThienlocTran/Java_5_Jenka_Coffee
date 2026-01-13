package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDAO extends JpaRepository<Account, String> {

}