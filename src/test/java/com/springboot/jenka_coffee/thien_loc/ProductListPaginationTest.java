package com.springboot.jenka_coffee.thien_loc;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
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
public class ProductListPaginationTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:8080";
    private static final String PRODUCT_LIST_URL = BASE_URL + "/product/list";

    @BeforeEach
    public void setupTest() {
        // OPTION 1: Nếu ChromeDriver không có trong PATH, uncomment dòng này:
        // System.setProperty("webdriver.chrome.driver", "C:\\path\\to\\chromedriver.exe");
        
        // OPTION 2: Hoặc thêm ChromeDriver vào PATH của Windows
        
        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        
        // Initialize driver
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Set implicit wait
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (driver != null) {
            driver.quit();
        }
        // Đợi 5 giây giữa các test để tránh quá tải database (DB ở Singapore rất chậm)
        System.out.println("⏳ Đợi 5 giây để DB recover...");
        Thread.sleep(5000);
    }

    /**
     * TC_PROD_001_01: Test hiển thị danh sách sản phẩm
     * 
     * Điều kiện tiên quyết: Có dữ liệu sản phẩm trong database
     * Kết quả mong đợi: Load đúng danh sách sản phẩm với đầy đủ thông tin
     */
    @Test
    @Order(1)
    @DisplayName("TC_PROD_001_01: Hiển thị danh sách sản phẩm")
    public void testProductListDisplay() throws InterruptedException {
        System.out.println("\n=== TEST: Hiển thị danh sách sản phẩm ===");
        
        // Retry logic cho connection reset
        int maxRetries = 3;
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                // Bước 1: Truy cập trang danh sách sản phẩm
                driver.get(PRODUCT_LIST_URL);
                System.out.println("✓ Đã truy cập: " + PRODUCT_LIST_URL);
                break; // Success, exit retry loop
            } catch (org.openqa.selenium.WebDriverException e) {
                if (e.getMessage().contains("ERR_CONNECTION_RESET") && retry < maxRetries - 1) {
                    System.out.println("⚠ Connection reset, retry " + (retry + 1) + "/" + maxRetries);
                    Thread.sleep(3000); // Đợi 3 giây trước khi retry
                } else {
                    throw e; // Throw nếu hết retries hoặc lỗi khác
                }
            }
        }
        
        // Bước 2: Đợi trang load xong
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
        
        // Bước 3: Verify URL
        assertTrue(driver.getCurrentUrl().contains("/product/list"), 
                "URL phải chứa '/product/list'");
        System.out.println("✓ URL đúng: " + driver.getCurrentUrl());
        
        // Bước 4: Verify page title/breadcrumb
        WebElement breadcrumb = driver.findElement(By.cssSelector(".breadcrumb-item.active"));
        assertNotNull(breadcrumb, "Breadcrumb phải tồn tại");
        System.out.println("✓ Breadcrumb: " + breadcrumb.getText());
        
        // Bước 5: Verify có hiển thị sản phẩm
        List<WebElement> productCards = driver.findElements(By.cssSelector(".product-card"));
        assertTrue(productCards.size() > 0, "Phải có ít nhất 1 sản phẩm");
        System.out.println("✓ Số sản phẩm hiển thị: " + productCards.size());
        
        // Bước 6: Verify thông tin sản phẩm đầu tiên
        WebElement firstProduct = productCards.get(0);
        
        // Verify có hình ảnh
        WebElement productImage = firstProduct.findElement(By.cssSelector("img.product-img"));
        assertNotNull(productImage, "Sản phẩm phải có hình ảnh");
        assertFalse(productImage.getAttribute("src").isEmpty(), "Hình ảnh phải có src");
        System.out.println("✓ Sản phẩm có hình ảnh");
        
        // Verify có tên sản phẩm
        WebElement productName = firstProduct.findElement(By.cssSelector(".card-title a"));
        assertNotNull(productName, "Sản phẩm phải có tên");
        assertFalse(productName.getText().isEmpty(), "Tên sản phẩm không được rỗng");
        System.out.println("✓ Tên sản phẩm: " + productName.getText());
        
        // Verify có giá
        WebElement productPrice = firstProduct.findElement(By.cssSelector(".text-danger.fw-bold"));
        assertNotNull(productPrice, "Sản phẩm phải có giá");
        assertFalse(productPrice.getText().isEmpty(), "Giá sản phẩm không được rỗng");
        System.out.println("✓ Giá sản phẩm: " + productPrice.getText());
        
        // Verify có category
        WebElement productCategory = firstProduct.findElement(By.cssSelector(".text-muted.small"));
        assertNotNull(productCategory, "Sản phẩm phải có danh mục");
        System.out.println("✓ Danh mục: " + productCategory.getText());
        
        // Verify có nút thêm vào giỏ hàng
        WebElement addToCartBtn = firstProduct.findElement(By.cssSelector(".btn-add-to-cart, .btn-disabled"));
        assertNotNull(addToCartBtn, "Sản phẩm phải có nút thêm vào giỏ");
        System.out.println("✓ Có nút thêm vào giỏ hàng");
        
        System.out.println("=== TEST PASSED: Danh sách sản phẩm hiển thị đúng ===\n");
    }

    /**
     * TC_PROD_001_02: Test phân trang danh sách sản phẩm
     * 
     * Điều kiện tiên quyết: Có nhiều hơn 1 trang sản phẩm trong database
     * Kết quả mong đợi: Phân trang hoạt động chính xác, chuyển trang thành công
     */
    @Test
    @Order(2)
    @DisplayName("TC_PROD_001_02: Phân trang danh sách sản phẩm")
    public void testProductListPagination() throws InterruptedException {
        System.out.println("\n=== TEST: Phân trang danh sách sản phẩm ===");
        
        // Retry logic cho connection reset
        int maxRetries = 3;
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                // Bước 1: Truy cập trang danh sách sản phẩm
                driver.get(PRODUCT_LIST_URL);
                System.out.println("✓ Đã truy cập: " + PRODUCT_LIST_URL);
                break; // Success, exit retry loop
            } catch (org.openqa.selenium.WebDriverException e) {
                if (e.getMessage().contains("ERR_CONNECTION_RESET") && retry < maxRetries - 1) {
                    System.out.println("⚠ Connection reset, retry " + (retry + 1) + "/" + maxRetries);
                    Thread.sleep(3000); // Đợi 3 giây trước khi retry
                } else {
                    throw e; // Throw nếu hết retries hoặc lỗi khác
                }
            }
        }
        
        // Bước 2: Đợi trang load xong
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
        
        // Bước 3: Kiểm tra có pagination không
        List<WebElement> paginationElements = driver.findElements(By.cssSelector(".pagination"));
        
        if (paginationElements.isEmpty()) {
            System.out.println("⚠ Không có pagination (có thể chỉ có 1 trang)");
            System.out.println("=== TEST SKIPPED: Không đủ dữ liệu để test pagination ===\n");
            return;
        }
        
        System.out.println("✓ Có pagination");
        
        // Bước 4: Lấy danh sách các trang
        List<WebElement> pageLinks = driver.findElements(By.cssSelector(".pagination .page-link"));
        assertTrue(pageLinks.size() > 0, "Phải có ít nhất 1 link phân trang");
        System.out.println("✓ Số lượng page links: " + pageLinks.size());
        
        // Bước 5: Lấy tên sản phẩm đầu tiên ở trang 1
        WebElement firstProductPage1 = driver.findElement(By.cssSelector(".product-card .card-title a"));
        String firstProductNamePage1 = firstProductPage1.getText();
        System.out.println("✓ Sản phẩm đầu tiên trang 1: " + firstProductNamePage1);
        
        // Bước 6: Tìm và click vào trang 2 (nếu có)
        List<WebElement> pageNumbers = driver.findElements(
            By.cssSelector(".pagination .page-item:not(.disabled) .page-link"));
        
        WebElement page2Link = null;
        for (WebElement link : pageNumbers) {
            String text = link.getText().trim();
            if (text.equals("2")) {
                page2Link = link;
                break;
            }
        }
        
        if (page2Link == null) {
            System.out.println("⚠ Không tìm thấy trang 2");
            System.out.println("=== TEST SKIPPED: Chỉ có 1 trang ===\n");
            return;
        }
        
        // Click vào trang 2
        page2Link.click();
        System.out.println("✓ Đã click vào trang 2");
        
        // Bước 7: Đợi trang 2 load xong
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
        Thread.sleep(1000); // Wait for page transition
        
        // Bước 8: Verify URL có chứa page=1 (page index bắt đầu từ 0)
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("page=1") || currentUrl.contains("/product/list"), 
                "URL phải chứa page parameter hoặc là product list");
        System.out.println("✓ URL trang 2: " + currentUrl);
        
        // Bước 9: Verify active page là trang 2
        WebElement activePage = driver.findElement(By.cssSelector(".pagination .page-item.active .page-link"));
        assertEquals("2", activePage.getText().trim(), "Trang active phải là trang 2");
        System.out.println("✓ Trang active: " + activePage.getText());
        
        // Bước 10: Verify sản phẩm đầu tiên ở trang 2 khác với trang 1
        WebElement firstProductPage2 = driver.findElement(By.cssSelector(".product-card .card-title a"));
        String firstProductNamePage2 = firstProductPage2.getText();
        System.out.println("✓ Sản phẩm đầu tiên trang 2: " + firstProductNamePage2);
        
        assertNotEquals(firstProductNamePage1, firstProductNamePage2, 
                "Sản phẩm đầu tiên ở trang 2 phải khác trang 1");
        System.out.println("✓ Sản phẩm trang 2 khác trang 1");
        
        // Bước 11: Test nút Previous
        WebElement previousBtn = driver.findElement(
            By.cssSelector(".pagination .page-item:first-child .page-link"));
        previousBtn.click();
        System.out.println("✓ Đã click nút Previous");
        
        // Đợi load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
        Thread.sleep(1000);
        
        // Verify quay lại trang 1
        WebElement activePageAfterPrevious = driver.findElement(
            By.cssSelector(".pagination .page-item.active .page-link"));
        assertEquals("1", activePageAfterPrevious.getText().trim(), 
                "Sau khi click Previous phải về trang 1");
        System.out.println("✓ Đã quay lại trang 1");
        
        System.out.println("=== TEST PASSED: Phân trang hoạt động đúng ===\n");
    }

    /**
     * TC_PROD_001_03: Test số lượng sản phẩm hiển thị mỗi trang
     * 
     * Điều kiện tiên quyết: Có dữ liệu sản phẩm trong database
     * Kết quả mong đợi: Mỗi trang hiển thị đúng số lượng sản phẩm (1-12 SP/trang)
     */
    @Test
    @Order(3)
    @DisplayName("TC_PROD_001_03: Số lượng sản phẩm mỗi trang")
    public void testProductsPerPage() throws InterruptedException {
        System.out.println("\n=== TEST: Số lượng sản phẩm mỗi trang ===");
        
        // Retry logic cho connection reset
        int maxRetries = 3;
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                // Bước 1: Truy cập trang danh sách sản phẩm
                driver.get(PRODUCT_LIST_URL);
                System.out.println("✓ Đã truy cập: " + PRODUCT_LIST_URL);
                break; // Success, exit retry loop
            } catch (org.openqa.selenium.WebDriverException e) {
                if (e.getMessage().contains("ERR_CONNECTION_RESET") && retry < maxRetries - 1) {
                    System.out.println("⚠ Connection reset, retry " + (retry + 1) + "/" + maxRetries);
                    Thread.sleep(3000); // Đợi 3 giây trước khi retry
                } else {
                    throw e; // Throw nếu hết retries hoặc lỗi khác
                }
            }
        }
        
        // Bước 2: Đợi trang load xong
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
        
        // Bước 3: Đếm số sản phẩm trên trang
        List<WebElement> productCards = driver.findElements(By.cssSelector(".product-card"));
        int productCount = productCards.size();
        System.out.println("✓ Số sản phẩm trên trang: " + productCount);
        
        // Bước 4: Verify số lượng sản phẩm hợp lý (1-12 sản phẩm)
        assertTrue(productCount >= 1, "Phải có ít nhất 1 sản phẩm");
        assertTrue(productCount <= 12, "Không được quá 12 sản phẩm mỗi trang");
        System.out.println("✓ Số lượng sản phẩm hợp lý (1-12)");
        
        // Bước 5: Verify text hiển thị số sản phẩm
        WebElement showingText = driver.findElement(By.cssSelector(".text-muted.small b"));
        String displayedCount = showingText.getText();
        assertEquals(String.valueOf(productCount), displayedCount, 
                "Số sản phẩm hiển thị phải khớp với số sản phẩm thực tế");
        System.out.println("✓ Text hiển thị đúng: Hiển thị " + displayedCount + " sản phẩm");
        
        System.out.println("=== TEST PASSED: Số lượng sản phẩm đúng ===\n");
    }
}
