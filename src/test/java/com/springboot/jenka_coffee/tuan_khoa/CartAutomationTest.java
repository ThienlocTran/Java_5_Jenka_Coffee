package com.springboot.jenka_coffee.tuan_khoa;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.time.Duration;

public class CartAutomationTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "http://localhost:8080";

    @BeforeClass
    public void setUp() {
        System.out.println("🚀 Đang khởi tạo trình duyệt...");
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-notifications");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        login();
    }

    private void login() {
        try {
            driver.get(BASE_URL + "/auth/login");
            WebElement userInp = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
            userInp.sendKeys("admin");
            driver.findElement(By.name("password")).sendKeys("123");
            driver.findElement(By.cssSelector("button[type='submit']")).click();
            wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
            System.out.println("✅ Đăng nhập thành công!");
        } catch (Exception e) {
            System.out.println("❌ Lỗi đăng nhập: " + e.getMessage());
        }
    }

    // Hàm bổ trợ: Xóa sạch nội dung bằng phím tắt trước khi nhập (Trị dứt điểm Auto-fill)
    private void clearAndType(WebElement element, String text) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        element.sendKeys(text);
    }

    @Test(priority = 1)
    public void TC_CART_001_Add_Update() {
        System.out.println("🛒 Đang chạy TC_CART_001...");
        driver.get(BASE_URL + "/cart/add/1");
        wait.until(ExpectedConditions.urlContains("/cart/view"));
        driver.get(BASE_URL + "/cart/update/1/5");

        WebElement qtyInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='number'], .qty-input")
        ));
        Assert.assertEquals(qtyInput.getAttribute("value"), "5", "Lỗi: Số lượng chưa khớp!");
        System.out.println("✅ TC_001 PASSED!");
    }

    @Test(priority = 2, dependsOnMethods = "TC_CART_001_Add_Update")
    public void TC_CART_002_Checkout_Success() {
        System.out.println("💳 Đang chạy TC_CART_002: Thanh toán...");
        driver.get(BASE_URL + "/checkout");

        try {
            // 1. Nhập Họ tên
            WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fullname")));
            clearAndType(name, "Nguyen Van Test");

            // 2. Nhập Email (Tránh dính @fpt)
            WebElement email = driver.findElement(By.id("email"));
            clearAndType(email, "automation@gmail.com");

            // 3. Nhập Số điện thoại (QUAN TRỌNG: Triệt tiêu số cũ có sẵn)
            WebElement phone = driver.findElement(By.id("phone"));
            clearAndType(phone, "0901234567");
            System.out.println("📱 Đã ép buộc nhập số điện thoại mới: 0901234567");

            // 4. Xử lý Dropdown Địa chỉ
            new Select(driver.findElement(By.id("province"))).selectByVisibleText("Hồ Chí Minh");
            Thread.sleep(1000); // Đợi JS load Quận
            new Select(driver.findElement(By.id("district"))).selectByVisibleText("Quận 1");
            Thread.sleep(1000); // Đợi JS load Phường
            new Select(driver.findElement(By.id("ward"))).selectByVisibleText("Bến Nghé");

            WebElement address = driver.findElement(By.id("address"));
            clearAndType(address, "70 To Hien Thanh");

            // 5. Radio & Checkbox
            jsClick(driver.findElement(By.id("paymentCOD")));
            jsClick(driver.findElement(By.id("agreeTerms")));

            // 6. Submit
            WebElement btnSubmit = driver.findElement(By.cssSelector("button[type='submit']"));
            jsClick(btnSubmit);

            System.out.println("🚀 Đã gửi form thanh toán...");

        } catch (Exception e) {
            Assert.fail("❌ Lỗi điền form: " + e.getMessage());
        }

        // 7. Verify trang thành công
        wait.until(ExpectedConditions.urlContains("/success"));
        Assert.assertTrue(driver.getCurrentUrl().contains("success"), "Lỗi: Không chuyển đến trang Success!");
        System.out.println("🎉 TC_CART_002 PASSED - Thanh toán rực rỡ!");
    }

    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}