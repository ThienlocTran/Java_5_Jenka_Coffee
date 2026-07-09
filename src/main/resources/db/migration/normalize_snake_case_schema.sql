-- Normalize the existing production schema to the snake_case names used by JPA.
-- Safe to run more than once. Back up production before running it.

CREATE OR REPLACE FUNCTION _jenka_rename_table_if_exists(old_table text, new_table text)
RETURNS void AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = old_table
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = new_table
    ) THEN
        EXECUTE format('ALTER TABLE %I RENAME TO %I', old_table, new_table);
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION _jenka_rename_column_if_exists(p_table_name text, p_old_column text, p_new_column text)
RETURNS void AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = p_table_name
          AND column_name = p_old_column
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = p_table_name
          AND column_name = p_new_column
    ) THEN
        EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', p_table_name, p_old_column, p_new_column);
    END IF;
END;
$$ LANGUAGE plpgsql;

SELECT _jenka_rename_table_if_exists('Accounts', 'accounts');
SELECT _jenka_rename_table_if_exists('Categories', 'categories');
SELECT _jenka_rename_table_if_exists('Products', 'products');
SELECT _jenka_rename_table_if_exists('ProductImages', 'product_images');
SELECT _jenka_rename_table_if_exists('Orders', 'orders');
SELECT _jenka_rename_table_if_exists('OrderDetails', 'order_details');
SELECT _jenka_rename_table_if_exists('Payments', 'payments');
SELECT _jenka_rename_table_if_exists('News', 'news');
SELECT _jenka_rename_table_if_exists('Contacts', 'contacts');
SELECT _jenka_rename_table_if_exists('productimages', 'product_images');
SELECT _jenka_rename_table_if_exists('orderdetails', 'order_details');
SELECT _jenka_rename_table_if_exists('pointhistory', 'point_history');

SELECT _jenka_rename_column_if_exists('accounts', 'Username', 'username');
SELECT _jenka_rename_column_if_exists('accounts', 'Fullname', 'fullname');
SELECT _jenka_rename_column_if_exists('accounts', 'Email', 'email');
SELECT _jenka_rename_column_if_exists('accounts', 'Photo', 'photo');
SELECT _jenka_rename_column_if_exists('accounts', 'Activated', 'activated');
SELECT _jenka_rename_column_if_exists('accounts', 'Admin', 'admin');
SELECT _jenka_rename_column_if_exists('accounts', 'ActivationToken', 'activation_token');
SELECT _jenka_rename_column_if_exists('accounts', 'ActivationTokenExpiry', 'activation_token_expiry');
SELECT _jenka_rename_column_if_exists('accounts', 'ResetToken', 'reset_token');
SELECT _jenka_rename_column_if_exists('accounts', 'ResetTokenExpiry', 'reset_token_expiry');
SELECT _jenka_rename_column_if_exists('accounts', 'ActivationMethod', 'activation_method');
SELECT _jenka_rename_column_if_exists('accounts', 'lastPasswordResetDate', 'last_password_reset_date');
SELECT _jenka_rename_column_if_exists('accounts', 'createdate', 'create_date');
SELECT _jenka_rename_column_if_exists('accounts', 'passwordhash', 'password_hash');
SELECT _jenka_rename_column_if_exists('accounts', 'phoneverified', 'phone_verified');
SELECT _jenka_rename_column_if_exists('accounts', 'customerrank', 'customer_rank');
SELECT _jenka_rename_column_if_exists('accounts', 'activationtoken', 'activation_token');
SELECT _jenka_rename_column_if_exists('accounts', 'activationtokenexpiry', 'activation_token_expiry');
SELECT _jenka_rename_column_if_exists('accounts', 'resettoken', 'reset_token');
SELECT _jenka_rename_column_if_exists('accounts', 'resettokenexpiry', 'reset_token_expiry');
SELECT _jenka_rename_column_if_exists('accounts', 'activationmethod', 'activation_method');
SELECT _jenka_rename_column_if_exists('accounts', 'lastpasswordresetdate', 'last_password_reset_date');

