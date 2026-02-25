# 🤖 TEST SCRIPTS TỰ ĐỘNG - TUẤN KHOA (Cart & Checkout Module)

**Thành viên:** Tuấn Khoa  
**Module:** Shopping Cart & Checkout  
**Số lượng:** 2 Test Scripts  
**Công nghệ:** Selenium WebDriver + JUnit 5

---

## 📋 DANH SÁCH TEST SCRIPTS

| ID | Tên Test Script | Mô tả | URL Test |
|---|---|---|---|
| **TS_CART_001** | AddToCartTest | Test thêm SP vào giỏ, cập nhật số lượng | `/cart/*` |
| **TS_CART_002** | CheckoutFlowTest | Test toàn bộ luồng checkout | `/checkout` |

---

## ✅ TEST SCRIPT 1: AddToCartTest.java

### Mô tả
Test thêm sản phẩm vào giỏ hàng, cập nhật số lượng, xóa sản phẩm

### Code đầy đủ

```java
package com.springboot.jenka_coffee.selenium.cart;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Add to Cart")
public class AddToCartTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_CART_001: Thêm sản phẩm vào giỏ hàng")
    public void testAddProductToCart() {
        // Step 1: Vào trang danh sách sản phẩm
        driver.get(baseUrl + "/product/list");
        
        // Step 2: Lấy sản phẩm đầu tiên
        WebElement firstProduct = driver.findElement(By.cssSelector(".product-item"));
        String productName = firstProduct.findElement(By.cssSelector(".product-name")).getText();
        
        System.out.println("🛒 Thêm vào giỏ: " + productName);
        
        // Step 3: Click nút "Thêm vào giỏ"
        WebElement addToCartBtn = firstProduct.findElement(By.cssSelector("button[onclick*='cart/add']"));
        addToCartBtn.click();
        
        // Step 4: Wait for cart update (có thể là AJAX hoặc redirect)
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/cart/view"),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".cart-badge"))
        ));
        
        // Step 5: Verify cart badge tăng lên
        WebElement cartBadge = driver.findElement(By.cssSelector(".cart-badge"));
        String cartCount = cartBadge.getText();
        assertTrue(Integer.parseInt(cartCount) > 0, "Cart count should be greater than 0");
        
        System.out.println("✅ Số lượng trong giỏ: " + cartCount);
        
        // Step 6: Vào trang giỏ hàng
        driver.get(baseUrl + "/cart/view");
        
        // Step 7: Verify sản phẩm có trong giỏ
        WebElement cartItem = driver.findElement(By.cssSelector(".cart-item"));
        String cartProductName = cartItem.findElement(By.cssSelector(".cart-item-name")).getText();
        
        assertTrue(cartProductName.contains(productName),
                "Cart should contain the added product");
        
        System.out.println("✅ Test thêm vào giỏ: PASSED");
    }
    
    @Test
    @DisplayName("TS_CART_002: Cập nhật số lượng sản phẩm trong giỏ")
    public void testUpdateCartQuantity() {
        // Prerequisite: Thêm 1 sản phẩm vào giỏ trước
        addProductToCartHelper();
        
        // Step 1: Vào trang giỏ hàng
        driver.get(baseUrl + "/cart/view");
        
        // Step 2: Lấy số lượng hiện tại
        WebElement qtyInput = driver.findElement(By.cssSelector("input[name='quantity']"));
        String currentQty = qtyInput.getAttribute("value");
        System.out.println("📦 Số lượng hiện tại: " + currentQty);
        
        // Step 3: Lấy tổng tiền hiện tại
        WebElement totalElement = driver.findElement(By.cssSelector(".cart-total"));
        String currentTotal = totalElement.getText().replaceAll("[^0-9]", "");
        long currentTotalAmount = Long.parseLong(currentTotal);
        
        // Step 4: Tăng số lượng lên 3
        qtyInput.clear();
        qtyInput.sendKeys("3");
        
        // Step 5: Click nút "Cập nhật"
        WebElement updateBtn = driver.findElement(By.cssSelector("button[onclick*='update']"));
        updateBtn.click();
        
        // Step 6: Wait for page reload
        wait.until(ExpectedConditions.stalenessOf(qtyInput));
        
        // Step 7: Verify số lượng đã thay đổi
        WebElement newQtyInput = driver.findElement(By.cssSelector("input[name='quantity']"));
        String newQty = newQtyInput.getAttribute("value");
        assertEquals("3", newQty, "Quantity should be updated to 3");
        
        // Step 8: Verify tổng tiền tăng lên
        WebElement newTotalElement = driver.findElement(By.cssSelector(".cart-total"));
        String newTotal = newTotalElement.getText().replaceAll("[^0-9]", "");
        long newTotalAmount = Long.parseLong(newTotal);
        
        assertTrue(newTotalAmount > currentTotalAmount,
                "Total amount should increase after quantity update");
        
        System.out.println("💰 Tổng tiền mới: " + newTotalAmount);
        System.out.println("✅ Test cập nhật số lượng: PASSED");
    }
    
    @Test
    @DisplayName("TS_CART_003: Xóa sản phẩm khỏi giỏ hàng")
    public void testRemoveProductFromCart() {
        // Prerequisite: Thêm 1 sản phẩm vào giỏ
        addProductToCartHelper();
        
        // Step 1: Vào trang giỏ hàng
        driver.get(baseUrl + "/cart/view");
        
        // Step 2: Đếm số sản phẩm trong giỏ
        int initialCount = driver.findElements(By.cssSelector(".cart-item")).size();
        System.out.println("🗑️ Số SP trước khi xóa: " + initialCount);
        
        // Step 3: Click nút "Xóa"
        WebElement removeBtn = driver.findElement(By.cssSelector("button[onclick*='remove']"));
        removeBtn.click();
        
        // Step 4: Wait for page reload
        wait.until(ExpectedConditions.or(
            ExpectedConditions.numberOfElementsToBeLessThan(By.cssSelector(".cart-item"), initialCount),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(".empty-cart-message"))
        ));
        
        // Step 5: Verify sản phẩm đã bị xóa
        int finalCount = driver.findElements(By.cssSelector(".cart-item")).size();
        assertTrue(finalCount < initialCount, "Cart item count should decrease");
        
        System.out.println("🗑️ Số SP sau khi xóa: " + finalCount);
        System.out.println("✅ Test xóa sản phẩm: PASSED");
    }
    
    // Helper method
    private void addProductToCartHelper() {
        driver.get(baseUrl + "/product/list");
        WebElement firstProduct = driver.findElement(By.cssSelector(".product-item"));
        WebElement addBtn = firstProduct.findElement(By.cssSelector("button[onclick*='cart/add']"));
        addBtn.click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".cart-badge")));
    }
}
```

