package com.springboot.jenka_coffee.service;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dqtrmwuxa");
        config.put("api_key", "287566178591623");
        config.put("api_secret", "4D8FARHcuZWH4kTPlu9styX92U4");
        return new Cloudinary(config);
    }
}
