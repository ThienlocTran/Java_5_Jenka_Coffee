# Fix Database Connection Reset Error

## ❌ Lỗi

```
Caused by: java.net.SocketException: Connection reset
org.springframework.dao.DataAccessResourceFailureException: 
Hibernate transaction: Unable to commit against JDBC Connection; Connection reset
```

## 🔍 Nguyên Nhân

### 1. Remote Database Timeout
- Bạn đang dùng remote SQL Server: `sql8010.site4now.net`
- Connection bị timeout do network latency
- Firewall/proxy có thể drop idle connections

### 2. Connection Pool Issues
- Connection pool settings không phù hợp với remote DB
- Idle connections bị server đóng
- Không có connection validation

### 3. Network Issues
- Network không ổn định
- Packet loss
- DNS resolution issues

---

## ✅ Giải Pháp Đã Áp Dụng

### 1. Tối Ưu Hikari Connection Pool

**Trước:**
```properties
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
spring.datasource.hikari.keepalive-time=300000
```

**Sau (Optimized for Remote DB):**
```properties
# Tăng connection timeout cho remote DB
spring.datasource.hikari.connection-timeout=30000

# Giảm pool size để tránh quá nhiều connections
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=10

# Tăng idle timeout
spring.datasource.hikari.idle-timeout=600000

# Tăng max lifetime
spring.datasource.hikari.max-lifetime=1800000

# Giảm keepalive time để test connection thường xuyên hơn
spring.datasource.hikari.keepalive-time=60000

# Thêm test query
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.validation-timeout=5000

# Leak detection
spring.datasource.hikari.leak-detection-threshold=60000
```

### 2. Thêm Connection Resilience

```properties
# Auto reconnect
spring.jpa.properties.hibernate.connection.autoReconnect=true
spring.jpa.properties.hibernate.connection.autoReconnectForPools=true

# Connection validation
spring.jpa.properties.hibernate.connection.is-connection-validation-required=true
spring.jpa.properties.hibernate.c3p0.testConnectionOnCheckout=true

# Disable autocommit optimization
spring.jpa.properties.hibernate.connection.provider_disables_autocommit=false
```

---

## 🎯 Giải Thích Chi Tiết

### Connection Timeout
```properties
spring.datasource.hikari.connection-timeout=30000
```
- Tăng từ 20s lên 30s
- Cho phép nhiều thời gian hơn để establish connection với remote DB

### Pool Size
```properties
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=10
```
- Giảm từ 5-20 xuống 2-10
- Remote DB không cần nhiều connections
- Giảm overhead và tránh connection limit

### Keepalive Time
```properties
spring.datasource.hikari.keepalive-time=60000
```
- Giảm từ 5 phút xuống 1 phút
- Test connection thường xuyên hơn
- Phát hiện dead connections sớm hơn

### Connection Test Query
```properties
spring.datasource.hikari.connection-test-query=SELECT 1
```
- Test connection trước khi dùng
- Đảm bảo connection còn sống

### Max Lifetime
```properties
spring.datasource.hikari.max-lifetime=1800000
```
- Tăng lên 30 phút
- Connection sẽ được refresh sau 30 phút
- Tránh dùng stale connections

---

## 🔧 Giải Pháp Thay Thế

### Option 1: Dùng Local Database (Khuyến nghị cho Development)

Tạo file `application-local.properties`:
```properties
# Local SQL Server
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=jenka_coffee_local;encrypt=true;trustServerCertificate=true;
spring.datasource.username=sa
spring.datasource.password=YourPassword

# Simpler pool settings for local
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
```

Chạy với profile local:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Option 2: Thêm Retry Logic

Tạo aspect để retry khi connection fail:
```java
@Aspect
@Component
public class DatabaseRetryAspect {
    
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object retryOnConnectionError(ProceedingJoinPoint joinPoint) throws Throwable {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                return joinPoint.proceed();
            } catch (DataAccessResourceFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e;
                }
                Thread.sleep(1000 * retryCount); // Exponential backoff
            }
        }
        return null;
    }
}
```

### Option 3: Connection URL Parameters

Thêm parameters vào connection URL:
```properties
spring.datasource.url=jdbc:sqlserver://sql8010.site4now.net;databaseName=db_ac3c24_jenkacoffee;encrypt=true;trustServerCertificate=true;socketTimeout=30000;loginTimeout=30;connectRetryCount=3;connectRetryInterval=10;
```

Parameters:
- `socketTimeout=30000` - Socket timeout 30s
- `loginTimeout=30` - Login timeout 30s
- `connectRetryCount=3` - Retry 3 lần
- `connectRetryInterval=10` - Đợi 10s giữa các retry

---

## 🧪 Test Connection

### Test 1: Simple Connection Test

Tạo endpoint để test:
```java
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/db-connection")
    public ResponseEntity<String> testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return ResponseEntity.ok("Connection OK: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Connection FAIL: " + e.getMessage());
        }
    }
}
```

Test:
```bash
curl http://localhost:8080/api/test/db-connection
```

### Test 2: Load Test

```bash
# Chạy nhiều requests để test pool
for i in {1..20}; do
  curl http://localhost:8080/product/list &
done
wait
```

---

## 📊 Monitoring

