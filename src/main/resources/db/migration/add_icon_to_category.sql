-- Add icon column to Categories table (SQL Server syntax)
ALTER TABLE Categories ADD Icon VARCHAR(255);

-- Update existing categories with their icons
UPDATE Categories SET Icon = 'ca_phe_do_an.webp' WHERE Id = 'CF_AN_VAT';
UPDATE Categories SET Icon = 'dung_cu_pha_che.webp' WHERE Id = 'DUNG_CU';
UPDATE Categories SET Icon = 'may_pha_may_xay_cu.webp' WHERE Id = 'HANG_CU';
UPDATE Categories SET Icon = 'May_Pha_Ca_Phe.webp' WHERE Id = 'MAY_PHA';
UPDATE Categories SET Icon = 'May_Xay_Ca_Phe.webp' WHERE Id = 'MAY_XAY';
UPDATE Categories SET Icon = 'may_xay_sinh_to_may_ep.webp' WHERE Id = 'XAY_EP';

-- Optional: Make icon required
-- ALTER TABLE Categories ALTER COLUMN Icon VARCHAR(255) NOT NULL;
