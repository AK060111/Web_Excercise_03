# Thiết lập database

Không có script nào tự chạy khi build/start ứng dụng. Chạy thủ công trong SSMS:

1. `ServletCRUDMVC.sql`: tạo database nếu chưa có, tạo `dbo.[User]` và `dbo.Category` nếu thiếu.
   Script không tạo SQL login, không có mật khẩu hoặc tài khoản mẫu.
2. `project02_otp_update.sql`: thêm active/otp/otpExpiry/otpAttempts nếu thiếu; giữ User cũ active.
3. `project02_products.sql`: tạo products, CHECK price >= 0 và FK category_id -> Category(cate_id).

Các script không DROP/DELETE/TRUNCATE và không sửa mật khẩu tài khoản đang có.
Script kiểm tra tồn tại không thay thế việc đối chiếu schema nếu database có bảng khác cấu trúc.

Quản trị viên cấu hình SQL Authentication/Mixed Mode, SQL login riêng và database user tương ứng
bằng SSMS; cấp quyền đọc/ghi các bảng cần thiết. Không cần cấp db_owner cho ứng dụng chạy thường ngày.
Đặt DB_USER/DB_PASSWORD trong môi trường Tomcat. Không ghi thông tin này vào SQL/source.

Tạo tài khoản ứng dụng bằng trang đăng ký, kích hoạt email. Để demo quyền admin, quản trị viên
kiểm tra đúng tài khoản vừa tạo và tự đặt roleid=1 bằng SSMS. Các tài khoản đăng ký mặc định roleid=5.
Không có mật khẩu admin mặc định trong repo.

Dữ liệu demo: tự tạo Category và ít nhất 13 Product bằng UI để kiểm tra các trang 6/6/1,
Home hiển thị tối đa 10. Không giả định dữ liệu trên máy tác giả tồn tại ở máy clone.
Không kèm dữ liệu tài khoản/email thật hoặc bản backup database.
