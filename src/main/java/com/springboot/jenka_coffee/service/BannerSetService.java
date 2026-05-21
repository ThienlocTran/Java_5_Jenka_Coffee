package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.BannerImageUpdateRequest;
import com.springboot.jenka_coffee.entity.BannerSet;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface BannerSetService {
    List<BannerSet> findAll();
    BannerSet findById(Long id);
    /** Tạo bộ mới với danh sách ảnh upload */
    BannerSet create(String name, String effect, List<MultipartFile> files,
                     List<String> titles, List<String> subtitles,
                     List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                     List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                     List<BigDecimal> imageZooms);
    /** Cập nhật metadata bộ (tên, hiệu ứng) */
    BannerSet updateMeta(Long id, String name, String effect);
    /** Thêm ảnh vào bộ */
    BannerSet addImages(Long id, List<MultipartFile> files,
                        List<String> titles, List<String> subtitles,
                        List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                        List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                        List<BigDecimal> imageZooms);
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
