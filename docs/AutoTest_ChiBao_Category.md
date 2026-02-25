# 🤖 TEST SCRIPTS TỰ ĐỘNG - CHÍ BẢO (Category Module)

**Thành viên:** Chí Bảo  
**Module:** Category Management  
**Số lượng:** 2 Test Scripts  
**Công nghệ:** Selenium WebDriver + JUnit 5

---

## 📋 DANH SÁCH TEST SCRIPTS

| ID | Tên Test Script | Mô tả | URL Test |
|---|---|---|---|
| **TS_CAT_001** | CategoryCRUDTest | Test CRUD danh mục (Admin) | `/admin/category/*` |
| **TS_CAT_002** | CategoryFilterTest | Test lọc SP theo danh mục (User) | `/product/list?categoryId=*` |

---

## ✅ TEST SCRIPT 1: CategoryCRUDTest.java

```java
package com.springboot.jenka_coffee.selenium.category;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Category CRUD")
public class CategoryCRUDTest extends BaseSeleniumTest {
    
    @BeforeEach
    public void loginAsAdmin() {
        driver.get(baseUrl + "/auth/login");
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/admin"));
    }
    
    @Test
    @DisplayName("TS_CAT_001: Thêm danh mục mới")
    public void testCreateCategory() {
        // Step 1: Vào trang danh sách danh mục
        driver.get(baseUrl + "/admin/category/list");
        
        // Step 2: Click nút "Thêm danh mục"
        WebElement addBtn = driver.findElement(By.cssSelector("a[href*='/add']"));
        addBtn.click();
        
        wait.until(ExpectedConditions.urlContains("/admin/category/add"));
        
        // Step 3: Điền form
        String categoryId = "TEST_" + System.currentTimeMillis();
        String categoryName = "Danh mục Test";
        
        driver.findElement(By.name("id")).sendKeys(categoryId);
        driver.findElement(By.name("name")).sendKeys(categoryName);
        
        System.out.println("📝 Tạo danh mục: " + categoryId + " - " + categoryName);
        
        // Step 4: Submit form
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        
        // Step 5: Wait for redirect
        wait.until(ExpectedConditions.urlContains("/admin/category/list"));
        
        // Step 6: Verify danh mục mới xuất hiện
        WebElement categoryTable = driver.findElement(By.cssSelector("table"));
        String tableText = categoryTable.getText();
        
        assertTrue(tableText.contains(categoryId),
                "Category list should contain new category ID");
        assertTrue(tableText.contains(categoryName),
                "Category list should contain new category name");
        
        System.out.println("✅ Test thêm danh mục: PASSED");
    }
    
    @Test
    @DisplayName("TS_CAT_002: Sửa danh mục")
    public void testUpdateCategory() {
        // Prerequisite: Tạo danh mục test trước
        String categoryId = createTestCategoryHelper();
        
        // Step 1: Vào trang danh sách
        driver.get(baseUrl + "/admin/category/list");
        
        // Step 2: Click nút "Sửa" cho danh mục vừa tạo
        WebElement editBtn = driver.findElement(
                By.cssSelector("a[href*='/edit/" + categoryId + "']"));
        editBtn.click();
        
        wait.until(ExpectedConditions.urlContains("/edit/" + categoryId));
        
        // Step 3: Sửa tên danh mục
        WebElement nameInput = driver.findElement(By.name("name"));
        nameInput.clear();
        String newName = "Danh mục đã sửa";
        nameInput.sendKeys(newName);
        
        System.out.println("✏️ Sửa tên thành: " + newName);
        
        // Step 4: Submit
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/admin/category/list"));
        
        // Step 5: Verify tên đã thay đổi
        WebElement categoryTable = driver.findElement(By.cssSelector("table"));
        assertTrue(categoryTable.getText().contains(newName),
                "Category name should be updated");
        
        System.out.println("✅ Test sửa danh mục: PASSED");
    }
    
    @Test
    @DisplayName("TS_CAT_003: Xóa danh mục rỗng")
    public void testDeleteEmptyCategory() {
        // Prerequisite: Tạo danh mục test (không có SP)
        String categoryId = createTestCategoryHelper();
        
        // Step 1: Vào trang danh sách
        driver.get(baseUrl + "/admin/category/list");
        
        // Step 2: Click nút "Xóa"
        WebElement deleteBtn = driver.findElement(
                By.cssSelector("button[onclick*='delete/" + categoryId + "']"));
        deleteBtn.click();
        
        // Step 3: Confirm dialog (nếu có)
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        
        // Step 4: Wait for page reload
        wait.until(ExpectedConditions.stalenessOf(deleteBtn));
        
        // Step 5: Verify danh mục đã bị xóa
        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(categoryId),
                "Category should be deleted from list");
        
        System.out.println("✅ Test xóa danh mục: PASSED");
    }
    
    // Helper method
    private String createTestCategoryHelper() {
        driver.get(baseUrl + "/admin/category/add");
        
        String categoryId = "TEST_" + System.currentTimeMillis();
        driver.findElement(By.name("id")).sendKeys(categoryId);
        driver.findElement(By.name("name")).sendKeys("Test Category");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/admin/category/list"));
        return categoryId;
    }
}
```

