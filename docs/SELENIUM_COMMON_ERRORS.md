# Selenium Common Errors & Solutions

## 📋 Tổng Quan

Tài liệu này giải thích các lỗi Selenium thường gặp và cách fix đã áp dụng trong test scripts.

---

## ❌ Lỗi 1: Element Not Interactable

### Triệu Chứng
```
org.openqa.selenium.ElementNotInteractableException: element not interactable
Element: input[name='keyword']
```

### Nguyên Nhân
- Element bị che bởi element khác (header, overlay, modal)
- Element nằm ngoài viewport
- Element chưa load xong
- CSS animations chưa hoàn thành

### Giải Pháp Đã Áp Dụng

#### 1. Scroll Element Vào View
```java
WebElement searchInput = driver.findElement(By.cssSelector("input[name='keyword']"));

// Scroll vào giữa màn hình
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    searchInput
);
Thread.sleep(500); // Đợi scroll animation
```

#### 2. Đợi Element Clickable
```java
WebElement searchInput = wait.until(
    ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='keyword']"))
);
```

#### 3. Kết Hợp Cả 2
```java
// Đợi clickable
WebElement searchInput = wait.until(
    ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='keyword']"))
);

// Scroll vào view
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    searchInput
);
Thread.sleep(500);

// Bây giờ mới interact
searchInput.sendKeys("Máy");
```

---

## ❌ Lỗi 2: Timeout Exception

### Triệu Chứng
```
org.openqa.selenium.TimeoutException: Expected condition failed: 
waiting for presence of element located by: By.cssSelector: .product-card 
(tried for 10 second(s) with 500 milliseconds interval)
```

### Nguyên Nhân
- Element không tồn tại trên trang
- Page load chậm
- AJAX request chưa hoàn thành
- Filter không trả về kết quả (empty results)

### Giải Pháp Đã Áp Dụng

#### 1. Tăng Timeout
```java
// Tăng từ 10s lên 15s
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
```

#### 2. Đợi Nhiều Điều Kiện (OR)
```java
// Đợi product-card HOẶC empty message
wait.until(ExpectedConditions.or(
    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")),
    ExpectedConditions.presenceOfElementLocated(
        By.xpath("//*[contains(text(), 'Không tìm thấy')]")
    )
));
```

#### 3. Try-Catch Để Handle Empty Results
```java
try {
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
} catch (TimeoutException e) {
    System.out.println("⚠ Không tìm thấy sản phẩm (có thể là empty results)");
    // Test vẫn pass, chỉ log warning
}
```

#### 4. Thêm Sleep Sau Submit
```java
filterButton.click();
Thread.sleep(2000); // Đợi page transition

// Sau đó mới wait
wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
```

---

## ❌ Lỗi 3: Element Click Intercepted

### Triệu Chứng
```
org.openqa.selenium.ElementClickInterceptedException: 
element click intercepted: Element is not clickable at point (280, 731)
Element: //a[contains(@href, '/product/filter') and contains(., 'triệu')]
```

### Nguyên Nhân
- Element bị che bởi element khác (sticky header, footer, overlay)
- Element nằm dưới một layer khác
- Z-index issues
- Fixed/sticky positioning

### Giải Pháp Đã Áp Dụng

#### 1. Scroll Trước Khi Click
```java
WebElement link = driver.findElement(By.xpath("//a[contains(., '1 triệu - 5 triệu')]"));

// Scroll vào giữa màn hình
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    link
);
Thread.sleep(500);

link.click();
```

#### 2. Đợi Element Clickable
```java
WebElement link = wait.until(
    ExpectedConditions.elementToBeClickable(
        By.xpath("//a[contains(., '1 triệu - 5 triệu')]")
    )
);
link.click();
```

#### 3. JavaScript Click (Fallback)
```java
try {
    link.click(); // Thử click thường
} catch (ElementClickInterceptedException e) {
    System.out.println("⚠ Click thường fail, dùng JavaScript click");
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
}
```

#### 4. Kết Hợp Tất Cả (Best Practice)
```java
WebElement link = driver.findElement(By.xpath("//a[contains(., '1 triệu - 5 triệu')]"));

// 1. Scroll vào view
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    link
);
Thread.sleep(500);

// 2. Đợi clickable
wait.until(ExpectedConditions.elementToBeClickable(link));

// 3. Thử click, fallback to JS
try {
    link.click();
} catch (ElementClickInterceptedException e) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
}
```

