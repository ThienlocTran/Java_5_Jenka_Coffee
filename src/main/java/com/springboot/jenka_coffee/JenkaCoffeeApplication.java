package com.springboot.jenka_coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableCaching
public class JenkaCoffeeApplication {

    private static final Logger log = LoggerFactory.getLogger(JenkaCoffeeApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JenkaCoffeeApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminAccount(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (accountRepository.findByUsernameOrEmailOrPhone("admin").isEmpty()) {
                Account admin = new Account();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("123456"));
                admin.setFullname("Administrator");
                admin.setEmail("admin@jenkacoffee.com");
                admin.setPhone("0999999999");
                admin.setActivated(true);
                admin.setAdmin(true);
                admin.setCustomerRank("DIAMOND");
                accountRepository.save(admin);
                log.info("Default admin account created: username=admin");
            }
        };
    }
}