SELECT _jenka_rename_column_if_exists('categories', 'Id', 'id');
SELECT _jenka_rename_column_if_exists('categories', 'Name', 'name');
SELECT _jenka_rename_column_if_exists('categories', 'Icon', 'icon');

SELECT _jenka_rename_column_if_exists('products', 'Id', 'id');
SELECT _jenka_rename_column_if_exists('products', 'Name', 'name');
SELECT _jenka_rename_column_if_exists('products', 'Image', 'image');
SELECT _jenka_rename_column_if_exists('products', 'createDate', 'create_date');
SELECT _jenka_rename_column_if_exists('products', 'Available', 'available');
SELECT _jenka_rename_column_if_exists('products', 'isFeatured', 'is_featured');
SELECT _jenka_rename_column_if_exists('products', 'requireContact', 'require_contact');
SELECT _jenka_rename_column_if_exists('products', 'Categoryid', 'category_id');
SELECT _jenka_rename_column_if_exists('products', 'createdate', 'create_date');
SELECT _jenka_rename_column_if_exists('products', 'isfeatured', 'is_featured');
SELECT _jenka_rename_column_if_exists('products', 'featuredposition', 'featured_position');
SELECT _jenka_rename_column_if_exists('products', 'requirecontact', 'require_contact');
SELECT _jenka_rename_column_if_exists('products', 'categoryid', 'category_id');

SELECT _jenka_rename_column_if_exists('news', 'Id', 'id');
SELECT _jenka_rename_column_if_exists('news', 'Title', 'title');
SELECT _jenka_rename_column_if_exists('news', 'Content', 'content');
SELECT _jenka_rename_column_if_exists('news', 'Image', 'image');
SELECT _jenka_rename_column_if_exists('news', 'CreateDate', 'create_date');
SELECT _jenka_rename_column_if_exists('news', 'Available', 'available');
SELECT _jenka_rename_column_if_exists('news', 'createdate', 'create_date');

SELECT _jenka_rename_column_if_exists('orders', 'Id', 'id');
SELECT _jenka_rename_column_if_exists('orders', 'Address', 'address');
SELECT _jenka_rename_column_if_exists('orders', 'CreateDate', 'create_date');
SELECT _jenka_rename_column_if_exists('orders', 'Phone', 'phone');
SELECT _jenka_rename_column_if_exists('orders', 'Status', 'status');
SELECT _jenka_rename_column_if_exists('orders', 'Username', 'username');
SELECT _jenka_rename_column_if_exists('orders', 'createdate', 'create_date');
SELECT _jenka_rename_column_if_exists('orders', 'orderCode', 'order_code');
SELECT _jenka_rename_column_if_exists('orders', 'ordercode', 'order_code');
SELECT _jenka_rename_column_if_exists('orders', 'totalamount', 'total_amount');
SELECT _jenka_rename_column_if_exists('orders', 'totalAmount', 'total_amount');
SELECT _jenka_rename_column_if_exists('orders', 'pointsused', 'points_used');
SELECT _jenka_rename_column_if_exists('orders', 'pointsUsed', 'points_used');

SELECT _jenka_rename_column_if_exists('order_details', 'Id', 'id');
SELECT _jenka_rename_column_if_exists('order_details', 'Quantity', 'quantity');
SELECT _jenka_rename_column_if_exists('order_details', 'Orderid', 'order_id');
SELECT _jenka_rename_column_if_exists('order_details', 'Productid', 'product_id');
SELECT _jenka_rename_column_if_exists('order_details', 'orderid', 'order_id');
SELECT _jenka_rename_column_if_exists('order_details', 'productid', 'product_id');

