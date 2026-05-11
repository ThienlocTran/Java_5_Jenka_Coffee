-- ============================================================
-- JENKA COFFEE — PostgreSQL Production Schema
-- Target: Vietnix VPS PostgreSQL
-- Frontend: Vercel (jenkacoffee.com)
-- Version: 3.0 - Clean production schema, no booking/voucher
--
-- HUONG DAN CHAY:
--   psql -U <db_user> -d <db_name> -f init_production.sql
--
-- LUU Y:
--   Script nay DROP va tao lai toan bo schema.
--   KHONG chay tren DB dang co du lieu that tru khi da backup!
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- ============================================================
-- BUOC 1: XOA CAC BANG KHONG CON DUNG
-- ============================================================

DROP TABLE IF EXISTS servicebookings CASCADE;
DROP TABLE IF EXISTS voucherusage CASCADE;
DROP TABLE IF EXISTS vouchers CASCADE;
DROP TABLE IF EXISTS banner CASCADE;
DROP TABLE IF EXISTS "Banner" CASCADE;

-- ============================================================
-- BUOC 2: XOA TOAN BO BANG HIEN TAI (clean slate)
-- ============================================================

DROP TABLE IF EXISTS visitor_stats CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS store_feedbacks CASCADE;
DROP TABLE IF EXISTS "PointHistory" CASCADE;
DROP TABLE IF EXISTS pointhistory CASCADE;
DROP TABLE IF EXISTS "Payments" CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS "OrderDetails" CASCADE;
DROP TABLE IF EXISTS orderdetails CASCADE;
DROP TABLE IF EXISTS "Orders" CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS "ProductImages" CASCADE;
DROP TABLE IF EXISTS productimages CASCADE;
DROP TABLE IF EXISTS "Products" CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS "Categories" CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS "News" CASCADE;
DROP TABLE IF EXISTS news CASCADE;
DROP TABLE IF EXISTS "Contacts" CASCADE;
DROP TABLE IF EXISTS contacts CASCADE;
DROP TABLE IF EXISTS banner_image CASCADE;
DROP TABLE IF EXISTS banner_set CASCADE;
DROP TABLE IF EXISTS "Accounts" CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;

DROP SEQUENCE IF EXISTS orders_id_seq CASCADE;
DROP SEQUENCE IF EXISTS products_id_seq CASCADE;
DROP SEQUENCE IF EXISTS orderdetails_id_seq CASCADE;
DROP SEQUENCE IF EXISTS payments_id_seq CASCADE;
DROP SEQUENCE IF EXISTS pointhistory_id_seq CASCADE;
DROP SEQUENCE IF EXISTS productimages_id_seq CASCADE;
DROP SEQUENCE IF EXISTS news_id_seq CASCADE;
DROP SEQUENCE IF EXISTS contacts_id_seq CASCADE;
DROP SEQUENCE IF EXISTS banner_set_id_seq CASCADE;
DROP SEQUENCE IF EXISTS banner_image_id_seq CASCADE;
DROP SEQUENCE IF EXISTS store_feedbacks_id_seq CASCADE;
DROP SEQUENCE IF EXISTS cart_items_id_seq CASCADE;


-- ============================================================
-- BUOC 3: TAO CAC BANG MOI (theo dung JPA Entity)
-- ============================================================


-- ----------------------------------------------------------
-- 1. ACCOUNTS (Entity: Account.java)
-- ----------------------------------------------------------
CREATE TABLE "Accounts" (
    "Username"              VARCHAR(50)     NOT NULL,
    password_hash           VARCHAR(255)    NOT NULL,
    "Fullname"              VARCHAR(255)    NOT NULL,
    "Email"                 VARCHAR(100)    UNIQUE,
    phone                   VARCHAR(15)     UNIQUE,
    phone_verified          BOOLEAN         NOT NULL DEFAULT FALSE,
    "Photo"                 VARCHAR(500),
    "Activated"             BOOLEAN         NOT NULL DEFAULT TRUE,
    "Admin"                 BOOLEAN         NOT NULL DEFAULT FALSE,
    points                  INTEGER         NOT NULL DEFAULT 0,
    customer_rank           VARCHAR(20)     NOT NULL DEFAULT 'MEMBER',
    "ActivationToken"       VARCHAR(100),
    "ActivationTokenExpiry" TIMESTAMP,
    "ResetToken"            VARCHAR(100),
    "ResetTokenExpiry"      TIMESTAMP,
    "ActivationMethod"      VARCHAR(10),
    "lastPasswordResetDate" TIMESTAMP,
    CONSTRAINT "Accounts_pkey" PRIMARY KEY ("Username")
);

