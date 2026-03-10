-- ======================================================================
-- SCRIPT TẠO LẠI CƠ SỞ DỮ LIỆU JENKA COFFEE CHO POSTGRESQL
-- Bao gồm cấu trúc bảng (schema) và các chỉ mục (indexes).
-- ======================================================================

-- ----------------------------------------------------------------------
-- PHẦN 0: DỌN DẸP DỮ LIỆU CŨ (DROP TABLES)
-- Xóa bảng theo thứ tự con trước, cha sau
-- cascade: Tự động xóa các phụ thuộc nếu có.
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS "pointhistory" CASCADE;
DROP TABLE IF EXISTS "servicebookings" CASCADE;
DROP TABLE IF EXISTS "payments" CASCADE;
DROP TABLE IF EXISTS "orderdetails" CASCADE;
DROP TABLE IF EXISTS "orders" CASCADE;
DROP TABLE IF EXISTS "vouchers" CASCADE;
DROP TABLE IF EXISTS "products" CASCADE;
DROP TABLE IF EXISTS "categories" CASCADE;
DROP TABLE IF EXISTS "accounts" CASCADE;
DROP TABLE IF EXISTS "news" CASCADE;

-- ----------------------------------------------------------------------
-- PHẦN 1: TẠO BẢNG
-- ----------------------------------------------------------------------

-- 1. ACCOUNTS
CREATE TABLE "accounts" (
    "username" VARCHAR(50) PRIMARY KEY,
    "password_hash" VARCHAR(255) NOT NULL,
    "fullname" VARCHAR(100) NOT NULL,
    "email" VARCHAR(100) NOT NULL UNIQUE,
    "phone" VARCHAR(15) UNIQUE,
    "phone_verified" BOOLEAN DEFAULT FALSE,
    "photo" VARCHAR(255),
    "activated" BOOLEAN DEFAULT TRUE,
    "admin" BOOLEAN DEFAULT FALSE,
    "points" INTEGER DEFAULT 0,
    "customer_rank" VARCHAR(20) DEFAULT 'MEMBER'
);

-- 2. CATEGORIES
CREATE TABLE "categories" (
    "id" VARCHAR(50) PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "icon" VARCHAR(255)
);

-- 3. PRODUCTS
-- PostgreSQL sử dụng SERIAL cho các cột tự tăng
CREATE TABLE "products" (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(200) NOT NULL,
    "image" VARCHAR(255),
    "price" DECIMAL(18,2) NOT NULL CHECK ("price" >= 0),
    "createdate" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "available" BOOLEAN DEFAULT TRUE,
    "categoryid" VARCHAR(50) NOT NULL REFERENCES "categories"("id") ON DELETE RESTRICT,
    "description" TEXT,
    "quantity" INTEGER DEFAULT 0
);

-- 4. VOUCHERS
CREATE TABLE "vouchers" (
    "code" VARCHAR(20) PRIMARY KEY,
    "discountamount" DECIMAL(18,2) NOT NULL,
    "discounttype" VARCHAR(10) DEFAULT 'FIXED', 
    "minorderamount" DECIMAL(18,2),
    "expirationdate" TIMESTAMP NOT NULL,
    "quantity" INTEGER DEFAULT 0,
    "active" BOOLEAN DEFAULT TRUE
);

-- 5. ORDERS
CREATE TABLE "orders" (
    "id" BIGSERIAL PRIMARY KEY,
    "username" VARCHAR(50) NOT NULL REFERENCES "accounts"("username") ON DELETE RESTRICT,
    "createdate" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "address" VARCHAR(255) NOT NULL,
    "phone" VARCHAR(15) NOT NULL,
    "status" INTEGER DEFAULT 0,
    "vouchercode" VARCHAR(20) REFERENCES "vouchers"("code") ON DELETE SET NULL,
    "totalamount" DECIMAL(18,2)
);

-- 6. ORDERDETAILS
CREATE TABLE "orderdetails" (
    "id" BIGSERIAL PRIMARY KEY,
    "orderid" BIGINT NOT NULL REFERENCES "orders"("id") ON DELETE CASCADE,
    "productid" INTEGER NOT NULL REFERENCES "products"("id") ON DELETE RESTRICT,
    "price" DECIMAL(18,2) NOT NULL,
    "quantity" INTEGER NOT NULL CHECK ("quantity" > 0)
);

-- 7. PAYMENTS
CREATE TABLE "payments" (
    "id" BIGSERIAL PRIMARY KEY,
    "orderid" BIGINT NOT NULL REFERENCES "orders"("id") ON DELETE CASCADE,
    "amount" DECIMAL(18,2) NOT NULL,
    "paymentmethod" VARCHAR(20) NOT NULL,
    "transactioncode" VARCHAR(50),
    "paymentdate" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "status" VARCHAR(20) DEFAULT 'PENDING'
);

-- 8. SERVICEBOOKINGS
CREATE TABLE "servicebookings" (
    "id" BIGSERIAL PRIMARY KEY,
    "username" VARCHAR(50) REFERENCES "accounts"("username") ON DELETE SET NULL,
    "customername" VARCHAR(100) NOT NULL,
    "phone" VARCHAR(15) NOT NULL,
    "description" TEXT NOT NULL,
    "bookingdate" TIMESTAMP NOT NULL,
    "preferredtime" VARCHAR(50),
    "status" VARCHAR(20) DEFAULT 'PENDING'
);

-- 9. POINTHISTORY
CREATE TABLE "pointhistory" (
    "id" BIGSERIAL PRIMARY KEY,
    "username" VARCHAR(50) NOT NULL REFERENCES "accounts"("username") ON DELETE CASCADE,
    "amount" INTEGER NOT NULL,
    "orderid" BIGINT REFERENCES "orders"("id") ON DELETE SET NULL,
    "reason" VARCHAR(255) NOT NULL,
    "createdate" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. NEWS
CREATE TABLE "news" (
    "id" SERIAL PRIMARY KEY,
    "title" VARCHAR(255) NOT NULL,
    "content" TEXT,
    "image" VARCHAR(500),
    "createdate" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "available" BOOLEAN DEFAULT TRUE
);

-- ----------------------------------------------------------------------
-- PHẦN 2: TẠO CHỈ MỤC (INDEXES)
-- Để tối ưu hóa truy vấn trên các cột khóa ngoại hoặc thường xuyên tìm kiếm
-- ----------------------------------------------------------------------

CREATE INDEX idx_products_category ON "products"("categoryid");
CREATE INDEX idx_orders_username ON "orders"("username");
CREATE INDEX idx_orders_vouchercode ON "orders"("vouchercode");
CREATE INDEX idx_orderdetails_orderid ON "orderdetails"("orderid");
CREATE INDEX idx_orderdetails_productid ON "orderdetails"("productid");
CREATE INDEX idx_payments_orderid ON "payments"("orderid");
CREATE INDEX idx_servicebookings_username ON "servicebookings"("username");
CREATE INDEX idx_pointhistory_username ON "pointhistory"("username");
CREATE INDEX idx_pointhistory_orderid ON "pointhistory"("orderid");
