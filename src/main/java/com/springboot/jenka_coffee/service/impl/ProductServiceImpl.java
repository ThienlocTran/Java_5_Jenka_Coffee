package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.ProductDAO;
import com.springboot.jenka_coffee.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    final ProductDAO pdao;
    final com.springboot.jenka_coffee.service.UploadService uploadService;

    public ProductServiceImpl(ProductDAO pdao, com.springboot.jenka_coffee.service.UploadService uploadService) {
        this.pdao = pdao;
        this.uploadService = uploadService;
    }

    @Override
    public List<Product> findAll() {
        return pdao.findAll();
    }

    @Override
    public Product saveProduct(Product product, org.springframework.web.multipart.MultipartFile file) {
        // --- XỬ LÝ ẢNH ---
        // Nếu người dùng có chọn ảnh mới -> Upload lên Cloudinary
        if (file != null && !file.isEmpty()) {
            String url = uploadService.saveImage(file);
            if (url != null) {
                product.setImage(url);
            }
        }
        // Nếu không chọn ảnh mới -> Giữ nguyên ảnh cũ (Do input hidden trong form lo)

        // --- LƯU VÀO DB ---
        return pdao.save(product);
    }

    @Override
    public Product findById(Integer id) {
        // .orElse(null) giúp tránh lỗi nếu ID không tồn tại
        return pdao.findById(id).orElse(null);
    }

    @Override
    public List<Product> findByCategoryId(String cid) {
        // Cách 1: Viết method trong DAO (Khuyên dùng)
        return pdao.findByCategoryId(cid);
    }

    @Override
    public Product create(Product product) {
        return pdao.save(product);
    }

    @Override
    public Product update(Product product) {
        return pdao.save(product);
    }

    @Override
    public void delete(Integer id) {
        pdao.deleteById(id);
    }
}