---

## ✅ TEST SCRIPT 2: CategoryFilterTest.java

```java
package com.springboot.jenka_coffee.selenium.category;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Category Filter")
public class CategoryFilterTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_CAT_004: Hiển thị menu danh mục")
    public void testCategoryMenuDisplay() {
        // Step 1: Vào trang chủ
        driver.get(baseUrl + "/");
        
        // Step 2: Verify có menu danh mục
        List<WebElement> categoryLinks = driver.findElements(
                By.cssSelector(".category-menu a"));
        
        assertTrue(categoryLinks.size() > 0,
                "Should display category menu");
        
        System.out.println("📂 Số danh mục: " + categoryLinks.size());
        
        // Step 3: Verify mỗi danh mục có icon và tên
        for (WebElement link : categoryLinks) {
            WebElement icon = link.findElement(By.cssSelector("img, i"));
            assertNotNull(icon, "Category should have icon");
            
            String categoryName = link.getText();
            assertFalse(categoryName.isEmpty(),
                    "Category should have name");
            
            System.out.println("  - " + categoryName);
        }
        
        System.out.println("✅ Test hiển thị menu: PASSED");
    }
    
    @Test
    @DisplayName("TS_CAT_005: Lọc sản phẩm theo danh mục")
    public void testFilterProductsByCategory() {
        // Step 1: Vào trang chủ
        driver.get(baseUrl + "/");
        
        // Step 2: Click vào danh mục đầu tiên
        WebElement firstCategory = driver.findElement(
                By.cssSelector(".category-menu a"));
        String categoryName = firstCategory.getText();
        String categoryId = firstCategory.getAttribute("data-category-id");
        
        System.out.println("🔍 Click vào danh mục: " + categoryName);
        
        firstCategory.click();
        
        // Step 3: Wait for filter
        wait.until(ExpectedConditions.urlContains("categoryId="));
        
        // Step 4: Verify URL có categoryId
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("categoryId="),
                "URL should contain categoryId parameter");
        
        // Step 5: Verify có sản phẩm
        List<WebElement> products = driver.findElements(
                By.cssSelector(".product-item"));
        
        assertTrue(products.size() > 0,
                "Should display products in this category");
        
        System.out.println("📦 Số sản phẩm: " + products.size());
        
        // Step 6: Verify breadcrumb hoặc title hiển thị tên danh mục
        WebElement pageTitle = driver.findElement(By.cssSelector("h1, .page-title"));
        String titleText = pageTitle.getText();
        
        assertTrue(titleText.contains(categoryName),
                "Page title should contain category name");
        
        System.out.println("✅ Test lọc theo danh mục: PASSED");
    }
    
    @Test
    @DisplayName("TS_CAT_006: Đếm số sản phẩm theo danh mục")
    public void testCategoryProductCount() {
        // Step 1: Vào trang chủ
        driver.get(baseUrl + "/");
        
        // Step 2: Lấy danh sách danh mục có hiển thị số lượng
        List<WebElement> categoryLinks = driver.findElements(
                By.cssSelector(".category-menu a"));
        
        for (WebElement link : categoryLinks) {
            String categoryName = link.getText();
            
            // Step 3: Click vào danh mục
            link.click();
            wait.until(ExpectedConditions.urlContains("categoryId="));
            
            // Step 4: Đếm số sản phẩm thực tế
            int actualCount = driver.findElements(
                    By.cssSelector(".product-item")).size();
            
            System.out.println("📊 " + categoryName + ": " + actualCount + " sản phẩm");
            
            // Step 5: Quay lại trang chủ
            driver.get(baseUrl + "/");
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".category-menu")));
            
            // Refresh category links
            categoryLinks = driver.findElements(
                    By.cssSelector(".category-menu a"));
        }
        
        System.out.println("✅ Test đếm sản phẩm: PASSED");
    }
}
```

---

## 🚀 CÁCH CHẠY

```bash
mvn test -Dtest=CategoryCRUDTest
mvn test -Dtest=CategoryFilterTest
```

---

## 📊 KẾT QUẢ MONG ĐỢI

```
✅ CategoryCRUDTest
   ├─ TS_CAT_001: Thêm danh mục ✓
   ├─ TS_CAT_002: Sửa danh mục ✓
   └─ TS_CAT_003: Xóa danh mục ✓

✅ CategoryFilterTest
   ├─ TS_CAT_004: Hiển thị menu ✓
   ├─ TS_CAT_005: Lọc theo danh mục ✓
   └─ TS_CAT_006: Đếm sản phẩm ✓

Total: 6 tests, 6 passed ✅
```

---

**Người tạo:** Chí Bảo  
**Ngày:** 2026-02-25
