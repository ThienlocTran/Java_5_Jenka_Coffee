# ImageService - Hướng dẫn sử dụng

## Tổng quan

ImageService là một service tiện ích để xử lý ảnh trong dự án Jenka Coffee, bao gồm:
- Resize ảnh theo tỷ lệ
- Nén ảnh để giảm dung lượng
- Lưu đè file gốc
- Tích hợp với Cloudinary upload

## Cấu trúc

### 1. ImageService Interface
```java
public interface ImageService {
    void processImage(File file, int targetWidth, float quality) throws IOException;
    void processImage(String filePath, int targetWidth, float quality) throws IOException;
    void processProductImage(File file) throws IOException;
    void processNewsImage(File file) throws IOException;
}
```

### 2. ImageServiceImpl
- Sử dụng Java AWT để resize ảnh với chất lượng cao
- Hỗ trợ nén JPEG với quality từ 0.1 đến 1.0
- Tự động maintain aspect ratio
- Xử lý an toàn với temporary files

### 3. UploadService Integration
- Tự động nén ảnh trước khi upload lên Cloudinary
- Validation ảnh với ImageUtils
- Hỗ trợ các preset cho từng loại ảnh

## Cách sử dụng

### 1. Xử lý ảnh sản phẩm
```java
@Autowired
private UploadService uploadService;

// Upload với nén tự động (800px, quality 0.85)
String imageUrl = uploadService.saveProductImage(multipartFile);
```

### 2. Xử lý ảnh avatar
```java
// Upload avatar với preset (400px, quality 0.8)
String avatarUrl = uploadService.saveImageWithCompression(
    photoFile, 
    ImageUtils.ImagePresets.AVATAR_WIDTH, 
    ImageUtils.ImagePresets.AVATAR_QUALITY
);
```

### 3. Xử lý ảnh tin tức
```java
// Upload ảnh tin tức (1200px, quality 0.85)
String newsImageUrl = uploadService.saveNewsImage(multipartFile);
```

### 4. Xử lý ảnh với cài đặt tùy chỉnh
```java
// Custom settings
String imageUrl = uploadService.saveImageWithCompression(file, 600, 0.9f);
```

## Image Presets

Được định nghĩa trong `ImageUtils.ImagePresets`:

| Loại ảnh | Chiều rộng | Chất lượng | Mục đích |
|----------|------------|------------|----------|
| Product  | 800px      | 0.85       | Ảnh sản phẩm |
| Avatar   | 400px      | 0.8        | Ảnh đại diện |
| News     | 1200px     | 0.85       | Ảnh tin tức |
| Thumbnail| 300px      | 0.75       | Ảnh thumbnail |

## Validation

ImageUtils cung cấp validation cho:
- Định dạng file: JPG, PNG, GIF, WebP
- Kích thước tối đa: 5MB
- Content-type validation
- File extension validation

## Lưu ý kỹ thuật

### 1. Xử lý ảnh nhỏ hơn target
- Nếu ảnh gốc nhỏ hơn target width, không resize
- Vẫn áp dụng compression nếu quality < 1.0

### 2. Chất lượng nén
- 0.1 = chất lượng thấp nhất, file nhỏ nhất
- 1.0 = chất lượng cao nhất, file lớn nhất
- Khuyến nghị: 0.8-0.9 cho ảnh quan trọng, 0.7-0.8 cho ảnh thường

### 3. Memory Management
- Sử dụng temporary files để tránh memory leak
- Tự động cleanup sau khi xử lý
- Graphics2D dispose đúng cách

### 4. Error Handling
- IOException cho lỗi file I/O
- InvalidFileException cho validation errors
- Logging chi tiết cho debugging

## Tích hợp với Controllers

### Product Controller
```java
@PostMapping("/admin/products/save")
public String saveProduct(@ModelAttribute Product product, 
                         @RequestParam("imageFile") MultipartFile file) {
    // ImageService tự động được gọi trong productService.saveProduct()
    Product savedProduct = productService.saveProduct(product, file);
    return "redirect:/admin/products";
}
```

### Account Controller
```java
@PostMapping("/register")
public String register(@ModelAttribute Account account,
                      @RequestParam("photoFile") MultipartFile photoFile) {
    // ImageService tự động được gọi trong accountService.createAccount()
    Account newAccount = accountService.createAccount(account, photoFile);
    return "redirect:/login";
}
```

## Performance Tips

1. **Batch Processing**: Xử lý nhiều ảnh cùng lúc nếu có thể
2. **Async Processing**: Sử dụng @Async cho upload không đồng bộ
3. **Caching**: Cache processed images nếu cần
4. **CDN**: Cloudinary tự động optimize và cache

## Troubleshooting

### Lỗi thường gặp:
1. **OutOfMemoryError**: Ảnh quá lớn, cần giảm kích thước trước khi xử lý
2. **IOException**: Kiểm tra quyền ghi file và disk space
3. **InvalidFileException**: File không phải ảnh hoặc bị corrupt
4. **Cloudinary Upload Failed**: Kiểm tra API key và network connection

### Debug:
- Enable logging level DEBUG cho package `com.springboot.jenka_coffee.service`
- Kiểm tra temp directory permissions
- Monitor memory usage khi xử lý ảnh lớn