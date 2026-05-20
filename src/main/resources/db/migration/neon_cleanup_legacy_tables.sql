-- Neon PostgreSQL cleanup script
-- Muc tieu:
-- 1. Audit cac bang/cot khong theo snake_case
-- 2. Xoa cac bang legacy/duplicate theo thu tu uu tien an toan
-- 3. Khong dung CASCADE de tranh xoa lan du lieu ngoai y muon

-- =========================================================
-- PHAN 1: AUDIT TEN BANG/COT KHONG CHUAN
-- =========================================================

-- Bang khong theo snake_case trong schema public
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name !~ '^[a-z0-9]+(_[a-z0-9]+)*$'
ORDER BY table_name;

-- Cot khong theo snake_case trong schema public
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND column_name !~ '^[a-z0-9]+(_[a-z0-9]+)*$'
ORDER BY table_name, ordinal_position;

-- Kiem tra ton tai cac bang legacy/nghi van can don
SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
                    'News',
                    'banner',
                    'orderdetails',
                    'pointhistory',
                    'productimages',
                    'voucherusage',
                    'vouchers',
                    'servicebooking',
                    'serverbooking'
    )
ORDER BY tablename;

-- Dem nhanh so dong de ban review truoc khi xoa
SELECT 'public."News"' AS table_name, count(*) AS row_count FROM public."News"
UNION ALL
SELECT 'public.banner', count(*) FROM public.banner
UNION ALL
SELECT 'public.orderdetails', count(*) FROM public.orderdetails
UNION ALL
SELECT 'public.pointhistory', count(*) FROM public.pointhistory
UNION ALL
SELECT 'public.productimages', count(*) FROM public.productimages
UNION ALL
SELECT 'public.voucherusage', count(*) FROM public.voucherusage
UNION ALL
SELECT 'public.vouchers', count(*) FROM public.vouchers;

-- Phu thuoc FK hien tai cua cac bang legacy
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    tc.constraint_name
FROM information_schema.table_constraints AS tc
         JOIN information_schema.key_column_usage AS kcu
              ON tc.constraint_name = kcu.constraint_name
                  AND tc.table_schema = kcu.table_schema
         JOIN information_schema.constraint_column_usage AS ccu
              ON ccu.constraint_name = tc.constraint_name
                  AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
  AND tc.table_name IN ('orderdetails', 'pointhistory', 'voucherusage')
ORDER BY tc.table_name, kcu.column_name;

-- =========================================================
-- PHAN 2: XOA BANG LEGACY/DUPLICATE
-- Thu tu uu tien:
-- 1. Bang khong co FK ra bang khac: banner, productimages, "News"
-- 2. Bang phu thuoc 1 FK: voucherusage -> vouchers
-- 3. Bang goc cua voucher usage: vouchers
-- 4. Bang legacy con FK ve orders/accounts/products: orderdetails, pointhistory
-- Luu y: Khong xoa banner_set/banner_image vi schema chuan hien tai van dung.
-- =========================================================

BEGIN;

DROP TABLE IF EXISTS public.banner;
DROP TABLE IF EXISTS public.productimages;
DROP TABLE IF EXISTS public."News";

DROP TABLE IF EXISTS public.voucherusage;
DROP TABLE IF EXISTS public.vouchers;

DROP TABLE IF EXISTS public.orderdetails;
DROP TABLE IF EXISTS public.pointhistory;

-- Neu ton tai cac bang typo ma dump repo chua thay, script van xu ly duoc
DROP TABLE IF EXISTS public.servicebooking;
DROP TABLE IF EXISTS public.serverbooking;

COMMIT;