CREATE INDEX idx_accounts_email      ON "Accounts" ("Email");
CREATE INDEX idx_accounts_phone      ON "Accounts" (phone);
CREATE INDEX idx_accounts_activated  ON "Accounts" ("Activated");
CREATE INDEX idx_accounts_admin      ON "Accounts" ("Admin");
CREATE INDEX idx_accounts_resettoken ON "Accounts" ("ResetToken") WHERE "ResetToken" IS NOT NULL;
CREATE INDEX idx_accounts_acttoken   ON "Accounts" ("ActivationToken") WHERE "ActivationToken" IS NOT NULL;

COMMENT ON TABLE "Accounts" IS 'Tai khoan nguoi dung - khach hang va admin';


-- ----------------------------------------------------------
-- 2. CATEGORIES (Entity: Category.java)
-- ----------------------------------------------------------
CREATE TABLE "Categories" (
    "Id"   VARCHAR(50)  NOT NULL,
    "Name" VARCHAR(100) NOT NULL,
    "Icon" VARCHAR(500),
    CONSTRAINT "Categories_pkey" PRIMARY KEY ("Id")
);

COMMENT ON TABLE "Categories" IS 'Danh muc san pham (ID tu nhap, VD: MAY_PHA)';


-- ----------------------------------------------------------
-- 3. PRODUCTS (Entity: Product.java)
-- ----------------------------------------------------------
CREATE SEQUENCE products_id_seq;
CREATE TABLE "Products" (
    "Id"             INTEGER       NOT NULL DEFAULT nextval('products_id_seq'),
    "Name"           VARCHAR(200)  NOT NULL,
    slug             VARCHAR(300)  UNIQUE,
    "Image"          VARCHAR(500),
    price            NUMERIC(18,2) NOT NULL,
    description      TEXT,
    "createDate"     TIMESTAMP     NOT NULL DEFAULT NOW(),
    "Available"      BOOLEAN       NOT NULL DEFAULT TRUE,
    "isFeatured"     BOOLEAN       NOT NULL DEFAULT FALSE,
    "requireContact" BOOLEAN       NOT NULL DEFAULT FALSE,
    "Categoryid"     VARCHAR(50)   NOT NULL,
    CONSTRAINT "Products_pkey"      PRIMARY KEY ("Id"),
    CONSTRAINT fk_products_category FOREIGN KEY ("Categoryid")
        REFERENCES "Categories" ("Id") ON DELETE RESTRICT
);

CREATE INDEX idx_products_category  ON "Products" ("Categoryid");
CREATE INDEX idx_products_available ON "Products" ("Available");
CREATE INDEX idx_products_featured  ON "Products" ("isFeatured");
CREATE INDEX idx_products_slug      ON "Products" (slug);

COMMENT ON TABLE "Products" IS 'San pham ca phe va may moc';


-- ----------------------------------------------------------
-- 4. PRODUCT IMAGES (Entity: ProductImage.java)
-- ----------------------------------------------------------
CREATE SEQUENCE productimages_id_seq;
CREATE TABLE "ProductImages" (
    "Id"           INTEGER      NOT NULL DEFAULT nextval('productimages_id_seq'),
    "ImageUrl"     VARCHAR(500) NOT NULL,
    "DisplayOrder" INTEGER      NOT NULL DEFAULT 0,
    "IsPrimary"    BOOLEAN      NOT NULL DEFAULT FALSE,
    "CreateDate"   TIMESTAMP    NOT NULL DEFAULT NOW(),
    "ProductId"    INTEGER      NOT NULL,
    CONSTRAINT "ProductImages_pkey"     PRIMARY KEY ("Id"),
    CONSTRAINT fk_productimages_product FOREIGN KEY ("ProductId")
        REFERENCES "Products" ("Id") ON DELETE CASCADE
);

