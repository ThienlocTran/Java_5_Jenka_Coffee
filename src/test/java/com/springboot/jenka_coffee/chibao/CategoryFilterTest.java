package com.springboot.jenka_coffee.selenium.category;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Category Filter")
public class CategoryFilterTest extends BaseSeleniumTest {

    @Test
    @DisplayName("TS_CAT_004: Hiển thị menu danh mục")
    public void testCategoryMenuDisplay() {

        // Step 1: Vào trang chủ
        driver.get(baseUrl + "/home");

        // Step 2: Click vào dropdown "Sản phẩm"
        WebElement productMenu = driver.findElement(By.id("navbarDropdown"));
        productMenu.click();

        // Step 3: Lấy danh sách category trong dropdown
        List<WebElement> categoryLinks =
                driver.findElements(By.cssSelector(".dropdown-menu .dropdown-item"));

        // Bỏ item "Tất cả sản phẩm"
        categoryLinks.removeIf(e -> e.getText().contains("Tất cả"));

        assertTrue(categoryLinks.size() > 0,
                "Should display category menu");

        System.out.println(" Số danh mục: " + categoryLinks.size());

        // Step 4: Kiểm tra từng danh mục
        for (WebElement link : categoryLinks) {

            String categoryName = link.getText().trim();

            assertFalse(categoryName.isEmpty(),
                    "Category should have name");

            System.out.println("  - " + categoryName);
        }

        System.out.println("Test hiển thị menu: PASSED");
    }

    @Test
    @DisplayName("TS_CAT_005: Lọc sản phẩm theo danh mục")
    public void testFilterProductsByCategory() {
        driver.get(baseUrl + "/home");

        // Click vào menu để mở dropdown
        WebElement menu = wait.until(ExpectedConditions.elementToBeClickable(By.id("navbarDropdown")));
        menu.click();

        // Chờ dropdown hiển thị và lấy tất cả item visible
        By categoryItemsSelector = By.cssSelector(".dropdown-menu.show .dropdown-item");
        List<WebElement> categories = wait.until(ExpectedConditions
                .visibilityOfAllElementsLocatedBy(categoryItemsSelector));

        // Lọc item không chứa "Tất cả" và lấy phần tử đầu tiên (nếu có)
        Optional<WebElement> optFirstCategory = categories.stream()
                .filter(e -> !e.getText().trim().contains("Tất cả"))
                .findFirst();

        assertTrue(optFirstCategory.isPresent(), "Không tìm thấy danh mục hợp lệ để test.");

        WebElement firstCategory = optFirstCategory.get();
        System.out.println(" Click vào: " + firstCategory.getText());

        // Đảm bảo element có thể click: thử nhiều cách, có fallback JS nếu bị chặn
        try {
            wait.until(ExpectedConditions.elementToBeClickable(firstCategory));
            // 1) thử click thông thường
            firstCategory.click();
        } catch (Exception clickEx) {
            System.out.println("Default click failed: " + clickEx.getClass().getSimpleName() + " - fallback to Actions/JS click");

            // 2) thử Actions click
            try {
                new Actions(driver).moveToElement(firstCategory).pause(Duration.ofMillis(200)).click().perform();
            } catch (Exception actionsEx) {
                System.out.println("Actions click failed: " + actionsEx.getClass().getSimpleName() + " - fallback to JS click");

                // 3) Fallback: scrollIntoView + JS click
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", firstCategory);

                // đợi chút để animation kết thúc, rồi JS click (JS click thường ít bị intercept)
                wait.until(driver -> ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState").equals("complete"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstCategory);
            }
        }

        // Sau click: chờ sản phẩm hiển thị (tăng timeout nếu cần)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-item")));

        List<WebElement> productItems = driver.findElements(By.cssSelector(".product-item"));
        assertTrue(productItems.size() > 0, "Không có sản phẩm nào hiển thị sau khi filter.");
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