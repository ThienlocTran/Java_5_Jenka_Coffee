package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.dto.request.BannerImageUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerSetService {
    List<BannerSet> findAll();
    BannerSet findById(Long id);
    /** Tạo bộ mới với danh sách ảnh upload */
    BannerSet create(String name, String effect, List<MultipartFile> files,
                     List<String> titles, List<String> subtitles,
                     List<String> objectPositions, List<Double> zooms);
    /** Cập nhật metadata bộ (tên, hiệu ứng) */
    BannerSet updateMeta(Long id, String name, String effect);
    /** Thêm ảnh vào bộ */
    BannerSet addImages(Long id, List<MultipartFile> files,
                        List<String> titles, List<String> subtitles,
                        List<String> objectPositions, List<Double> zooms);
    /** Cap nhat metadata/vung hien thi cho nhieu anh trong cung banner set */
    BannerSet updateImages(Long id, List<BannerImageUpdateRequest> images);
    /** Xóa 1 ảnh khỏi bộ */
    void removeImage(Long imageId);
    /** Xóa cả bộ */
    void delete(Long id);
    /** Kích hoạt bộ này (deactivate tất cả còn lại) */
    BannerSet activate(Long id);
    /** Trả về bộ đang active để hiển thị trang chủ */
    BannerSet getActive();
}
