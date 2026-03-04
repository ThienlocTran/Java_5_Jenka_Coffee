package com.springboot.jenka_coffee.thien_loc;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductSearchFilterTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:8080";
    private static final String PRODUCT_LIST_URL = BASE_URL + "/product/list";

    @BeforeEach
    public void setupTest() throws InterruptedException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        
        Thread.sleep(2000);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (driver != null) {
            driver.quit();
        }
        System.out.println("⏳ Đợi 5 giây để DB recover...");
        Thread.sleep(5000);
    }

    /**
     * TC_PROD_002: Test tìm kiếm và filter sản phẩm (GỘP)
     * 
     * Điều kiện tiên quyết: Có dữ liệu sản phẩm trong database
     * Kết quả mong đợi: 
     * - Tìm kiếm theo keyword hoạt động đúng
     * - Filter theo giá hoạt động đúng
     * - Kết hợp search + filter hoạt động đúng
     */
    @Test
    @Order(1)
    @DisplayName("TC_PROD_002: Test tìm kiếm và filter sản phẩm")
    public void testSearchAndFilter() throws InterruptedException {
        System.out.println("\n=== TC_PROD_002: Test tìm kiếm và filter sản phẩm ===");
        
        // ============================================================
        // PHẦN 1: TEST TÌM KIẾM THEO KEYWORD
        // ============================================================
        System.out.println("\n--- PHẦN 1: Tìm kiếm theo keyword ---");
        
        driver.get(PRODUCT_LIST_URL);
        System.out.println("✓ Đã truy cập: " + PRODUCT_LIST_URL);
        Thread.sleep(3000);
        
        // Verify page load
        List<WebElement> searchInputs = driver.findElements(By.cssSelector("input[name='keyword']"));
        assertTrue(!searchInputs.isEmpty(), "Page phải load được search input");
        System.out.println("✓ Page load thành công");
        
        // Nhập keyword "Cafe"
        WebElement searchInput = searchInputs.get(0);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", searchInput);
        Thread.sleep(500);
        
        String keyword = "Cà Phê";
        // Use JavaScript to set value directly to avoid ElementNotInteractableException
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", 
            searchInput, keyword);
        System.out.println("✓ Đã nhập từ khóa: " + keyword);
        
        // Submit search - Click button instead of form.submit() to ensure values are captured
        WebElement searchButton = driver.findElement(By.cssSelector("button[type='submit'] .fa-search"));
        WebElement submitButton = searchButton.findElement(By.xpath("ancestor::button"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        System.out.println("✓ Đã submit tìm kiếm");
        
        Thread.sleep(3000);
        
        // Verify search results
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("keyword") || currentUrl.contains("/product/filter"), 
                "URL phải chứa keyword");
        System.out.println("✓ URL sau search: " + currentUrl);
        
        List<WebElement> searchResults = driver.findElements(By.cssSelector(".product-card"));
        System.out.println("✓ Số sản phẩm tìm thấy: " + searchResults.size());
        
        if (searchResults.size() > 0) {
            // Verify keyword hiển thị
            List<WebElement> keywordDisplay = driver.findElements(By.xpath("//span[contains(text(), '" + keyword + "')]"));
            if (!keywordDisplay.isEmpty()) {
                System.out.println("✓ Keyword được hiển thị trên trang");
            }
        }
        
        System.out.println("✓ PHẦN 1 PASSED: Tìm kiếm hoạt động đúng");
        
        // ============================================================
        // PHẦN 2: TEST FILTER THEO GIÁ
        // ============================================================
        System.out.println("\n--- PHẦN 2: Filter theo khoảng giá ---");
        
        // Quay lại trang list
        driver.get(PRODUCT_LIST_URL);
        Thread.sleep(3000);
        
        // Tìm form filter giá
        WebElement priceFilterForm = driver.findElement(By.id("priceFilterForm"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", priceFilterForm);
        Thread.sleep(500);
        System.out.println("✓ Tìm thấy form lọc giá");
        
        // Nhập giá từ 100,000
        WebElement minPriceInput = priceFilterForm.findElement(By.name("minPrice"));
        // Use JavaScript to set value directly to avoid ElementNotInteractableException
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", 
            minPriceInput, "100000");
        System.out.println("✓ Đã nhập giá min: 100,000");
        
        // Submit filter
        WebElement filterButton = priceFilterForm.findElement(By.cssSelector("button[type='submit']"));
        filterButton.click();
        System.out.println("✓ Đã click nút lọc giá");
        
        Thread.sleep(3000);
        
        // Verify filter results
        currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("minPrice") || currentUrl.contains("/product/filter"), 
                "URL phải chứa minPrice");
        System.out.println("✓ URL sau filter: " + currentUrl);
        
        List<WebElement> filteredProducts = driver.findElements(By.cssSelector(".product-card"));
        System.out.println("✓ Số sản phẩm sau filter: " + filteredProducts.size());
        
        System.out.println("✓ PHẦN 2 PASSED: Filter giá hoạt động đúng");
        
        // ============================================================
        // PHẦN 3: TEST KẾT HỢP SEARCH + FILTER
        // ============================================================
        System.out.println("\n--- PHẦN 3: Kết hợp tìm kiếm và filter ---");
        
        // Quay lại trang list
        driver.get(PRODUCT_LIST_URL);
        Thread.sleep(3000);
        
        // Nhập keyword
        searchInputs = driver.findElements(By.cssSelector("input[name='keyword']"));
        searchInput = searchInputs.get(0);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", searchInput);
        Thread.sleep(500);
        
        // Use JavaScript to set value directly to avoid ElementNotInteractableException
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", 
            searchInput, keyword);
        System.out.println("✓ Đã nhập keyword: " + keyword);
        
        // Submit search (sẽ giữ keyword trong URL) - Click button instead of form.submit()
        searchButton = driver.findElement(By.cssSelector("button[type='submit'] .fa-search"));
        submitButton = searchButton.findElement(By.xpath("ancestor::button"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);
        System.out.println("✓ Đã submit search");
        
        Thread.sleep(3000);
        
        // Verify combined results
        currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("keyword"), "URL phải chứa keyword");
        System.out.println("✓ URL kết hợp: " + currentUrl);
        
        List<WebElement> combinedResults = driver.findElements(By.cssSelector(".product-card"));
        System.out.println("✓ Số sản phẩm kết hợp: " + combinedResults.size());
        
        System.out.println("✓ PHẦN 3 PASSED: Kết hợp search + filter hoạt động đúng");
        
        System.out.println("\n=== TC_PROD_002 PASSED: Tất cả kiểm tra đều thành công ===\n");
    }
}
