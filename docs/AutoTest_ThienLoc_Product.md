# 🤖 TEST SCRIPTS TỰ ĐỘNG - THIÊN LỘC (Product Module)

**Thành viên:** Thiên Lộc  
**Module:** Product Management  
**Số lượng:** 2 Test Scripts  
**Công nghệ:** Selenium WebDriver + JUnit 5

---

## 📋 DANH SÁCH TEST SCRIPTS

| ID | Tên Test Script | Mô tả | URL Test |
|---|---|---|---|
| **TS_PROD_001** | ProductListTest | Test hiển thị và phân trang danh sách SP | `/product/list` |
| **TS_PROD_002** | ProductSearchTest | Test tìm kiếm và filter sản phẩm | `/product/search` |

---

## 🔧 SETUP - BaseSeleniumTest.java

```java
package com.springboot.jenka_coffee.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BaseSeleniumTest {
    
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl = "http://localhost:8080";
    
    @BeforeEach
    public void setUp() {
        // Tự động download ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        // Cấu hình Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        // options.addArguments("--headless"); // Uncomment để chạy không hiển thị browser
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        // Set implicit wait
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    // Helper method: Login as admin
    protected void loginAsAdmin() {
        driver.get(baseUrl + "/auth/login");
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }
}
```

---

## ✅ TEST SCRIPT 1: ProductListTest.java

### Mô tả
Test hiển thị danh sách sản phẩm, phân trang, và sắp xếp

### Code đầy đủ

```java
package com.springboot.jenka_coffee.selenium.product;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Product List")
public class ProductListTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_PROD_001: Hiển thị danh sách sản phẩm")
    public void testProductListDisplay() {
        // Step 1: Truy cập trang danh sách sản phẩm
        driver.get(baseUrl + "/product/list");
        
        // Step 2: Verify page title
        String pageTitle = driver.getTitle();
        assertTrue(pageTitle.contains("Sản phẩm") || pageTitle.contains("Product"),
                "Page title should contain 'Sản phẩm' or 'Product'");
        
        // Step 3: Verify có hiển thị sản phẩm
        List<WebElement> products = driver.findElements(By.cssSelector(".product-item"));
        assertTrue(products.size() > 0, "Should display at least 1 product");
        assertTrue(products.size() <= 12, "Should display maximum 12 products per page");
        
        System.out.println("✅ Số sản phẩm hiển thị: " + products.size());
        
        // Step 4: Verify mỗi sản phẩm có đầy đủ thông tin
        WebElement firstProduct = products.get(0);
        assertNotNull(firstProduct.findElement(By.cssSelector(".product-name")),
                "Product should have name");
        assertNotNull(firstProduct.findElement(By.cssSelector(".product-price")),
                "Product should have price");
        assertNotNull(firstProduct.findElement(By.cssSelector(".product-image")),
                "Product should have image");
        
        System.out.println("✅ Test hiển thị danh sách sản phẩm: PASSED");
    }
    
    @Test
    @DisplayName("TS_PROD_002: Test phân trang sản phẩm")
    public void testProductPagination() {
        // Step 1: Truy cập trang 1
        driver.get(baseUrl + "/product/list?page=0");
        
        // Step 2: Lấy số sản phẩm trang 1
        List<WebElement> page1Products = driver.findElements(By.cssSelector(".product-item"));
        int page1Count = page1Products.size();
        System.out.println("📄 Trang 1 có: " + page1Count + " sản phẩm");
        
        // Step 3: Kiểm tra có nút phân trang không
        List<WebElement> paginationLinks = driver.findElements(By.cssSelector(".pagination a"));
        
        if (paginationLinks.size() > 0) {
            // Step 4: Click sang trang 2
            WebElement page2Link = driver.findElement(By.cssSelector("a[href*='page=1']"));
            page2Link.click();
            
            // Step 5: Wait for page load
            wait.until(ExpectedConditions.urlContains("page=1"));
            
            // Step 6: Verify URL changed
            String currentUrl = driver.getCurrentUrl();
            assertTrue(currentUrl.contains("page=1"), "URL should contain page=1");
            
            // Step 7: Verify có sản phẩm trang 2
            List<WebElement> page2Products = driver.findElements(By.cssSelector(".product-item"));
            assertTrue(page2Products.size() > 0, "Page 2 should have products");
            
            System.out.println("📄 Trang 2 có: " + page2Products.size() + " sản phẩm");
            System.out.println("✅ Test phân trang: PASSED");
        } else {
            System.out.println("ℹ️ Không có phân trang (ít hơn 12 sản phẩm)");
        }
    }
    
    @Test
    @DisplayName("TS_PROD_003: Test click vào sản phẩm")
    public void testClickProductDetail() {
        // Step 1: Vào trang danh sách
        driver.get(baseUrl + "/product/list");
        
        // Step 2: Lấy sản phẩm đầu tiên
        WebElement firstProduct = driver.findElement(By.cssSelector(".product-item"));
        String productName = firstProduct.findElement(By.cssSelector(".product-name")).getText();
        
        System.out.println("🔍 Click vào sản phẩm: " + productName);
        
        // Step 3: Click vào sản phẩm
        firstProduct.findElement(By.cssSelector("a")).click();
        
        // Step 4: Wait for detail page
        wait.until(ExpectedConditions.urlContains("/product/detail/"));
        
        // Step 5: Verify đã chuyển sang trang chi tiết
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/product/detail/"), 
                "Should navigate to product detail page");
        
        // Step 6: Verify có nút "Thêm vào giỏ"
        WebElement addToCartBtn = driver.findElement(By.cssSelector("button[onclick*='cart']"));
        assertNotNull(addToCartBtn, "Should have 'Add to Cart' button");
        
        System.out.println("✅ Test click sản phẩm: PASSED");
    }
}
```

