package com.springboot.jenka_coffee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.springboot.jenka_coffee.service.UploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UploadServiceImpl implements UploadService {


     final Cloudinary cloudinary;

     public UploadServiceImpl(Cloudinary cloudinary) {
         this.cloudinary = cloudinary;
     }

    @Override
    public String saveImage(MultipartFile file) {
        try {
            // Upload file lên Cloudinary
            Map r = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto"
            ));
            // Trả về đường dẫn ảnh (URL)
            return (String) r.get("secure_url");
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Trả về null nếu lỗi
        }
    }
}