CREATE INDEX idx_productimages_product ON "ProductImages" ("ProductId");
CREATE INDEX idx_productimages_order   ON "ProductImages" ("ProductId", "DisplayOrder");

COMMENT ON TABLE "ProductImages" IS 'Anh gallery cua san pham (nhieu anh)';


-- ----------------------------------------------------------
-- 5. ORDERS (Entity: Order.java)
-- Status: 0=NEW, 1=CONFIRMED, 2=CANCELLED
-- Public URL: /orders/{orderCode}; never expose numeric Id to customers
-- Physical column name is lowercase ordercode to avoid PostgreSQL quoted camelCase issues.
-- ----------------------------------------------------------
CREATE SEQUENCE orders_id_seq;
CREATE TABLE orders (
    id            BIGINT        NOT NULL DEFAULT nextval('orders_id_seq'),
    ordercode     VARCHAR(30)   NOT NULL,
    address       VARCHAR(500)  NOT NULL,
    createdate    TIMESTAMP     NOT NULL DEFAULT NOW(),
    phone         VARCHAR(15),
    status        INTEGER       NOT NULL DEFAULT 0
                                CHECK (status IN (0, 1, 2)),
    totalamount   NUMERIC(18,2),
    pointsused    INTEGER       NOT NULL DEFAULT 0,
    note          VARCHAR(500),
    username      VARCHAR(50),
    CONSTRAINT orders_pkey       PRIMARY KEY (id),
    CONSTRAINT uq_order_code     UNIQUE (ordercode),
    CONSTRAINT fk_orders_account FOREIGN KEY (username)
        REFERENCES "Accounts" ("Username") ON DELETE SET NULL
);

CREATE INDEX idx_order_code        ON orders (ordercode);
CREATE INDEX idx_orders_username   ON orders (username);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_createdate ON orders (createdate DESC);
CREATE INDEX idx_orders_user_date  ON orders (username, createdate DESC);

COMMENT ON TABLE orders IS 'Don hang. Status: 0=NEW, 1=CONFIRMED, 2=CANCELLED';


-- ----------------------------------------------------------
-- 6. ORDER DETAILS (Entity: OrderDetail.java)
-- ----------------------------------------------------------
CREATE SEQUENCE orderdetails_id_seq;
CREATE TABLE "OrderDetails" (
    "Id"        BIGINT        NOT NULL DEFAULT nextval('orderdetails_id_seq'),
    price       NUMERIC(18,2) NOT NULL,
    "Quantity"  INTEGER       NOT NULL CHECK ("Quantity" > 0),
    "Orderid"   BIGINT        NOT NULL,
    "Productid" INTEGER       NOT NULL,
    CONSTRAINT "OrderDetails_pkey"     PRIMARY KEY ("Id"),
    CONSTRAINT fk_orderdetails_order   FOREIGN KEY ("Orderid")
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_orderdetails_product FOREIGN KEY ("Productid")
        REFERENCES "Products" ("Id") ON DELETE RESTRICT
);

CREATE INDEX idx_orderdetails_order   ON "OrderDetails" ("Orderid");
CREATE INDEX idx_orderdetails_product ON "OrderDetails" ("Productid");

COMMENT ON TABLE "OrderDetails" IS 'Chi tiet don hang - gia snapshot tai thoi diem dat';


