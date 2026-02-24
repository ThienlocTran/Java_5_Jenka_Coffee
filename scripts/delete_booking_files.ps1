# ============================================
# SCRIPT TỰ ĐỘNG XÓA CÁC FILE BOOKING
# Jenka Coffee Project
# ============================================

Write-Host "=== BẮT ĐẦU XÓA CÁC FILE BOOKING ===" -ForegroundColor Cyan
Write-Host ""

# Danh sách các file cần xóa
$filesToDelete = @(
    "src/main/java/com/springboot/jenka_coffee/entity/Booking.java",
    "src/main/java/com/springboot/jenka_coffee/entity/ServiceBooking.java",
    "src/main/java/com/springboot/jenka_coffee/repository/BookingRepository.java",
    "src/main/java/com/springboot/jenka_coffee/repository/ServiceBookingRepository.java",
    "src/main/java/com/springboot/jenka_coffee/service/BookingService.java",
    "src/main/java/com/springboot/jenka_coffee/service/impl/BookingServiceImpl.java",
    "src/main/java/com/springboot/jenka_coffee/controller/site/BookingController.java",
    "src/main/java/com/springboot/jenka_coffee/controller/admin/AdminBookingController.java"
)

$deletedCount = 0
$notFoundCount = 0

foreach ($file in $filesToDelete) {
    if (Test-Path $file) {
        Write-Host "Đang xóa: $file" -ForegroundColor Yellow
        Remove-Item $file -Force
        Write-Host "  ✓ Đã xóa thành công!" -ForegroundColor Green
        $deletedCount++
    } else {
        Write-Host "  ⚠ File không tồn tại: $file" -ForegroundColor Gray
        $notFoundCount++
    }
    Write-Host ""
}

Write-Host "=== KẾT QUẢ ===" -ForegroundColor Cyan
Write-Host "Đã xóa: $deletedCount file(s)" -ForegroundColor Green
Write-Host "Không tìm thấy: $notFoundCount file(s)" -ForegroundColor Gray
Write-Host ""

Write-Host "=== BƯỚC TIẾP THEO ===" -ForegroundColor Cyan
Write-Host "1. Cập nhật Account.java (xóa serviceBookings)" -ForegroundColor Yellow
Write-Host "2. Chạy: mvn clean install" -ForegroundColor Yellow
Write-Host "3. Restart ứng dụng" -ForegroundColor Yellow
Write-Host ""

Write-Host "=== HOÀN THÀNH ===" -ForegroundColor Green