SELECT _jenka_rename_column_if_exists('product_images', 'Id', 'id');
SELECT _jenka_rename_column_if_exists('product_images', 'ImageUrl', 'image_url');
SELECT _jenka_rename_column_if_exists('product_images', 'DisplayOrder', 'display_order');
SELECT _jenka_rename_column_if_exists('product_images', 'IsPrimary', 'is_primary');
SELECT _jenka_rename_column_if_exists('product_images', 'CreateDate', 'create_date');
SELECT _jenka_rename_column_if_exists('product_images', 'ProductId', 'product_id');
SELECT _jenka_rename_column_if_exists('product_images', 'imageurl', 'image_url');
SELECT _jenka_rename_column_if_exists('product_images', 'displayorder', 'display_order');
SELECT _jenka_rename_column_if_exists('product_images', 'isprimary', 'is_primary');
SELECT _jenka_rename_column_if_exists('product_images', 'createdate', 'create_date');
SELECT _jenka_rename_column_if_exists('product_images', 'productid', 'product_id');

SELECT _jenka_rename_column_if_exists('payments', 'OrderId', 'order_id');
SELECT _jenka_rename_column_if_exists('payments', 'paymentMethod', 'payment_method');
SELECT _jenka_rename_column_if_exists('payments', 'transactionCode', 'transaction_code');
SELECT _jenka_rename_column_if_exists('payments', 'paymentDate', 'payment_date');
SELECT _jenka_rename_column_if_exists('payments', 'orderid', 'order_id');
SELECT _jenka_rename_column_if_exists('payments', 'paymentmethod', 'payment_method');
SELECT _jenka_rename_column_if_exists('payments', 'transactioncode', 'transaction_code');
SELECT _jenka_rename_column_if_exists('payments', 'paymentdate', 'payment_date');

SELECT _jenka_rename_column_if_exists('point_history', 'orderid', 'order_id');
SELECT _jenka_rename_column_if_exists('point_history', 'createdate', 'create_date');

SELECT _jenka_rename_column_if_exists('contacts', 'fullName', 'full_name');
SELECT _jenka_rename_column_if_exists('contacts', 'createdAt', 'created_at');
SELECT _jenka_rename_column_if_exists('contacts', 'isRead', 'is_read');
SELECT _jenka_rename_column_if_exists('contacts', 'fullname', 'full_name');
SELECT _jenka_rename_column_if_exists('contacts', 'fullName', 'full_name');
SELECT _jenka_rename_column_if_exists('contacts', 'createdat', 'created_at');
SELECT _jenka_rename_column_if_exists('contacts', 'createdAt', 'created_at');
SELECT _jenka_rename_column_if_exists('contacts', 'isread', 'is_read');
SELECT _jenka_rename_column_if_exists('contacts', 'isRead', 'is_read');

SELECT _jenka_rename_column_if_exists('store_feedbacks', 'storerating', 'store_rating');
SELECT _jenka_rename_column_if_exists('store_feedbacks', 'storeRating', 'store_rating');
SELECT _jenka_rename_column_if_exists('store_feedbacks', 'staffrating', 'staff_rating');
SELECT _jenka_rename_column_if_exists('store_feedbacks', 'staffRating', 'staff_rating');
SELECT _jenka_rename_column_if_exists('store_feedbacks', 'createdat', 'created_at');
SELECT _jenka_rename_column_if_exists('store_feedbacks', 'createdAt', 'created_at');

SELECT _jenka_rename_column_if_exists('banner_image', 'bannersetid', 'banner_set_id');
SELECT _jenka_rename_column_if_exists('banner_image', 'sortorder', 'sort_order');
SELECT _jenka_rename_column_if_exists('banner_set', 'createdat', 'created_at');

ALTER TABLE IF EXISTS accounts
    ADD COLUMN IF NOT EXISTS create_date timestamp DEFAULT now(),
    ADD COLUMN IF NOT EXISTS phone_verified boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS points integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS customer_rank varchar(20) DEFAULT 'BRONZE',
    ADD COLUMN IF NOT EXISTS activation_method varchar(10) DEFAULT 'EMAIL';

ALTER TABLE IF EXISTS products
    ADD COLUMN IF NOT EXISTS category_id varchar(50),
    ADD COLUMN IF NOT EXISTS create_date timestamp DEFAULT now(),
    ADD COLUMN IF NOT EXISTS is_featured boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS featured_position integer,
    ADD COLUMN IF NOT EXISTS require_contact boolean DEFAULT false;

