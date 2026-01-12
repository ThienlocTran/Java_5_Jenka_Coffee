package com.springboot.jenka_coffee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        try {
            // upload(file.getBytes(), Map cấu hình tùy chọn)
            // ObjectUtils.emptyMap() nghĩa là upload mặc định, không chỉnh sửa gì thêm
            Map data = this.cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

            // Lấy cái url ảnh trả về
            return (String) data.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
        }
    }
}