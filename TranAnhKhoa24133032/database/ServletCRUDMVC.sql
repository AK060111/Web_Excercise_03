-- Run manually in SSMS with an account permitted to create the database/tables.
-- SQL logins/users and permissions must be configured separately by the administrator.
USE master;
GO
IF DB_ID(N'ServletCRUDMVC') IS NULL CREATE DATABASE ServletCRUDMVC;
GO
USE ServletCRUDMVC;
GO
IF OBJECT_ID(N'dbo.Category', N'U') IS NULL
CREATE TABLE dbo.Category (
    cate_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cate_name NVARCHAR(255) NOT NULL,
    icons NVARCHAR(255) NULL
);
GO
IF OBJECT_ID(N'dbo.[User]', N'U') IS NULL
CREATE TABLE dbo.[User] (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    email NVARCHAR(255) NOT NULL UNIQUE,
    username NVARCHAR(100) NOT NULL UNIQUE,
    fullname NVARCHAR(255) NOT NULL,
    password NVARCHAR(255) NOT NULL,
    avatar NVARCHAR(255) NULL,
    roleid INT NOT NULL DEFAULT 5,
    phone NVARCHAR(30) NULL UNIQUE,
    createdDate DATE NOT NULL
);
GO