ALTER TABLE IF EXISTS news
    ADD COLUMN IF NOT EXISTS create_date timestamp DEFAULT now(),
    ADD COLUMN IF NOT EXISTS available boolean DEFAULT true;

ALTER TABLE IF EXISTS orders
    ADD COLUMN IF NOT EXISTS order_code varchar(30),
    ADD COLUMN IF NOT EXISTS create_date timestamp DEFAULT now(),
    ADD COLUMN IF NOT EXISTS total_amount numeric(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS points_used integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS note varchar(500);

UPDATE orders
SET order_code = 'ORD' || lpad(id::text, 8, '0')
WHERE order_code IS NULL OR trim(order_code) = '';

ALTER TABLE IF EXISTS contacts
    ADD COLUMN IF NOT EXISTS full_name varchar(100),
    ADD COLUMN IF NOT EXISTS created_at timestamp DEFAULT now(),
    ADD COLUMN IF NOT EXISTS is_read boolean DEFAULT false;

UPDATE contacts
SET full_name = COALESCE(NULLIF(trim(full_name), ''), 'Khach hang')
WHERE full_name IS NULL OR trim(full_name) = '';

ALTER TABLE IF EXISTS store_feedbacks
    ADD COLUMN IF NOT EXISTS store_rating integer DEFAULT 5,
    ADD COLUMN IF NOT EXISTS staff_rating integer DEFAULT 5,
    ADD COLUMN IF NOT EXISTS created_at timestamp DEFAULT now();

ALTER TABLE IF EXISTS product_images
    ADD COLUMN IF NOT EXISTS image_url varchar(500),
    ADD COLUMN IF NOT EXISTS product_id integer,
    ADD COLUMN IF NOT EXISTS display_order integer DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_primary boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS create_date timestamp DEFAULT now();

ALTER TABLE IF EXISTS order_details
    ADD COLUMN IF NOT EXISTS order_id bigint,
    ADD COLUMN IF NOT EXISTS product_id integer;

ALTER TABLE IF EXISTS payments
    ADD COLUMN IF NOT EXISTS order_id bigint,
    ADD COLUMN IF NOT EXISTS payment_method varchar(20),
    ADD COLUMN IF NOT EXISTS transaction_code varchar(50),
    ADD COLUMN IF NOT EXISTS payment_date timestamp DEFAULT now(),
    ADD COLUMN IF NOT EXISTS status varchar(20);

ALTER TABLE IF EXISTS point_history
    ADD COLUMN IF NOT EXISTS order_id bigint,
    ADD COLUMN IF NOT EXISTS create_date timestamp DEFAULT now();

ALTER TABLE IF EXISTS banner_set
    ADD COLUMN IF NOT EXISTS created_at timestamp DEFAULT now();

ALTER TABLE IF EXISTS orders ALTER COLUMN order_code SET NOT NULL;
ALTER TABLE IF EXISTS contacts ALTER COLUMN full_name SET NOT NULL;
ALTER TABLE IF EXISTS contacts ALTER COLUMN is_read SET NOT NULL;
ALTER TABLE IF EXISTS store_feedbacks ALTER COLUMN created_at SET NOT NULL;

DO $$
BEGIN
    IF to_regclass('orders') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS idx_order_code ON orders(order_code);
    END IF;

    IF to_regclass('products') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
    END IF;

    IF to_regclass('product_images') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
    END IF;

    IF to_regclass('order_details') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_order_details_order_id ON order_details(order_id);
        CREATE INDEX IF NOT EXISTS idx_order_details_product_id ON order_details(product_id);
    END IF;

    IF to_regclass('contacts') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_contacts_is_read ON contacts(is_read);
    END IF;
END $$;

DROP FUNCTION IF EXISTS _jenka_rename_column_if_exists(text, text, text);
DROP FUNCTION IF EXISTS _jenka_rename_table_if_exists(text, text);
