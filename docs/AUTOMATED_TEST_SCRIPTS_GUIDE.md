# 🤖 HƯỚNG DẪN VIẾT TEST SCRIPT TỰ ĐỘNG (Y4)

**Mục đích:** Viết test scripts tự động cho web application  
**Công nghệ:** Selenium WebDriver + JUnit 5  
**Yêu cầu:** Mỗi người tối thiểu 2 test scripts  
**Tổng:** 8 test scripts (4 người × 2)

---

## 📋 PHÂN CÔNG TEST SCRIPTS

| Thành viên | Test Scripts | Module |
|------------|--------------|--------|
| **Thiên Lộc** | 2 scripts | Product Management |
| **Tuấn Khoa** | 2 scripts | Cart & Checkout |
| **Chí Bảo** | 2 scripts | Category Management |
| **Khánh Ý** | 2 scripts | Authentication |

---

## 🛠️ SETUP SELENIUM

### 1. Thêm dependencies vào `pom.xml`

```xml
<!-- Selenium WebDriver -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.15.0</version>
    <scope>test</scope>
</dependency>

<!-- WebDriverManager (tự động download driver) -->
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>
```

### 2. Cấu trúc thư mục test

```
src/test/java/com/springboot/jenka_coffee/
├── selenium/
│   ├── BaseSeleniumTest.java          (Base class chung)
│   ├── product/
│   │   ├── ProductListTest.java       (Thiên Lộc)
│   │   └── ProductSearchTest.java     (Thiên Lộc)
│   ├── cart/
│   │   ├── AddToCartTest.java         (Tuấn Khoa)
│   │   └── CheckoutFlowTest.java      (Tuấn Khoa)
│   ├── category/
│   │   ├── CategoryCRUDTest.java      (Chí Bảo)
│   │   └── CategoryFilterTest.java    (Chí Bảo)
│   └── auth/
│       ├── LoginTest.java             (Khánh Ý)
│       └── SignupTest.java            (Khánh Ý)
```

---

## 📝 CHI TIẾT TEST SCRIPTS

Chi tiết code và hướng dẫn xem trong các files:
- `AutoTest_ThienLoc_Product.md`
- `AutoTest_TuanKhoa_Cart.md`
- `AutoTest_ChiBao_Category.md`
- `AutoTest_KhanhY_Auth.md`

---

**Người tạo:** Kiro AI  
**Ngày:** 2026-02-25
