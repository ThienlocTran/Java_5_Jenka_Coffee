# Jenka Coffee - Backend API

> RESTful API cho hệ thống E-commerce máy pha cà phê, máy xay cà phê và dụng cụ pha chế chính hãng.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=java)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Private-red)](LICENSE)

## 📋 Mục Lục

- [Tổng Quan](#-tổng-quan)
- [Công Nghệ](#-công-nghệ)
- [Kiến Trúc](#-kiến-trúc)
- [Tính Năng](#-tính-năng)
- [Cài Đặt](#-cài-đặt)
- [Cấu Hình](#-cấu-hình)
- [Database](#-database)
- [API Documentation](#-api-documentation)
- [Security](#-security)
- [Deployment](#-deployment)
- [Troubleshooting](#-troubleshooting)

## 🎯 Tổng Quan

Jenka Coffee Backend là RESTful API được xây dựng với Spring Boot 3, cung cấp các endpoint cho:
- Quản lý sản phẩm, đơn hàng, người dùng
- Xác thực & phân quyền (JWT + Google OAuth 2.0)
- Thanh toán & giỏ hàng
- Upload ảnh (Cloudinary)
- Email notifications
- Admin dashboard & reports

### Đặc Điểm Nổi Bật

- 🚀 **High Performance**: Connection pooling, caching, async processing
- 🔐 **Security First**: JWT, rate limiting, XSS protection, CSRF tokens
- 📊 **Scalable**: Stateless architecture, horizontal scaling ready
- 🗄️ **Database**: PostgreSQL với Flyway migration
- ☁️ **Cloud-Ready**: Deployed on Railway.app
- 📧 **Email**: SMTP integration cho OTP & notifications
- 🖼️ **Image Storage**: Cloudinary CDN
- 🔄 **CI/CD**: Auto-deploy với Railway

## 🛠 Công Nghệ

### Core Framework
- **Spring Boot 3.2.4** - Application framework
- **Spring Security 6** - Authentication & authorization
- **Spring Data JPA** - Data access layer
- **Hibernate** - ORM framework
- **Java 17** - Programming language

### Database
- **PostgreSQL 15** - Primary database (Neon.tech)
- **Flyway** - Database migration tool
- **HikariCP** - Connection pooling

### Security & Authentication
- **JWT (jjwt 0.12.6)** - Token-based authentication
- **Google OAuth 2.0** - Social login
- **Bucket4j 8.10** - Rate limiting
- **OWASP HTML Sanitizer** - XSS protection
- **BCrypt** - Password hashing

### External Services
- **Cloudinary** - Image storage & CDN
- **Gmail SMTP** - Email service
- **Vercel Webhook** - Frontend auto-rebuild

### Caching & Performance
- **Caffeine Cache** - In-memory caching
- **Spring Cache** - Cache abstraction

### Development Tools
- **Lombok** - Reduce boilerplate code
- **Spring Boot DevTools** - Hot reload
- **Maven** - Build tool

## 🏗 Kiến Trúc

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Controllers, DTOs, Exception Handler) │
├─────────────────────────────────────────┤
│          Service Layer                  │
│    (Business Logic, Validation)         │
├─────────────────────────────────────────┤
│       Repository Layer                  │
│    (Data Access, JPA Repositories)      │
├─────────────────────────────────────────┤
│         Database Layer                  │
│      (PostgreSQL, Flyway)               │
└─────────────────────────────────────────┘
```

### Package Structure

```
com.springboot.jenka_coffee/
├── api/                          # REST Controllers
│   ├── ApiAuthController         # Authentication
│   ├── ApiProductController      # Products (public)
│   ├── ApiOrderController        # Orders
│   ├── ApiCartController         # Shopping cart
│   ├── ApiContactController      # Contact form
│   ├── ApiFeedbackController     # Store feedback
│   └── admin/                    # Admin endpoints
│       ├── ApiAdminProductController
│       ├── ApiAdminOrderController
│       ├── ApiAdminAccountController
│       ├── ApiAdminDashboardController
│       └── ...
├── config/                       # Configuration classes
│   ├── SecurityConfig            # Spring Security
│   ├── WebConfig                 # CORS, interceptors
│   ├── CloudinaryConfig          # Cloudinary setup
│   ├── AsyncConfig               # Async processing
│   ├── I18nConfig                # Internationalization
│   └── RateLimitFilter           # Rate limiting
├── dto/                          # Data Transfer Objects
│   ├── request/                  # Request DTOs
│   └── response/                 # Response DTOs
├── entity/                       # JPA Entities
│   ├── Account                   # User accounts
│   ├── Product                   # Products
│   ├── Order                     # Orders
│   ├── OrderDetail               # Order items
│   ├── Category                  # Product categories
│   ├── News                      # News articles
│   ├── Contact                   # Contact messages
│   ├── StoreFeedback             # Store ratings
│   └── ...
├── repository/                   # JPA Repositories
│   ├── AccountRepository
│   ├── ProductRepository
│   ├── OrderRepository
│   └── ...
├── service/                      # Service interfaces
│   └── impl/                     # Service implementations
│       ├── AccountServiceImpl
│       ├── ProductServiceImpl
│       ├── OrderServiceImpl
│       ├── EmailServiceImpl
│       ├── UploadServiceImpl
│       ├── GoogleOAuthServiceImpl
│       ├── VercelWebhookServiceImpl
│       └── ...
├── security/                     # Security components
│   ├── JwtService                # JWT generation/validation
│   ├── JwtAuthFilter             # JWT authentication filter
│   └── CustomUserDetailsService  # User details loader
├── exception/                    # Exception handling
│   ├── GlobalExceptionHandler    # Global error handler
│   ├── ResourceNotFoundException
│   └── ...
├── util/                         # Utility classes
│   ├── ImageUtils                # Image processing
│   ├── SlugUtils                 # URL slug generation
│   └── ...
└── JenkaCoffeeApplication        # Main application class
```

## ✨ Tính Năng

### Authentication & Authorization

#### JWT Authentication
- Stateless token-based authentication
- Access token (15 minutes) + Refresh token (7 days)
- Token blacklist cho logout
- Automatic token refresh

#### Google OAuth 2.0
- Social login với Google
- ID Token verification
- Auto account creation

#### OTP Email Verification
- 6-digit OTP code
- 5 minutes expiration
- Rate limiting (3 requests/minute)

#### Role-Based Access Control (RBAC)
- **ADMIN**: Full system access
- **STAFF**: Limited admin access
- **CUSTOMER**: Customer features only

### Product Management

#### CRUD Operations
- Create, Read, Update, Delete products
- Bulk operations support
- Soft delete (available flag)

#### Image Management
- Upload to Cloudinary CDN
- Automatic compression & optimization
- Multiple images per product
- Image gallery with display order

#### Category Management
- Hierarchical categories
- Product count per category
- Category-based filtering

#### Inventory Management
- Stock tracking
- Low stock alerts
- Inventory reports

#### Pricing
- Regular price & sale price
- Discount percentage calculation
- Price history (future feature)

### Order Management

#### Order Processing
- Multi-step checkout flow
- Order status tracking (PENDING → CONFIRMED → SHIPPING → DELIVERED)
- Order cancellation
- Order history

#### Payment Integration
- COD (Cash on Delivery)
- Bank transfer
- Payment status tracking

#### Shipping
- Vietnam provinces support
- Shipping address validation
- Delivery tracking

### Shopping Cart

#### Session-Based Cart
- Add/Remove/Update items
- Quantity validation
- Price calculation
- Cart persistence

#### Cart Operations
- Merge cart on login
- Clear cart after checkout
- Cart expiration (30 days)

### Email Notifications

#### Transactional Emails
- OTP verification
- Order confirmation
- Order status updates
- Password reset
- Welcome email

#### Email Templates
- HTML templates
- Responsive design
- Brand styling

### Admin Dashboard

#### Statistics
- Revenue by day/month/year
- Order count & status
- Product sales ranking
- Customer growth

#### Charts & Reports
- Line chart (revenue trend)
- Bar chart (product sales)
- Pie chart (order status)
- Export to Excel/PDF

### Content Management

#### News & Blog
- CRUD news articles
- Rich text content
- Featured image
- Publish/Unpublish

#### Contact Management
- Contact form submissions
- Email notifications
- Admin response tracking

#### Store Feedback
- 2-step feedback popup
- Store rating (1-5 stars)
- Staff rating (1-5 stars)
- Branch selection (HN/HCM)

### SEO & Performance

#### Sitemap Generation
- Dynamic XML sitemap
- Auto-update on content change
- Submit to search engines

#### Vercel Webhook Integration
- Auto-rebuild frontend on data change
- Self-healing pipeline
- Exponential backoff retry
- Network-aware triggering

## 🚀 Cài Đặt

### Yêu Cầu Hệ Thống

- **Java**: 17 hoặc cao hơn
- **Maven**: 3.8+ hoặc cao hơn
- **PostgreSQL**: 15+ hoặc cao hơn
- **Git**: 2.x hoặc cao hơn

### Clone Repository

```bash
git clone https://github.com/your-repo/jenka-coffee.git
cd jenka-coffee/Java_5_Jenka_Coffee
```

### Cài Đặt Dependencies

```bash
# Maven sẽ tự động download dependencies
mvn clean install

# Hoặc skip tests
mvn clean install -DskipTests
```

## ⚙️ Cấu Hình

### Application Properties

Tạo file `src/main/resources/application.properties`:

```properties
# ============================================================================
# SERVER CONFIGURATION
# ============================================================================
server.port=8080
spring.application.name=jenka_coffee

# ============================================================================
# DATABASE CONFIGURATION (Neon.tech PostgreSQL)
# ============================================================================
spring.datasource.url=jdbc:postgresql://your-neon-host/jenka_coffee?sslmode=require
spring.datasource.username=your-username
spring.datasource.password=your-password
spring.datasource.driver-class-name=org.postgresql.Driver

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=90000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.initialization-fail-timeout=-1

# ============================================================================
# JPA & HIBERNATE CONFIGURATION
# ============================================================================
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

# ============================================================================
# FLYWAY MIGRATION
# ============================================================================
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# ============================================================================
# JWT CONFIGURATION
# ============================================================================
jwt.secret=your-super-secret-key-min-256-bits
jwt.expiration=900000
jwt.refresh-expiration=604800000

# ============================================================================
# GOOGLE OAUTH 2.0
# ============================================================================
google.client-id=your-google-client-id.apps.googleusercontent.com

# ============================================================================
# CLOUDINARY CONFIGURATION
# ============================================================================
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# ============================================================================
# EMAIL CONFIGURATION (Gmail SMTP)
# ============================================================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# ============================================================================
# VERCEL WEBHOOK (Frontend Auto-Rebuild)
# ============================================================================
vercel.deploy-hook-url=https://api.vercel.com/v1/integrations/deploy/...

# ============================================================================
# CORS CONFIGURATION
# ============================================================================
cors.allowed-origins=http://localhost:5173,https://jenkacoffee.com

# ============================================================================
# FILE UPLOAD
# ============================================================================
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# ============================================================================
# LOGGING
# ============================================================================
logging.level.root=INFO
logging.level.com.springboot.jenka_coffee=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Environment Variables (Production)

Trên Railway.app, cấu hình các biến môi trường:

```env
DATABASE_URL=postgresql://...
JWT_SECRET=your-production-secret
GOOGLE_CLIENT_ID=your-client-id
CLOUDINARY_URL=cloudinary://...
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
VERCEL_DEPLOY_HOOK=https://api.vercel.com/...
```

## 🗄️ Database

### PostgreSQL Setup (Local)

```bash
# Tạo database
createdb jenka_coffee

# Hoặc dùng psql
psql -U postgres
CREATE DATABASE jenka_coffee;
```

### Flyway Migration

Migrations được tự động chạy khi start application.

```bash
# Chạy migrations
mvn flyway:migrate

# Rollback (nếu cần)
mvn flyway:undo

# Xem migration history
mvn flyway:info
```

### Database Schema

```sql
-- Core tables
accounts          -- User accounts
products          -- Products
categories        -- Product categories
orders            -- Orders
order_details     -- Order items
carts             -- Shopping carts
cart_items        -- Cart items

-- Content tables
news              -- News articles
banners           -- Homepage banners
contacts          -- Contact messages
store_feedbacks   -- Store ratings

-- Auth tables
otps              -- OTP codes
jwt_blacklist     -- Blacklisted tokens
```

### Sample Data

```bash
# Import sample data (optional)
psql -U postgres -d jenka_coffee < sample_data.sql
```

## 📖 API Documentation

### Base URL

- **Local**: `http://localhost:8080`
- **Production**: `https://your-app.railway.app`

### Authentication

Tất cả admin endpoints yêu cầu JWT token:

```bash
# Login
POST /api/auth/login
{
  "email": "admin@example.com",
  "password": "password"
}

# Response
{
  "status": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "account": { ... }
  }
}

# Use token in subsequent requests
Authorization: Bearer eyJhbGc...
```

### Public Endpoints

```bash
# Products
GET    /api/products              # List products
GET    /api/products/{id}         # Product detail
GET    /api/products/slug/{slug}  # Product by slug
GET    /api/categories            # List categories

# News
GET    /api/news                  # List news
GET    /api/news/{id}             # News detail

# Contact
POST   /api/contacts              # Submit contact form

# Feedback
POST   /api/feedbacks             # Submit store feedback
```

### Customer Endpoints (Requires Auth)

```bash
# Cart
GET    /api/cart                  # Get cart
POST   /api/cart/add              # Add to cart
PUT    /api/cart/update/{id}      # Update quantity
DELETE /api/cart/remove/{id}      # Remove item

# Orders
POST   /api/orders/checkout       # Create order
GET    /api/orders                # Order history
GET    /api/orders/{id}           # Order detail

# Account
GET    /api/account/profile       # Get profile
PUT    /api/account/profile       # Update profile
POST   /api/account/change-password
POST   /api/account/upload-avatar
```

### Admin Endpoints (Requires ADMIN Role)

```bash
# Products
GET    /api/admin/products        # List all products
POST   /api/admin/products        # Create product
PUT    /api/admin/products/{id}   # Update product
DELETE /api/admin/products/{id}   # Delete product

# Orders
GET    /api/admin/orders          # List all orders
PUT    /api/admin/orders/{id}/status  # Update status

# Accounts
GET    /api/admin/accounts        # List accounts
POST   /api/admin/accounts        # Create account
PUT    /api/admin/accounts/{id}   # Update account
DELETE /api/admin/accounts/{id}   # Delete account

# Dashboard
GET    /api/admin/dashboard/stats # Dashboard statistics
GET    /api/admin/reports/revenue # Revenue report
```

### Response Format

```json
{
  "status": "SUCCESS",
  "message": "Operation successful",
  "data": { ... }
}
```

### Error Response

```json
{
  "status": "ERROR",
  "message": "Error description",
  "errors": {
    "field": "Error message"
  }
}
```

## 🔐 Security

### Security Features

#### Authentication
- JWT with RS256 algorithm
- Token expiration & refresh
- Token blacklist on logout
- Secure password hashing (BCrypt)

#### Authorization
- Role-based access control (RBAC)
- Method-level security (@PreAuthorize)
- Resource ownership validation

#### Rate Limiting
- Sliding window algorithm (Bucket4j)
- Per-IP rate limiting
- Auth endpoints: 5 requests/minute
- API endpoints: 100 requests/minute

#### XSS Protection
- OWASP HTML Sanitizer
- Input validation
- Output encoding

#### CSRF Protection
- CSRF tokens for state-changing operations
- SameSite cookie attribute

#### SQL Injection Prevention
- Parameterized queries (JPA)
- Input validation
- Prepared statements

#### CORS Configuration
- Whitelist allowed origins
- Credentials support
- Preflight caching

### Security Best Practices

```java
// ✅ DO: Use parameterized queries
@Query("SELECT p FROM Product p WHERE p.name = :name")
Product findByName(@Param("name") String name);

// ❌ DON'T: String concatenation
String query = "SELECT * FROM products WHERE name = '" + name + "'";

// ✅ DO: Validate input
@NotBlank(message = "Name is required")
@Size(min = 3, max = 100)
private String name;

// ✅ DO: Sanitize HTML
String clean = Sanitizers.FORMATTING.sanitize(userInput);

// ✅ DO: Hash passwords
String hashed = passwordEncoder.encode(password);
```

## 🚢 Deployment

### Railway.app (Recommended)

#### Setup

1. Tạo tài khoản Railway.app
2. Kết nối GitHub repository
3. Tạo PostgreSQL database
4. Deploy application

#### Configuration

```toml
# railway.toml
[build]
builder = "NIXPACKS"

[deploy]
startCommand = "java -jar target/jenka_coffee-0.0.1-SNAPSHOT.jar"
healthcheckPath = "/actuator/health"
healthcheckTimeout = 100
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 10
```

#### Environment Variables

Cấu hình trên Railway dashboard:
- `DATABASE_URL`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `CLOUDINARY_URL`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

### Manual Deployment

```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/jenka_coffee-0.0.1-SNAPSHOT.jar

# Or with profile
java -jar -Dspring.profiles.active=prod target/jenka_coffee-0.0.1-SNAPSHOT.jar
```

### Docker (Optional)

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t jenka-coffee-backend .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=... \
  -e JWT_SECRET=... \
  jenka-coffee-backend
```

## 🐛 Troubleshooting

### Database Connection Issues

```bash
# Check database is running
psql -U postgres -d jenka_coffee

# Test connection
telnet your-neon-host 5432

# Check Flyway migrations
mvn flyway:info
```

### Port Already in Use

```bash
# Find process on port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Build Errors

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests
mvn clean install -DskipTests

# Clear Maven cache
rm -rf ~/.m2/repository
```

### Memory Issues

```bash
# Increase heap size
export MAVEN_OPTS="-Xmx2048m"

# Or in pom.xml
<argLine>-Xmx2048m</argLine>
```

## 📚 Tài Liệu Bổ Sung

### Code Documentation

- Javadoc comments trong source code
- Inline comments cho complex logic
- README files trong các package

### External Documentation

- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Docs](https://docs.spring.io/spring-security/reference/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)

## 🧪 Testing

### Run Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=ProductServiceTest

# Integration tests
mvn verify
```

### Test Coverage

```bash
# Generate coverage report
mvn jacoco:report

# View report
open target/site/jacoco/index.html
```

## 📊 Monitoring

### Health Check

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db
```

### Metrics

```bash
# JVM metrics
curl http://localhost:8080/actuator/metrics

# HTTP metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## 🤝 Contributing

Dự án này là private, chỉ dành cho team nội bộ.

## 📄 License

Private - All rights reserved © 2024 Jenka Coffee

## 👥 Team

- **Backend**: Spring Boot 3 + PostgreSQL
- **Frontend**: Vue 3 + Vite + Tailwind CSS
- **DevOps**: Railway (Backend) + Vercel (Frontend)
- **Database**: Neon.tech (PostgreSQL)
- **Storage**: Cloudinary (Images)

## 📞 Contact

- **Website**: https://jenkacoffee.com
- **Facebook**: https://www.facebook.com/tung.mia.5
- **Zalo**: 0817909090
- **Email**: coffeejenka274@gmail.com

---

Made with ❤️ by Jenka Coffee Team
