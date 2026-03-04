package com.springboot.jenka_coffee.tuan_khoa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

public class CartAutomationTest {

    WebDriver driver;

    @BeforeClass
    public void setUp() {
        // 1. Khởi tạo trình duyệt
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();

        // 2. Đăng nhập hệ thống (Điều kiện tiên quyết cho TC_CART_002)
        driver.get("http://localhost:8080/auth/login");
        driver.findElement(By.name("username")).sendKeys("admin"); // Tài khoản
        driver.findElement(By.name("password")).sendKeys("123");   // Mật khẩu
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    @Test(priority = 1, description = "TC_CART_001: Test thêm SP vào giỏ, cập nhật số lượng")
    public void testTC_CART_001_AddToCartAndUpdate() throws InterruptedException {
        // Bước 1: Thêm sản phẩm ID = 1 vào giỏ hàng
        driver.get("http://localhost:8080/cart/add/1");
        Thread.sleep(1000); // Dừng 1s để quan sát

        // Bước 2: Cập nhật số lượng thành 2 (Sử dụng URL update của CartController)
        driver.get("http://localhost:8080/cart/update/1/2");
        Thread.sleep(1000);

        // Kiểm tra kết quả mong đợi
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("/cart/view"), "Phải chuyển về trang xem giỏ hàng!");

        // Kiểm tra xem trong giỏ hàng có hiển thị đúng số lượng là 2 không (Nếu HTML có ô input qty)
        WebElement qtyInput = driver.findElement(By.name("qty"));
        String actualQty = qtyInput.getAttribute("value");
        Assert.assertEquals(actualQty, "2", "Số lượng trong giỏ hàng phải là 2!");
    }

    @Test(priority = 2, description = "TC_CART_002: Test toàn bộ luồng checkout")
    public void testTC_CART_002_CheckoutFlow() throws InterruptedException {
        // Bước 1: Vào trang checkout (Điều kiện: Đã có hàng từ TC_CART_001)
        driver.get("http://localhost:8080/checkout");
        Thread.sleep(1000);

        // Bước 2: Nhập thông tin giao hàng (Dữ liệu đầu vào)
        driver.findElement(By.name("fullname")).clear();
        driver.findElement(By.name("fullname")).sendKeys("Nguyễn Văn A");

        driver.findElement(By.name("phone")).clear();
        driver.findElement(By.name("phone")).sendKeys("0901234567");

        driver.findElement(By.name("email")).clear();
        driver.findElement(By.name("email")).sendKeys("test@gmail.com");

        driver.findElement(By.name("address")).clear();
        driver.findElement(By.name("address")).sendKeys("123 ABC Street");

        // Bước 3: Xác nhận thanh toán (Click nút Đặt hàng)
        // Lưu ý: Đảm bảo ô checkbox agreeTerms được tick nếu Controller yêu cầu @Valid
        WebElement agreeCheck = driver.findElement(By.name("agreeTerms"));
        if (!agreeCheck.isSelected()) agreeCheck.click();

        driver.findElement(By.cssSelector("button[type='submit']")).click();
        Thread.sleep(2000); // Chờ server xử lý tạo Order

        // Kết quả mong đợi
        String finalUrl = driver.getCurrentUrl();
        Assert.assertTrue(finalUrl.contains("/checkout/success"), "Phải nhảy sang trang báo thành công!");

        // Kiểm tra xem giỏ hàng đã trống chưa (Quay lại cart xem có hàng không)
        driver.get("http://localhost:8080/cart/view");
        // Nếu giỏ hàng trống, thường sẽ hiển thị dòng chữ "Giỏ hàng trống" hoặc không có item nào
        boolean isCartEmpty = driver.findElements(By.className("cart-item")).isEmpty();
        Assert.assertTrue(isCartEmpty, "Giỏ hàng phải được xóa sạch sau khi hoàn tất checkout!");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}