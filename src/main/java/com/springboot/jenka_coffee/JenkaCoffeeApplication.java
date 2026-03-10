package com.springboot.jenka_coffee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class JenkaCoffeeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JenkaCoffeeApplication.class, args);
    }

}
