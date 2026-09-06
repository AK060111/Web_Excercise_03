USE [ServletCRUDMVC];
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.Category', N'U') IS NULL
    THROW 50001, N'Chưa có bảng Category. Kiểm tra database trước khi chạy.', 1;
IF OBJECT_ID(N'dbo.products', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.products (
        product_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_products PRIMARY KEY,
        product_name NVARCHAR(255) NOT NULL,
        description NVARCHAR(MAX) NULL,
        price DECIMAL(18,2) NOT NULL CONSTRAINT CK_products_price CHECK (price >= 0),
        image NVARCHAR(255) NULL,
        category_id INT NOT NULL,
        created_date DATETIME2 NOT NULL CONSTRAINT DF_products_created_date DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_products_Category FOREIGN KEY (category_id) REFERENCES dbo.Category(cate_id)
    );
    CREATE INDEX IX_products_newest ON dbo.products(created_date DESC, product_id DESC);
    CREATE INDEX IX_products_category ON dbo.products(category_id);
END;
COMMIT TRANSACTION;
GO
-- Không xóa/sửa dữ liệu cũ. Nếu products đã tồn tại, kiểm tra schema trước khi sử dụng.