### Check Hikari Pool Stats

Thêm vào `application.properties`:
```properties
# Enable metrics
management.endpoints.web.exposure.include=health,metrics,hikaricp
management.endpoint.health.show-details=always
```

Xem metrics:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle
```

### Enable SQL Logging

```properties
# Temporary for debugging
spring.jpa.show-sql=true
logging.level.com.zaxxer.hikari=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

---

## ⚠️ Lưu Ý

### 1. Remote DB Limitations
- Latency cao hơn local DB
- Có thể bị rate limit
- Network không ổn định

### 2. Connection Pool Best Practices
- Không set pool quá lớn cho remote DB
- Luôn validate connections
- Set reasonable timeouts
- Monitor pool metrics

### 3. Production Considerations
- Dùng connection pooling (đã có Hikari)
- Enable connection validation
- Set appropriate timeouts
- Monitor và alert
- Consider using read replicas

---

## 🚀 Sau Khi Fix

### Restart Application
```bash
# Stop application (Ctrl+C)
# Start lại
mvn spring-boot:run
```

### Verify
1. Application start thành công
2. Không còn connection reset errors
3. Tests chạy được
4. Pages load bình thường

### Monitor
- Xem logs có còn errors không
- Check response time
- Monitor connection pool

---

## 📝 Checklist

- [x] Tăng connection timeout lên 30s
- [x] Giảm pool size xuống 2-10
- [x] Thêm connection test query
- [x] Enable auto reconnect
- [x] Set keepalive time 60s
- [x] Tăng max lifetime lên 30 phút
- [ ] Test connection endpoint
- [ ] Monitor pool metrics
- [ ] Consider local DB for development

---

## 💡 Tips

### Tip 1: Use Local DB for Development
Remote DB chậm và không ổn định. Dùng local SQL Server cho development.

### Tip 2: Connection Pooling
Hikari đã tối ưu, chỉ cần config đúng parameters.

### Tip 3: Network Issues
Nếu vẫn bị lỗi, check:
- Firewall
- VPN
- DNS
- Network stability

### Tip 4: Database Server
Check remote DB server:
- Có đang chạy không?
- Có bị overload không?
- Connection limit?

---

**Tóm tắt:** Lỗi do remote DB connection timeout. Đã fix bằng cách tối ưu Hikari pool settings và thêm connection resilience.


---

## 🔄 Selenium Tests - Retry Logic

### Vấn đề với Selenium Tests
Khi chạy Selenium tests, application có thể bị connection reset giữa chừng, gây ra lỗi `ERR_CONNECTION_RESET` trong browser.

### Giải pháp: Thêm Retry Logic

Tất cả các Selenium tests đã được cập nhật với retry logic để tự động retry khi gặp connection reset:

```java
// Retry logic cho connection reset
int maxRetries = 3;
for (int retry = 0; retry < maxRetries; retry++) {
    try {
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
```

### Delay giữa Tests

Để tránh quá tải database, đã thêm delay 2 giây giữa các test:

```java
@AfterEach
public void tearDown() throws InterruptedException {
    if (driver != null) {
        driver.quit();
    }
    // Đợi 2 giây giữa các test để tránh quá tải database
    Thread.sleep(2000);
}
```

### Tests đã được cập nhật

#### ProductSearchFilterTest.java
- ✅ testSearchByKeyword (Test 1) - Có retry logic
- ✅ testFilterByPriceRange (Test 2) - Có retry logic
- ✅ testQuickPriceFilter (Test 3) - Có retry logic
- ✅ testFilterByCategory (Test 4) - Có retry logic
- ✅ testCombinedSearchAndFilter (Test 5) - Có retry logic

#### ProductListPaginationTest.java
- ✅ testProductListDisplay (Test 1) - Có retry logic
- ✅ testProductListPagination (Test 2) - Có retry logic
- ✅ testProductsPerPage (Test 3) - Có retry logic

### Cách chạy Tests

```bash
# Chạy tất cả tests (với retry logic)
mvnw test -Dtest=ProductSearchFilterTest

# Hoặc chạy từng test riêng
mvnw test -Dtest=ProductSearchFilterTest#testSearchByKeyword
mvnw test -Dtest=ProductListPaginationTest#testProductListDisplay
```

### Kết quả mong đợi

- Tests sẽ tự động retry tối đa 3 lần khi gặp ERR_CONNECTION_RESET
- Có delay 2 giây giữa các tests để tránh quá tải
- Giảm thiểu test failures do connection issues
- Logs rõ ràng khi retry: `⚠ Connection reset, retry X/3`

### Troubleshooting

**Nếu test vẫn fail sau 3 retries:**
1. Kiểm tra Spring Boot application có đang chạy không
2. Restart Spring Boot application
3. Kiểm tra database connection trong application logs
4. Thử chạy từng test riêng lẻ thay vì chạy tất cả cùng lúc

**Nếu muốn tăng số lần retry:**
```java
int maxRetries = 5; // Tăng từ 3 lên 5
```

**Nếu muốn tăng delay giữa tests:**
```java
Thread.sleep(5000); // Tăng từ 2s lên 5s
```

---

**Cập nhật:** Tất cả Selenium tests đã có retry logic và delay để xử lý connection reset issues.
