package com.springboot.jenka_coffee.selenium.category;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Category CRUD")
public class AutoMated_Test_CategoryCRUD extends BaseSeleniumTest {

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

        // Step 1: Vào trang danh sách
        driver.get(baseUrl + "/admin/category/list");

        // Step 2: Click nút thêm
        WebElement addBtn = driver.findElement(By.cssSelector("a[href*='/add']"));
        addBtn.click();

        wait.until(ExpectedConditions.urlContains("/admin/category/add"));

        // Step 3: Điền form
        String categoryId = "TEST_ID";
        String categoryName = "Danh mục Test";

        driver.findElement(By.name("id")).sendKeys(categoryId);
        driver.findElement(By.name("name")).sendKeys(categoryName);

        // Step 4: Submit
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Step 5: Wait redirect
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            wait.until(ExpectedConditions.urlContains("/admin/category/list"));
        } catch (TimeoutException e) {
            // Không cho crash
        }

        assertTrue(driver.getCurrentUrl().contains("/admin/category/list"));

        // Step 6: Verify
        WebElement categoryTable = driver.findElement(By.cssSelector("table"));
        String tableText = categoryTable.getText();

        assertTrue(tableText.contains(categoryName),
                "Category list should contain new category name");

        System.out.println("Test thêm danh mục: PASSED");
    }

    @Test
    @DisplayName("TS_CAT_002: Sửa danh mục")
    public void testUpdateCategory() {

        String categoryId = "XAY_EP";

        // Step 1: Vào trang danh sách
        driver.get(baseUrl + "/admin/category/list");

        // Step 2: Kiểm tra và click nút Edit
        List<WebElement> editButtons = driver.findElements(
                By.cssSelector("a[href*='/edit/" + categoryId + "']"));

        assertFalse(editButtons.isEmpty(),
                "Không tìm thấy nút Edit với ID: " + categoryId);

        editButtons.get(0).click();

        // Chờ chuyển trang
        wait.until(ExpectedConditions.urlContains("/edit/" + categoryId));

        // Assert đã vào đúng trang edit
        assertTrue(driver.getCurrentUrl().contains("/edit/" + categoryId),
                "Phải chuyển đến trang edit");

        // Step 3: Kiểm tra input name tồn tại
        List<WebElement> nameInputs = driver.findElements(By.name("name"));

        assertFalse(nameInputs.isEmpty(),
                "Không tìm thấy ô nhập tên danh mục");

        WebElement nameInput = nameInputs.get(0);
        nameInput.clear();

        String newName = "Danh mục đã sửa";
        nameInput.sendKeys(newName);

        System.out.println("Sửa tên thành: " + newName);

        // Step 4: Kiểm tra và click nút Submit
        List<WebElement> submitButtons =
                driver.findElements(By.cssSelector("button[type='submit']"));

        assertFalse(submitButtons.isEmpty(),
                "Không tìm thấy nút Submit");

        submitButtons.get(0).click();

        // Chờ quay về list
        wait.until(ExpectedConditions.urlContains("/admin/category/list"));

        //Assert đã quay về trang list
        assertTrue(driver.getCurrentUrl().contains("/admin/category/list"),
                "Phải quay về trang danh sách");

        // Step 5: Verify tên đã thay đổi
        List<WebElement> tables = driver.findElements(By.cssSelector("table"));

        assertFalse(tables.isEmpty(),
                "Không tìm thấy bảng danh mục");

        assertTrue(tables.get(0).getText().contains(newName),
                "Category name should be updated");

        System.out.println("Test sửa danh mục: PASSED");
    }

    @Test
    @DisplayName("TS_CAT_003: Xóa danh mục rỗng")
    public void testDeleteEmptyCategory() {

        // ===== Bước 1: Tạo category test =====
        String categoryId = createTestCategoryHelper();

        assertTrue(categoryId != null && !categoryId.isEmpty(),
                "Không tạo được category test");

        // ===== Bước 2: Vào trang danh sách =====
        driver.get(baseUrl + "/admin/category/list");

        assertTrue(driver.getCurrentUrl().contains("/admin/category/list"),
                "Không vào được trang list");

        // ===== Bước 3: Tìm nút Xóa =====
        List<WebElement> deleteButtons = driver.findElements(
                By.cssSelector("button[onclick*='delete/" + categoryId + "']"));

        assertTrue(deleteButtons.size() > 0,
                "Không tìm thấy nút Xóa");

        // Click nút xóa
        deleteButtons.get(0).click();

        // ===== Bước 4: Kiểm tra alert =====
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
        } catch (Exception e) {
            assertTrue(false, "Không xuất hiện confirm alert");
        }

        // ===== Bước 5: Chờ quay lại trang list =====
        try {
            wait.until(ExpectedConditions.urlContains("/admin/category/list"));
        } catch (Exception e) {
            assertTrue(false, "Không redirect về trang list");
        }

        // ===== Bước 6: Kiểm tra đã xóa =====
        String pageSource = driver.getPageSource();

        assertTrue(!pageSource.contains(categoryId),
                "Category vẫn còn tồn tại sau khi xóa");

        System.out.println("Test xóa danh mục: PASSED");
    }

    private String createTestCategoryHelper() {

        driver.get(baseUrl + "/admin/category/add");

        String categoryId = "TEST_ID" ;

        driver.findElement(By.name("id")).clear();
        driver.findElement(By.name("id")).sendKeys(categoryId);

        driver.findElement(By.name("name")).clear();
        driver.findElement(By.name("name")).sendKeys("Test Category");

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        boolean redirected = false;

        try {
            wait.until(driver ->
                    driver.getCurrentUrl().contains("/admin/category/list") ||
                            driver.getCurrentUrl().contains("/admin/category/save")
            );

            redirected = driver.getCurrentUrl().contains("/admin/category/list");

        } catch (Exception ignored) {
            redirected = false;
        }

        if (!redirected) {
            return null; // không tạo được → để test FAIL
        }

        return categoryId;
    }
}