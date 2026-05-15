-- ============================================================
-- JENKA COFFEE - Ensure loyalty points schema works with Hibernate
--
-- Run on the SAME database/schema Spring Boot uses if checkout fails
-- while inserting point history.
-- ============================================================

SELECT current_database() AS database_name, current_schema() AS schema_name;

CREATE SEQUENCE IF NOT EXISTS pointhistory_id_seq;

DO $$
BEGIN
    IF to_regclass('public.pointhistory') IS NULL
       AND to_regclass('public."PointHistory"') IS NOT NULL THEN
        ALTER TABLE public."PointHistory" RENAME TO pointhistory;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.pointhistory (
    id         BIGINT      NOT NULL DEFAULT nextval('pointhistory_id_seq'),
    username   VARCHAR(50) NOT NULL,
    amount     INTEGER     NOT NULL,
    orderid    BIGINT,
    reason     VARCHAR(255) NOT NULL,
    createdate TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT pointhistory_pkey PRIMARY KEY (id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'pointhistory'
          AND column_name = 'OrderId'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'pointhistory'
          AND column_name = 'orderid'
    ) THEN
        ALTER TABLE public.pointhistory RENAME COLUMN "OrderId" TO orderid;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'pointhistory'
          AND column_name = 'createDate'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'pointhistory'
          AND column_name = 'createdate'
    ) THEN
        ALTER TABLE public.pointhistory RENAME COLUMN "createDate" TO createdate;
    END IF;
END $$;

ALTER TABLE public.pointhistory
    ADD COLUMN IF NOT EXISTS username VARCHAR(50),
    ADD COLUMN IF NOT EXISTS amount INTEGER,
    ADD COLUMN IF NOT EXISTS orderid BIGINT,
    ADD COLUMN IF NOT EXISTS reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS createdate TIMESTAMP DEFAULT NOW();

ALTER TABLE public.pointhistory
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN amount SET NOT NULL,
    ALTER COLUMN reason SET NOT NULL,
    ALTER COLUMN createdate SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pointhistory_username ON public.pointhistory (username);
CREATE INDEX IF NOT EXISTS idx_pointhistory_order ON public.pointhistory (orderid);
CREATE INDEX IF NOT EXISTS idx_pointhistory_date ON public.pointhistory (createdate DESC);

ALTER SEQUENCE pointhistory_id_seq OWNED BY public.pointhistory.id;
