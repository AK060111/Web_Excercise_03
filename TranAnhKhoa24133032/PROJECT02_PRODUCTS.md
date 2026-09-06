# Project 02 — Products

> Ghi chú trước khi clone: đây là tài liệu của giai đoạn triển khai. Tác giả đã kiểm thử các flow trên Tomcat sau đó; các dòng ghi "chưa test"/"chưa áp dụng migration" bên dưới là trạng thái tại thời điểm viết. Dùng README.md và database/README.md làm hướng dẫn setup hiện tại. SMTP đang dùng Resend qua MAIL_*; không điền credentials thật vào properties để commit.

## Kết quả và giới hạn kiểm thử

- mvn clean package: BUILD SUCCESS.
- CheckProduct.java --mapping: 27 checks PASS.
- 25 checks dùng EntityManager giả lập: CRUD, validate, pagination, newest, quyền admin.
- 2 checks dùng Hibernate thật: metamodel Product–Category và kiểm tra JPQL, không execute query Product.
- Chưa chạy migration hoặc CRUD Product trên SQL Server vì products chưa tồn tại.
- Chưa chạy luồng JSP/upload trên Tomcat. Không tự thay đổi Tomcat hoặc publish.
- Không sửa các flow tài khoản, MailService, SMTP/database credentials, dependency hoặc web.xml.

## File tạo

- database/project02_products.sql
- src/main/java/vn/iotstar/model/Product.java
- src/main/java/vn/iotstar/dao/ProductDao.java
- src/main/java/vn/iotstar/dao/impl/ProductDaoImpl.java
- src/main/java/vn/iotstar/service/ProductService.java
- src/main/java/vn/iotstar/service/impl/ProductServiceImpl.java
- src/main/java/vn/iotstar/controller/ProductController.java
- src/main/java/vn/iotstar/controller/ProductAdminController.java
- src/main/java/vn/iotstar/util/ImageUpload.java
- src/main/webapp/views/includes/product-cards.jsp
- src/main/webapp/WEB-INF/views/product-list.jsp
- src/main/webapp/WEB-INF/views/product-detail.jsp
- src/main/webapp/WEB-INF/views/admin/product-list.jsp
- src/main/webapp/WEB-INF/views/admin/product-form.jsp
- src/main/webapp/WEB-INF/views/admin/add-product.jsp
- src/main/webapp/WEB-INF/views/admin/edit-product.jsp
- scripts/CheckProduct.java
- PROJECT02_PRODUCTS.md

## File sửa

