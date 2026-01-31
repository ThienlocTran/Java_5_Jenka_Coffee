-- Migration: Add activation and password reset fields to Accounts table
-- Date: 2026-01-31

-- Add activation token fields
ALTER TABLE Accounts ADD ActivationToken NVARCHAR(100);
ALTER TABLE Accounts ADD ActivationTokenExpiry DATETIME;

-- Add password reset token fields
ALTER TABLE Accounts ADD ResetToken NVARCHAR(100);
ALTER TABLE Accounts ADD ResetTokenExpiry DATETIME;

-- Add activation method field
ALTER TABLE Accounts ADD ActivationMethod NVARCHAR(10) DEFAULT 'EMAIL';

-- Create index for token lookups (performance optimization)
CREATE INDEX IDX_Accounts_ActivationToken ON Accounts(ActivationToken);
CREATE INDEX IDX_Accounts_ResetToken ON Accounts(ResetToken);
