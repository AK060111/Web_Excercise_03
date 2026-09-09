USE [ServletCRUDMVC];
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
-- Existing accounts stay active. New registrations explicitly insert active = 0.
IF COL_LENGTH(N'dbo.User', N'active') IS NULL
    ALTER TABLE dbo.[User] ADD active BIT NOT NULL
        CONSTRAINT DF_User_active DEFAULT (1) WITH VALUES;
IF COL_LENGTH(N'dbo.User', N'otp') IS NULL
    ALTER TABLE dbo.[User] ADD otp VARCHAR(64) NULL;
IF COL_LENGTH(N'dbo.User', N'otpExpiry') IS NULL
    ALTER TABLE dbo.[User] ADD otpExpiry DATETIME2 NULL;
IF COL_LENGTH(N'dbo.User', N'otpAttempts') IS NULL
    ALTER TABLE dbo.[User] ADD otpAttempts INT NOT NULL
        CONSTRAINT DF_User_otpAttempts DEFAULT (0) WITH VALUES;
COMMIT TRANSACTION;
GO
-- otp stores a SHA-256 digest; otpExpiry uses UTC.