-- ----------------------------------------------------------
-- 7. PAYMENTS (Entity: Payment.java)
-- Status: PENDING, SUCCESS, FAILED
-- ----------------------------------------------------------
CREATE SEQUENCE payments_id_seq;
CREATE TABLE "Payments" (
    id                BIGINT        NOT NULL DEFAULT nextval('payments_id_seq'),
    "OrderId"         BIGINT        NOT NULL,
    amount            NUMERIC(18,2) NOT NULL,
    "paymentMethod"   VARCHAR(20)   NOT NULL DEFAULT 'COD',
    "transactionCode" VARCHAR(50),
    "paymentDate"     TIMESTAMP     NOT NULL DEFAULT NOW(),
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT payments_pkey     PRIMARY KEY (id),
    CONSTRAINT fk_payments_order FOREIGN KEY ("OrderId")
        REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_payments_orderid ON "Payments" ("OrderId");
CREATE INDEX idx_payments_status  ON "Payments" (status);

COMMENT ON TABLE "Payments" IS 'Lich su thanh toan - audit trail cho moi don hang';


-- ----------------------------------------------------------
-- 8. POINT HISTORY (Entity: PointHistory.java)
-- ----------------------------------------------------------
CREATE SEQUENCE pointhistory_id_seq;
CREATE TABLE "PointHistory" (
    id           BIGINT       NOT NULL DEFAULT nextval('pointhistory_id_seq'),
    username     VARCHAR(50)  NOT NULL,
    amount       INTEGER      NOT NULL,
    "OrderId"    BIGINT,
    reason       VARCHAR(255) NOT NULL,
    "createDate" TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pointhistory_pkey       PRIMARY KEY (id),
    CONSTRAINT fk_pointhistory_account FOREIGN KEY (username)
        REFERENCES "Accounts" ("Username") ON DELETE CASCADE,
    CONSTRAINT fk_pointhistory_order   FOREIGN KEY ("OrderId")
        REFERENCES orders (id) ON DELETE SET NULL
);

CREATE INDEX idx_pointhistory_username ON "PointHistory" (username);
CREATE INDEX idx_pointhistory_order    ON "PointHistory" ("OrderId");
CREATE INDEX idx_pointhistory_date     ON "PointHistory" ("createDate" DESC);

COMMENT ON TABLE "PointHistory" IS 'Lich su diem tich luy. amount > 0: tich, < 0: tieu';


-- ----------------------------------------------------------
-- 9. NEWS (Entity: News.java)
-- ----------------------------------------------------------
CREATE SEQUENCE news_id_seq;
CREATE TABLE "News" (
    "Id"         INTEGER      NOT NULL DEFAULT nextval('news_id_seq'),
    "Title"      VARCHAR(500) NOT NULL,
    "Content"    TEXT,
    "Image"      VARCHAR(500),
    "CreateDate" TIMESTAMP    NOT NULL DEFAULT NOW(),
    "Available"  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT "News_pkey" PRIMARY KEY ("Id")
);

CREATE INDEX idx_news_available  ON "News" ("Available");
CREATE INDEX idx_news_createdate ON "News" ("CreateDate" DESC);

COMMENT ON TABLE "News" IS 'Tin tuc va bai viet blog';


-- ----------------------------------------------------------
-- 10. CONTACTS (Entity: Contact.java)
-- ----------------------------------------------------------
CREATE SEQUENCE contacts_id_seq;
CREATE TABLE "Contacts" (
    id          BIGINT       NOT NULL DEFAULT nextval('contacts_id_seq'),
    "fullName"  VARCHAR(100) NOT NULL,
    phone       VARCHAR(15),
    email       VARCHAR(150),
    subject     VARCHAR(200),
    message     TEXT,
    "createdAt" TIMESTAMP    NOT NULL DEFAULT NOW(),
    "isRead"    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT "Contacts_pkey" PRIMARY KEY (id)
);

CREATE INDEX idx_contacts_isread    ON "Contacts" ("isRead");
CREATE INDEX idx_contacts_createdat ON "Contacts" ("createdAt" DESC);

COMMENT ON TABLE "Contacts" IS 'Lien he tu khach hang qua form';


-- ----------------------------------------------------------
-- 11. BANNER SET (Entity: BannerSet.java)
-- ----------------------------------------------------------
CREATE SEQUENCE banner_set_id_seq;
CREATE TABLE banner_set (
    id         BIGINT       NOT NULL DEFAULT nextval('banner_set_id_seq'),
    name       VARCHAR(100) NOT NULL,
    effect     VARCHAR(30)  NOT NULL DEFAULT 'fade',
    active     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT banner_set_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_banner_set_active ON banner_set (active);

COMMENT ON TABLE banner_set IS 'Bo banner slide show. Chi 1 bo active tai 1 thoi diem';


-- ----------------------------------------------------------
-- 12. BANNER IMAGE (Entity: BannerImage.java)
-- ----------------------------------------------------------
CREATE SEQUENCE banner_image_id_seq;
CREATE TABLE banner_image (
    id            BIGINT       NOT NULL DEFAULT nextval('banner_image_id_seq'),
    banner_set_id BIGINT       NOT NULL,
    image         VARCHAR(500) NOT NULL,
    title         VARCHAR(200),
    subtitle      VARCHAR(300),
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT banner_image_pkey   PRIMARY KEY (id),
    CONSTRAINT fk_bannerimage_set  FOREIGN KEY (banner_set_id)
        REFERENCES banner_set (id) ON DELETE CASCADE
);

CREATE INDEX idx_banner_image_set   ON banner_image (banner_set_id);
CREATE INDEX idx_banner_image_order ON banner_image (banner_set_id, sort_order);

COMMENT ON TABLE banner_image IS 'Anh trong tung bo banner';


-- ----------------------------------------------------------
-- 13. STORE FEEDBACKS (Entity: StoreFeedback.java)
-- ----------------------------------------------------------
CREATE SEQUENCE store_feedbacks_id_seq;
CREATE TABLE store_feedbacks (
    id            BIGINT      NOT NULL DEFAULT nextval('store_feedbacks_id_seq'),
    branch        VARCHAR(10) NOT NULL,
    fullname      VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    comment       TEXT,
    "storeRating" INTEGER     NOT NULL CHECK ("storeRating" BETWEEN 1 AND 5),
    "staffRating" INTEGER     NOT NULL CHECK ("staffRating" BETWEEN 1 AND 5),
    "createdAt"   TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT store_feedbacks_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_feedbacks_branch    ON store_feedbacks (branch);
CREATE INDEX idx_feedbacks_createdat ON store_feedbacks ("createdAt" DESC);

COMMENT ON TABLE store_feedbacks IS 'Danh gia cua hang tu khach - hien thi popup trang chu';


-- ----------------------------------------------------------
-- 14. CART ITEMS (MOI - Thay the ConcurrentHashMap in-memory)
-- Persist gio hang vao DB, khong mat khi server restart
-- ----------------------------------------------------------
CREATE SEQUENCE cart_items_id_seq;
CREATE TABLE cart_items (
    id             BIGINT        NOT NULL DEFAULT nextval('cart_items_id_seq'),
    cart_key       VARCHAR(255)  NOT NULL,
    -- cart_key = username cho user da dang nhap
    -- cart_key = 'anon:<uuid>' cho anonymous user (UUID luu trong cookie)
    product_id     INTEGER       NOT NULL,
    quantity       INTEGER       NOT NULL DEFAULT 1
                                 CHECK (quantity > 0 AND quantity <= 99),
    price_snapshot NUMERIC(18,2) NOT NULL,
    product_name   VARCHAR(200)  NOT NULL,
    product_image  VARCHAR(500),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT cart_items_pkey        PRIMARY KEY (id),
    CONSTRAINT cart_items_unique_item UNIQUE (cart_key, product_id),
    CONSTRAINT fk_cart_product        FOREIGN KEY (product_id)
        REFERENCES "Products" ("Id") ON DELETE CASCADE
);

CREATE INDEX idx_cart_cart_key ON cart_items (cart_key);
CREATE INDEX idx_cart_updated  ON cart_items (updated_at);

COMMENT ON TABLE cart_items IS 'Gio hang persistent. TTL: cleanup cart > 24h khong active';


-- ----------------------------------------------------------
-- 15. VISITOR STATS (MOI - Thay the AtomicLong in-memory)
-- Persist thong ke luot truy cap, giu data khi restart server
-- ----------------------------------------------------------
CREATE TABLE visitor_stats (
    stat_date       DATE    NOT NULL,
    unique_visitors INTEGER NOT NULL DEFAULT 0,
    total_visits    BIGINT  NOT NULL DEFAULT 0,
    online_peak     INTEGER NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT visitor_stats_pkey PRIMARY KEY (stat_date)
);

CREATE INDEX idx_visitor_date ON visitor_stats (stat_date DESC);

-- Tao row hom nay de tranh NULL khi doc lan dau
INSERT INTO visitor_stats (stat_date, unique_visitors, total_visits, online_peak)
VALUES (CURRENT_DATE, 0, 0, 0)
ON CONFLICT (stat_date) DO NOTHING;

COMMENT ON TABLE visitor_stats IS 'Thong ke luot truy cap theo ngay. Tong hop hang ngay';


-- ============================================================
-- BUOC 4: SEQUENCE OWNERSHIP
-- ============================================================
ALTER SEQUENCE orders_id_seq          OWNED BY orders.id;
ALTER SEQUENCE products_id_seq        OWNED BY "Products"."Id";
ALTER SEQUENCE orderdetails_id_seq    OWNED BY "OrderDetails"."Id";
ALTER SEQUENCE payments_id_seq        OWNED BY "Payments".id;
ALTER SEQUENCE pointhistory_id_seq    OWNED BY "PointHistory".id;
ALTER SEQUENCE productimages_id_seq   OWNED BY "ProductImages"."Id";
ALTER SEQUENCE news_id_seq            OWNED BY "News"."Id";
ALTER SEQUENCE contacts_id_seq        OWNED BY "Contacts".id;
ALTER SEQUENCE banner_set_id_seq      OWNED BY banner_set.id;
ALTER SEQUENCE banner_image_id_seq    OWNED BY banner_image.id;
ALTER SEQUENCE store_feedbacks_id_seq OWNED BY store_feedbacks.id;
ALTER SEQUENCE cart_items_id_seq      OWNED BY cart_items.id;


-- ============================================================
-- BUOC 5: TAO ADMIN MAC DINH
-- Password: Admin@123 (BCrypt $2a$12$...)
-- DOI NGAY SAU KHI DEPLOY!
-- ============================================================
INSERT INTO "Accounts" (
    "Username", password_hash, "Fullname", "Email",
    phone, phone_verified, "Activated", "Admin",
    points, customer_rank
) VALUES (
    'admin',
    '$2a$12$WXQcv3T1xGMmAF4HkCm8S.yEEv8P1oX4UqWKq.P7aIDKU3Y5w/GEu',
    'Admin Jenka Coffee',
    'admin@jenkacoffee.com',
    NULL, TRUE, TRUE, TRUE, 0, 'VIP'
);


-- ============================================================
-- BUOC 6: DU LIEU MAU TOI THIEU
-- Danh muc mac dinh de admin tao san pham ngay
-- ============================================================
INSERT INTO "Categories" ("Id", "Name", "Icon") VALUES
    ('MAY_PHA',   'May Pha Ca Phe',     'may-pha.webp'),
    ('CA_PHE',    'Ca Phe Nguyen Chat', 'ca-phe.webp'),
    ('PHU_KIEN',  'Phu Kien',           'phu-kien.webp');

-- Tao 1 bo banner trong de admin them anh
INSERT INTO banner_set (name, effect, active)
VALUES ('Banner Trang Chu', 'fade', TRUE);


COMMIT;


-- ============================================================
-- VERIFY - Kiem tra sau khi chay xong
-- ============================================================
SELECT
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns c
     WHERE c.table_name = t.table_name AND c.table_schema = 'public') AS col_count
FROM information_schema.tables t
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Ket qua mong doi: 15 bang
-- Accounts, Categories, Products, ProductImages,
-- Orders, OrderDetails, Payments, PointHistory,
-- News, Contacts, banner_set, banner_image,
-- store_feedbacks, cart_items, visitor_stats
