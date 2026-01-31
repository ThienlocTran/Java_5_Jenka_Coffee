-- Fix: Activate all existing accounts (run this after migration_activation_reset.sql)
-- This is needed for existing accounts created before activation feature was added

-- Set all existing accounts to activated
UPDATE Accounts 
SET Activated = 1 
WHERE Activated IS NULL OR Activated = 0;

UPDATE Accounts SET email = 'tranthienloc21102005@gmail.com' WHERE username = 'admin'

-- Optional: Clear any old activation tokens
UPDATE Accounts
SET ActivationToken = NULL,
    ActivationTokenExpiry = NULL
WHERE Activated = 1;

-- Verify the update
SELECT Username, Fullname, Email, Activated, ActivationMethod 
FROM Accounts;
