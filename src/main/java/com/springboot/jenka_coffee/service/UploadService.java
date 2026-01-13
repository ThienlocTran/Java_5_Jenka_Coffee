package com.springboot.jenka_coffee.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    String saveImage(MultipartFile file);
}