---

## ✅ TEST SCRIPT 2: CheckoutFlowTest.java

### Mô tả
Test toàn bộ luồng checkout từ giỏ hàng đến đặt hàng thành công

### Code đầy đủ

```java
package com.springboot.jenka_coffee.selenium.cart;

import com.springboot.jenka_coffee.selenium.BaseSeleniumTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Selenium Test - Checkout Flow")
public class CheckoutFlowTest extends BaseSeleniumTest {
    
    @Test
    @DisplayName("TS_CART_004: Checkout thành công (End-to-End)")
    public void testCompleteCheckoutFlow() {
        // ========== PHASE 1: LOGIN ==========
        System.out.println("🔐 Phase 1: Login");
        driver.get(baseUrl + "/auth/login");
        
        driver.findElement(By.name("username")).sendKeys("user1");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/home"));
        System.out.println("✅ Login thành công");
        
        // ========== PHASE 2: ADD TO CART ==========
        System.out.println("🛒 Phase 2: Add to Cart");
        driver.get(baseUrl + "/product/list");
        
        WebElement firstProduct = driver.findElement(By.cssSelector(".product-item"));
        String productName = firstProduct.findElement(By.cssSelector(".product-name")).getText();
        String productPrice = firstProduct.findElement(By.cssSelector(".product-price"))
                .getText().replaceAll("[^0-9]", "");
        
        System.out.println("📦 Sản phẩm: " + productName + " - " + productPrice + "đ");
        
        WebElement addBtn = firstProduct.findElement(By.cssSelector("button[onclick*='cart/add']"));
        addBtn.click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".cart-badge")));
        System.out.println("✅ Đã thêm vào giỏ");
        
        // ========== PHASE 3: VIEW CART ==========
        System.out.println("👀 Phase 3: View Cart");
        driver.get(baseUrl + "/cart/view");
        
        WebElement cartItem = driver.findElement(By.cssSelector(".cart-item"));
        assertNotNull(cartItem, "Cart should have items");
        
        WebElement checkoutBtn = driver.findElement(By.cssSelector("a[href*='/checkout']"));
        checkoutBtn.click();
        
        wait.until(ExpectedConditions.urlContains("/checkout"));
        System.out.println("✅ Đã vào trang checkout");
        
        // ========== PHASE 4: FILL CHECKOUT FORM ==========
        System.out.println("📝 Phase 4: Fill Checkout Form");
        
        // Fullname (có thể đã auto-fill)
        WebElement fullnameInput = driver.findElement(By.name("fullname"));
        if (fullnameInput.getAttribute("value").isEmpty()) {
            fullnameInput.sendKeys("Nguyễn Văn A");
        }
        
        // Email
        WebElement emailInput = driver.findElement(By.name("email"));
        if (emailInput.getAttribute("value").isEmpty()) {
            emailInput.sendKeys("user1@gmail.com");
        }
        
        // Phone
        WebElement phoneInput = driver.findElement(By.name("phone"));
        if (phoneInput.getAttribute("value").isEmpty()) {
            phoneInput.sendKeys("0901234567");
        }
        
        // Address
        driver.findElement(By.name("address")).sendKeys("123 Lê Lợi");
        
        // Province
        Select provinceSelect = new Select(driver.findElement(By.name("province")));
        provinceSelect.selectByVisibleText("TP. Hồ Chí Minh");
        
        // District
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("select[name='district'] option"), 1));
        Select districtSelect = new Select(driver.findElement(By.name("district")));
        districtSelect.selectByIndex(1);
        
        // Ward
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("select[name='ward'] option"), 1));
        Select wardSelect = new Select(driver.findElement(By.name("ward")));
        wardSelect.selectByIndex(1);
        
        // Payment method
        WebElement codRadio = driver.findElement(By.cssSelector("input[value='COD']"));
        codRadio.click();
        
        // Agree terms
        WebElement agreeCheckbox = driver.findElement(By.name("agreeTerms"));
        if (!agreeCheckbox.isSelected()) {
            agreeCheckbox.click();
        }
        
        System.out.println("✅ Đã điền form");
        
        // ========== PHASE 5: SUBMIT ORDER ==========
        System.out.println("🚀 Phase 5: Submit Order");
        
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        submitBtn.click();
        
        // Wait for success page
        wait.until(ExpectedConditions.urlContains("/checkout/success"));
        
        // ========== PHASE 6: VERIFY SUCCESS ==========
        System.out.println("✅ Phase 6: Verify Success");
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/checkout/success"),
                "Should redirect to success page");
        
        // Verify success message
        WebElement successMsg = driver.findElement(By.cssSelector(".success-message"));
        assertNotNull(successMsg, "Should show success message");
        
        String msgText = successMsg.getText();
        assertTrue(msgText.contains("thành công") || msgText.contains("success"),
                "Success message should contain 'thành công'");
        
        // Verify order ID
        WebElement orderIdElement = driver.findElement(By.cssSelector(".order-id"));
        String orderIdText = orderIdElement.getText();
        assertTrue(orderIdText.matches(".*\\d+.*"),
                "Should display order ID");
        
        System.out.println("🎉 Mã đơn hàng: " + orderIdText);
        
        // ========== PHASE 7: VERIFY CART CLEARED ==========
        System.out.println("🧹 Phase 7: Verify Cart Cleared");
        
        driver.get(baseUrl + "/cart/view");
        
        boolean cartEmpty = driver.findElements(By.cssSelector(".cart-item")).isEmpty();
        assertTrue(cartEmpty, "Cart should be empty after checkout");
        
        WebElement emptyMsg = driver.findElement(By.cssSelector(".empty-cart-message"));
        assertNotNull(emptyMsg, "Should show empty cart message");
        
        System.out.println("✅ Giỏ hàng đã được xóa");
        
        // ========== TEST PASSED ==========
        System.out.println("\n🎊 ========== CHECKOUT FLOW TEST PASSED ========== 🎊");
    }
    
    @Test
    @DisplayName("TS_CART_005: Checkout khi chưa login (Chặn)")
    public void testCheckoutWithoutLogin() {
        // Step 1: Thêm sản phẩm vào giỏ (không cần login)
        driver.get(baseUrl + "/product/list");
        WebElement firstProduct = driver.findElement(By.cssSelector(".product-item"));
        WebElement addBtn = firstProduct.findElement(By.cssSelector("button[onclick*='cart/add']"));
        addBtn.click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".cart-badge")));
        
        // Step 2: Cố truy cập trang checkout
        driver.get(baseUrl + "/checkout");
        
        // Step 3: Verify bị redirect về login
        wait.until(ExpectedConditions.urlContains("/auth/login"));
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/auth/login"),
                "Should redirect to login page");
        
        // Step 4: Verify có redirect parameter
        assertTrue(currentUrl.contains("redirect") || currentUrl.contains("returnUrl"),
                "URL should contain redirect parameter");
        
        System.out.println("✅ Test chặn checkout khi chưa login: PASSED");
    }
    
    @Test
    @DisplayName("TS_CART_006: Checkout với giỏ hàng trống (Chặn)")
    public void testCheckoutWithEmptyCart() {
        // Step 1: Login
        driver.get(baseUrl + "/auth/login");
        driver.findElement(By.name("username")).sendKeys("user1");
        driver.findElement(By.name("password")).sendKeys("123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("/home"));
        
        // Step 2: Clear cart (nếu có)
        driver.get(baseUrl + "/cart/clear");
        
        // Step 3: Cố truy cập checkout với giỏ trống
        driver.get(baseUrl + "/checkout");
        
        // Step 4: Verify bị redirect về cart
        wait.until(ExpectedConditions.urlContains("/cart"));
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/cart"),
                "Should redirect to cart page");
        
        System.out.println("✅ Test chặn checkout giỏ trống: PASSED");
    }
}
```

---

## 🚀 CÁCH CHẠY TEST

### 1. Chạy từ IDE
```
Right-click → Run 'AddToCartTest'
Right-click → Run 'CheckoutFlowTest'
```

### 2. Chạy từ Maven
```bash
mvn test -Dtest=AddToCartTest
mvn test -Dtest=CheckoutFlowTest
```

---

## 📊 KẾT QUẢ MONG ĐỢI

```
✅ AddToCartTest
   ├─ TS_CART_001: Thêm sản phẩm vào giỏ ✓
   ├─ TS_CART_002: Cập nhật số lượng ✓
   └─ TS_CART_003: Xóa sản phẩm ✓

✅ CheckoutFlowTest
   ├─ TS_CART_004: Checkout thành công (E2E) ✓
   ├─ TS_CART_005: Chặn checkout chưa login ✓
   └─ TS_CART_006: Chặn checkout giỏ trống ✓

Total: 6 tests, 6 passed ✅
```

---

**Người tạo:** Tuấn Khoa  
**Reviewer:** Kiro AI  
**Ngày:** 2026-02-25