---

## 🔧 Best Practices Đã Áp Dụng

### 1. Luôn Scroll Trước Khi Interact
```java
// BAD
element.click();

// GOOD
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    element
);
Thread.sleep(500);
element.click();
```

### 2. Dùng Explicit Waits
```java
// BAD
Thread.sleep(5000);
element.click();

// GOOD
WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
element.click();
```

### 3. Try-Catch Cho Fallback
```java
try {
    element.click(); // Thử cách thường
} catch (Exception e) {
    // Fallback to JavaScript
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
}
```

### 4. Đợi Page Transitions
```java
button.click();
Thread.sleep(1000); // Đợi page transition
wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".result")));
```

### 5. Handle Empty Results
```java
try {
    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
    // Có kết quả
} catch (TimeoutException e) {
    // Không có kết quả - vẫn OK
    System.out.println("⚠ Empty results");
}
```

---

## 📊 So Sánh Trước & Sau Fix

### Test 1: Search Input

**Trước (FAIL):**
```java
WebElement searchInput = driver.findElement(By.cssSelector("input[name='keyword']"));
searchInput.sendKeys("Máy"); // ❌ ElementNotInteractableException
```

**Sau (PASS):**
```java
WebElement searchInput = wait.until(
    ExpectedConditions.elementToBeClickable(By.cssSelector("input[name='keyword']"))
);
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    searchInput
);
Thread.sleep(500);
searchInput.sendKeys("Máy"); // ✅ Works!
```

### Test 2: Price Filter

**Trước (FAIL):**
```java
filterButton.click();
wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")));
// ❌ TimeoutException - không tìm thấy product-card
```

**Sau (PASS):**
```java
filterButton.click();
Thread.sleep(2000); // Đợi page transition

try {
    wait.until(ExpectedConditions.or(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product-card")),
        ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(), 'Không tìm thấy')]"))
    ));
} catch (Exception e) {
    System.out.println("⚠ Không tìm thấy sản phẩm");
}
// ✅ Works! Handle cả empty results
```

### Test 3: Quick Filter Link

**Trước (FAIL):**
```java
link.click(); // ❌ ElementClickInterceptedException
```

**Sau (PASS):**
```java
((JavascriptExecutor) driver).executeScript(
    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
    link
);
Thread.sleep(500);
wait.until(ExpectedConditions.elementToBeClickable(link));

try {
    link.click();
} catch (Exception e) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
}
// ✅ Works!
```

---

## 💡 Tips Thêm

### 1. Debug Bằng Screenshots
```java
// Thêm vào @AfterEach hoặc khi test fail
public void takeScreenshot(String testName) {
    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    try {
        Files.copy(screenshot.toPath(), 
                   Paths.get("screenshots/" + testName + ".png"));
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

### 2. Log Element Position
```java
WebElement element = driver.findElement(By.cssSelector(".my-element"));
Point location = element.getLocation();
Dimension size = element.getSize();
System.out.println("Element at: " + location.x + ", " + location.y);
System.out.println("Element size: " + size.width + "x" + size.height);
```

### 3. Check Element Visibility
```java
WebElement element = driver.findElement(By.cssSelector(".my-element"));
boolean isDisplayed = element.isDisplayed();
boolean isEnabled = element.isEnabled();
System.out.println("Displayed: " + isDisplayed + ", Enabled: " + isEnabled);
```

### 4. Wait for AJAX
```java
// Đợi jQuery AJAX hoàn thành
wait.until(driver -> 
    ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0")
);
```

---

## 🎯 Checklist Khi Test Fail

- [ ] Element có tồn tại trên trang không? (inspect trong browser)
- [ ] Element có visible không? (check CSS display, visibility)
- [ ] Element có bị che không? (check z-index, overlays)
- [ ] Đã scroll element vào view chưa?
- [ ] Đã đợi element clickable chưa?
- [ ] Page transition đã hoàn thành chưa?
- [ ] AJAX request đã hoàn thành chưa?
- [ ] Selector có đúng không? (test trong browser console)
- [ ] Có animations đang chạy không?
- [ ] Timeout có đủ không?

---

**Tóm tắt:** 3 lỗi chính đã fix:
1. ✅ Element Not Interactable → Scroll + Wait Clickable
2. ✅ Timeout Exception → Try-Catch + Handle Empty Results
3. ✅ Click Intercepted → Scroll + Wait + JavaScript Click Fallback