---

## ✅ TEST SCRIPT 2: ProductSearchTest.java

### Mô tả
Test tìm kiếm và lọc sản phẩm theo keyword, category, giá

### Code đầy đủ

```java
package com.springboot.jenka_coffee.selenium.product;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Product Search & Filter")
public class ProductSearchTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_PROD_004: Tìm kiếm sản phẩm theo keyword")
    public void testSearchByKeyword() {
        // Step 1: Vào trang chủ
        driver.get(baseUrl + "/product/list");
        
        // Step 2: Tìm ô search
        WebElement searchBox = driver.findElement(By.name("keyword"));
        assertNotNull(searchBox, "Search box should exist");
        
        // Step 3: Nhập từ khóa "Espresso"
        String keyword = "Espresso";
        searchBox.sendKeys(keyword);
        searchBox.sendKeys(Keys.ENTER);
        
        System.out.println("🔍 Tìm kiếm: " + keyword);
        
        // Step 4: Wait for results
        wait.until(ExpectedConditions.urlContains("keyword=" + keyword));
        
        // Step 5: Verify URL có keyword
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("keyword=" + keyword),
                "URL should contain search keyword");
        
        // Step 6: Verify kết quả tìm kiếm
        List<WebElement> results = driver.findElements(By.cssSelector(".product-item"));
        
        if (results.size() > 0) {
            // Verify tên sản phẩm chứa keyword
            for (WebElement product : results) {
                String productName = product.findElement(By.cssSelector(".product-name"))
                        .getText().toLowerCase();
                assertTrue(productName.contains(keyword.toLowerCase()),
                        "Product name should contain keyword: " + keyword);
            }
            System.out.println("✅ Tìm thấy " + results.size() + " sản phẩm");
        } else {
            // Verify có thông báo "Không tìm thấy"
            WebElement noResultMsg = driver.findElement(By.cssSelector(".no-result-message"));
            assertNotNull(noResultMsg, "Should show 'No results' message");
            System.out.println("ℹ️ Không tìm thấy sản phẩm");
        }
        
        System.out.println("✅ Test tìm kiếm: PASSED");
    }
    
    @Test
    @DisplayName("TS_PROD_005: Lọc sản phẩm theo category")
    public void testFilterByCategory() {
        // Step 1: Vào trang sản phẩm
        driver.get(baseUrl + "/product/list");
        
        // Step 2: Tìm dropdown category (hoặc link category)
        List<WebElement> categoryLinks = driver.findElements(By.cssSelector(".category-filter a"));
        
        if (categoryLinks.size() > 0) {
            // Step 3: Click vào category đầu tiên
            WebElement firstCategory = categoryLinks.get(0);
            String categoryName = firstCategory.getText();
            String categoryId = firstCategory.getAttribute("data-category-id");
            
            System.out.println("📂 Lọc theo category: " + categoryName);
            
            firstCategory.click();
            
            // Step 4: Wait for filter
            wait.until(ExpectedConditions.urlContains("categoryId="));
            
            // Step 5: Verify URL có categoryId
            String currentUrl = driver.getCurrentUrl();
            assertTrue(currentUrl.contains("categoryId="),
                    "URL should contain categoryId parameter");
            
            // Step 6: Verify có sản phẩm
            List<WebElement> filteredProducts = driver.findElements(By.cssSelector(".product-item"));
            assertTrue(filteredProducts.size() > 0,
                    "Should have products in this category");
            
            System.out.println("✅ Tìm thấy " + filteredProducts.size() + " sản phẩm trong category");
            System.out.println("✅ Test lọc category: PASSED");
        } else {
            System.out.println("⚠️ Không tìm thấy category filter");
        }
    }
    
    @Test
    @DisplayName("TS_PROD_006: Lọc sản phẩm theo khoảng giá")
    public void testFilterByPriceRange() {
        // Step 1: Vào trang filter
        driver.get(baseUrl + "/product/filter");
        
        // Step 2: Nhập giá min
        WebElement minPriceInput = driver.findElement(By.name("minPrice"));
        minPriceInput.clear();
        minPriceInput.sendKeys("1000000");
        
        // Step 3: Nhập giá max
        WebElement maxPriceInput = driver.findElement(By.name("maxPrice"));
        maxPriceInput.clear();
        maxPriceInput.sendKeys("5000000");
        
        System.out.println("💰 Lọc giá: 1.000.000đ - 5.000.000đ");
        
        // Step 4: Click nút "Lọc"
        WebElement filterBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        filterBtn.click();
        
        // Step 5: Wait for results
        wait.until(ExpectedConditions.urlContains("minPrice="));
        
        // Step 6: Verify URL
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("minPrice=1000000"),
                "URL should contain minPrice");
        assertTrue(currentUrl.contains("maxPrice=5000000"),
                "URL should contain maxPrice");
        
        // Step 7: Verify kết quả
        List<WebElement> filteredProducts = driver.findElements(By.cssSelector(".product-item"));
        System.out.println("✅ Tìm thấy " + filteredProducts.size() + " sản phẩm trong khoảng giá");
        
        // Step 8: Verify giá sản phẩm nằm trong khoảng
        for (WebElement product : filteredProducts) {
            String priceText = product.findElement(By.cssSelector(".product-price"))
                    .getText().replaceAll("[^0-9]", "");
            long price = Long.parseLong(priceText);
            
            assertTrue(price >= 1000000 && price <= 5000000,
                    "Product price should be in range 1M-5M");
        }
        
        System.out.println("✅ Test lọc giá: PASSED");
    }
}
```

---

## 🚀 CÁCH CHẠY TEST

### 1. Chạy từ IDE (IntelliJ/Eclipse)
```
Right-click vào file test → Run 'ProductListTest'
```

### 2. Chạy từ Maven
```bash
mvn test -Dtest=ProductListTest
mvn test -Dtest=ProductSearchTest
```

### 3. Chạy tất cả Selenium tests
```bash
mvn test -Dtest="**/selenium/**/*Test.java"
```

---

## 📊 KẾT QUẢ MONG ĐỢI

```
✅ ProductListTest
   ├─ TS_PROD_001: Hiển thị danh sách sản phẩm ✓
   ├─ TS_PROD_002: Test phân trang sản phẩm ✓
   └─ TS_PROD_003: Test click vào sản phẩm ✓

✅ ProductSearchTest
   ├─ TS_PROD_004: Tìm kiếm theo keyword ✓
   ├─ TS_PROD_005: Lọc theo category ✓
   └─ TS_PROD_006: Lọc theo khoảng giá ✓

Total: 6 tests, 6 passed ✅
```

---

**Người tạo:** Thiên Lộc  
**Reviewer:** Kiro AI  
**Ngày:** 2026-02-25