- src/main/resources/META-INF/persistence.xml: đăng ký Product, giữ schema-generation=none.
- src/main/java/vn/iotstar/controller/HomeController.java: query tối đa 10 mới nhất.
- src/main/java/vn/iotstar/controller/CategoryAddController.java: delegate upload sang ImageUpload, giữ GIF cho Category.
- src/main/java/vn/iotstar/controller/CategoryDeleteController.java: lỗi 409 thân thiện khi Category bị FK chặn xóa.
- src/main/java/vn/iotstar/filter/AuthenticationFilter.java: roleid=1 cho /admin/product và /admin/product/*.
- src/main/webapp/views/home.jsp: section sản phẩm mới nhất, giữ lời chào khi đã login.
- src/main/webapp/views/includes/header.jsp: link sản phẩm công khai và menu quản lý cho admin.
- src/main/webapp/views/includes/style.jsp: grid/card/form Product và responsive.

## SQL: người dùng tự chạy

Mở database/project02_products.sql trong SSMS, chạy trước khi publish bản mới.
Không có migration được chạy tự động trong lần triển khai này.

Bảng dbo.products:

| Cột | Kiểu / quy tắc |
|---|---|
| product_id | INT IDENTITY, primary key |
| product_name | NVARCHAR(255), NOT NULL |
| description | NVARCHAR(MAX), NULL |
| price | DECIMAL(18,2), NOT NULL, CHECK >= 0 |
| image | NVARCHAR(255), NULL |
| category_id | INT NOT NULL, FK dbo.Category(cate_id) |
| created_date | DATETIME2 NOT NULL, default SYSUTCDATETIME() |

Script dùng IF OBJECT_ID để chỉ tạo khi chưa có bảng; có index theo ngày/id và category.
Không DROP/DELETE, không thay schema/dữ liệu Category. Chạy lại không tạo trùng bảng.
Nếu đã có products từ nguồn khác, script giữ nguyên bảng đó; cần kiểm tra schema có khớp.
FK không cascade: muốn xóa Category có Product, chuyển hoặc xóa Product trước bằng UI.

Product có @ManyToOne LAZY, @JoinColumn category_id → cate_id. Query đọc dùng JOIN FETCH
để JSP đọc category sau khi đóng EntityManager. Không thêm List<Product> vào Category.
@PrePersist tự set createdDate UTC; update giữ nguyên createdDate. Hiển thị thời gian có nhãn UTC.

## URL (context /ServletCRUDMVC)

- /ServletCRUDMVC/admin/product/list — danh sách admin (alias /admin/product).
- /ServletCRUDMVC/admin/product/add — thêm.
- /ServletCRUDMVC/admin/product/edit?id=ID — sửa.
- /ServletCRUDMVC/admin/product/delete — POST từ nút Xóa, GET trả 405.
- /ServletCRUDMVC/product?page=1 — public, tối đa 6 mỗi trang.
- /ServletCRUDMVC/product/detail?id=ID — chi tiết public.
- /ServletCRUDMVC/home — tối đa 10 mới nhất.

ID âm/sai định dạng trả 400, không tồn tại trả 404. page sai/âm về 1; quá tổng trang về trang cuối.
Ngày bằng nhau dùng id DESC để thứ tự ổn định. Không load tất cả rồi subList ở trang public.
CRUD Product cần session account roleid=1, POST có CSRF token; tài khoản thường nhận 403.

## Upload

Dùng app.upload.dir / APP_UPLOAD_DIR hiện có, lưu trong thư mục product bên dưới root đó.
Product nhận JPG/JPEG/PNG/WEBP tối đa 5 MB, tên UUID, không dùng đường dẫn do người dùng cung cấp.
Ảnh hiển thị bằng /image?fname=product/.... Không chọn file mới khi edit sẽ giữ ảnh cũ.
Các file ảnh cũ sau edit/delete được giữ lại trên disk; không tự xóa file có thể còn được sử dụng.
File mới vừa upload được dọn nếu lưu DB thất bại. Category vẫn nhận cả GIF như trước.
Validation upload theo extension và kích thước; không thêm thư viện xử lý ảnh.

## Publish STS/Tomcat

1. Chạy migration trong SSMS trên ServletCRUDMVC.
2. Trong STS: Refresh project; Maven > Update Project nếu workspace chưa thấy source mới.
3. Chạy mvn clean package.
4. Publish project vào Tomcat 11 hiện có và reload/restart ứng dụng bằng cách đang dùng.
5. Giữ context ServletCRUDMVC, DB/SMTP environment và APP_UPLOAD_DIR hiện tại.

Không có dependency mới. Không chạy run-local.ps1 để thay cấu hình Tomcat hiện tại.

## Checklist test thủ công

### A. Database

- Chạy migration, kiểm tra products và FK tới Category(cate_id).
- Chạy lại migration: không lỗi/tạo trùng, dữ liệu giữ nguyên.
- Có ít nhất một Category thật để chọn; không có danh mục thì form thông báo cần tạo trước.

### B. Admin CRUD

- Admin roleid=1 thêm Product hợp lệ, tên tiếng Việt, mô tả, giá 0 hoặc số dương, Category.
- Thử tên rỗng, giá âm/quá 2 số lẻ, Category ID không tồn tại: bị từ chối.
- Xem danh sách đủ ID/tên/giá/Category/ngày/ảnh.
- Edit: dropdown chọn đúng Category; sửa giá/tên/Category nhưng giữ ngày tạo.
- Edit không upload ảnh mới: ảnh cũ còn nguyên; upload mới: hiển thị ảnh mới.
- Thử file .txt/.svg và ảnh quá 5 MB: từ chối. Thử PNG/JPG/WEBP: hiển thị được.
- Delete bằng nút: xóa đúng Product. Delete ID không tồn tại: 404; GET delete: 405.
- Chưa login: chuyển login; user role khác 1: 403 khi truy cập hoặc POST CRUD.
- Xóa Category có Product: bị chặn với thông báo 409, dữ liệu Product không bị cascade delete.

### C. Public

- Mở /product không login: xem được. Database chưa có Product: thông báo trống.
- Tạo ít nhất 13 Product bằng UI: các trang lần lượt tối đa 6,6,1.
- Thử page=abc, -1, 0, số rất lớn, vượt tổng số trang: không crash.
- Chuyển trang và click detail: đúng ID/tên/giá/mô tả/Category/ngày/ảnh.
- Detail ID sai định dạng/âm: 400; ID không có: 404.
- Tên/mô tả có ký tự HTML được hiển thị như text.

### D. Home

- /home xem được khi chưa login.
- Với hơn 10 Product: đúng tối đa 10, createdDate DESC rồi id DESC.
- Click card mở đúng detail. Chưa có Product: thông báo trống.
- Khi login vẫn có lời chào và header tên tài khoản/Logout.

### E. Regression

- Login và Logout hoạt động như trước.
- Register + activation OTP qua Resend hoạt động.
- Forgot Password OTP + reset + login bằng mật khẩu mới hoạt động.
- Category list/add/edit/upload GIF hoạt động; xóa Category chưa có Product vẫn được.

## Bộ kiểm tra

Sau build, từ thư mục project:

    java -cp "target/classes;target/ServletCRUDMVC/WEB-INF/lib/*;.m2/repository/jakarta/servlet/jakarta.servlet-api/6.1.0/jakarta.servlet-api-6.1.0.jar" scripts/CheckProduct.java --mapping

Chế độ --mapping tạo EntityManager để kiểm tra metamodel/JPQL, không execute CRUD Product.
Bỏ --mapping để chỉ chạy 25 checks không kết nối DB.

SAU KHI TỰ CHẠY MIGRATION và đã có một Category, có thể chạy cùng lệnh với --database.
Chế độ này đọc Category có sẵn, thêm 13 Product thử trong transaction, test CRUD/pagination/newest
rồi rollback toàn bộ. Chỉ xóa Product vừa tạo trong transaction thử, không xóa dữ liệu có sẵn.
Không chạy migration. Identity SQL Server có thể có khoảng trống sau rollback.
