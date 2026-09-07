# Profile User — 07/09/2026

## Phạm vi

GET/POST /ServletCRUDMVC/profile, chỉ người đã đăng nhập và còn active.
JPA/Hibernate đọc User từ dbo.[User] theo ID session account. POST chỉ nhận fullname, phone, avatar;
không dùng ID, username, email, role, password hoặc trạng thái OTP từ form.

User hiện là @Entity cùng các getter/setter/constructor cũ, dùng chung JPAConfig với Category/Product.
UserProfileDaoImpl dùng JPQL UPDATE chỉ fullname/phone/avatar; không merge User lấy từ session.
Các trường bảo vệ được map updatable=false. Login/Register/OTP/Forgot Password vẫn dùng JDBC cũ.
Điều này giữ API và behavior hiện có, đồng thời Profile đọc/ghi hoàn toàn qua JPA.

Không thay đổi SQL/schema, không migration. Cột avatar đã có sẵn NVARCHAR(255).
Phone có UNIQUE theo schema hiện tại: không dùng số của tài khoản khác; giá trị trống/null cũng
chịu ràng buộc UNIQUE SQL Server. Nếu để trống bị từ chối, nhập số điện thoại riêng.

## File tạo

- src/main/java/vn/iotstar/dao/UserProfileDao.java
- src/main/java/vn/iotstar/dao/impl/UserProfileDaoImpl.java
- src/main/java/vn/iotstar/service/UserProfileService.java
- src/main/java/vn/iotstar/service/impl/UserProfileServiceImpl.java
- src/main/java/vn/iotstar/controller/ProfileController.java
- src/main/webapp/WEB-INF/views/profile.jsp
- scripts/CheckProfile.java
- PROFILE_USER.md

## File sửa

- src/main/java/vn/iotstar/model/User.java: mapping JPA, giữ API hiện tại.
- src/main/resources/META-INF/persistence.xml: đăng ký User, giữ schema generation none.
- src/main/java/vn/iotstar/util/ImageUpload.java: dùng chung đường lưu/cleanup; thêm nhánh avatar.
- src/main/webapp/views/includes/header.jsp: link Hồ sơ khi đã đăng nhập.
- README.md: cập nhật Profile và hướng dẫn triển khai độc lập.

Không xóa file. Không thêm dependency hoặc thay MailService/credentials/Tomcat/web.xml.

## Avatar

Lưu tại <APP_UPLOAD_DIR>/avatar/<timestamp>-<UUID>.<extension>.
Database chỉ lưu avatar/<filename>. Hiển thị bằng /image?fname=avatar/....
Không chọn ảnh mới: giữ ảnh cũ. Ảnh cũ không bị tự xóa; ảnh mới chưa lưu DB được dọn khi lỗi.
Chấp nhận JPG/JPEG/PNG/GIF, tối đa 5 MB, kích thước tối đa 4096 x 4096 pixel.
Kiểm tra nội dung ảnh bằng ImageIO của Java SE trước khi lưu; không chỉ kiểm tra tên/MIME client.
Product/Category vẫn dùng chung ImageUpload với loại file hiện có (Product gồm WEBP, Category có GIF).

Để ảnh bền qua redeploy/restart, đặt APP_UPLOAD_DIR ở một thư mục có quyền ghi, ngoài thư mục
webapps/ServletCRUDMVC và target. Đường dẫn do từng máy cấu hình trong environment, không commit.
Giữ nguyên cùng thư mục khi restart/deploy. Nếu dùng mặc định upload tương đối, phải giữ nguyên
working directory của JVM. Không chép ảnh vào WAR.

## Maven và Tomcat độc lập, không cần STS

1. Cài JDK phù hợp (target Java 17), Maven và Tomcat 11 bên ngoài project theo cách của bạn.
2. Clone mới: copy application.example.properties thành application.properties nếu chưa có.
3. Cấu hình DB_URL, DB_USER, DB_PASSWORD, APP_UPLOAD_DIR cho tiến trình Tomcat.
   Giữ MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD đang hoạt động; hiện tác giả dùng Gmail,
   sender lấy từ username cấu hình. Không thay lại sender Resend hoặc đưa credentials vào repo.
4. Tại project: mvn clean package. Kết quả target/ServletCRUDMVC.war.
5. Copy WAR vào <TOMCAT_HOME>/webapps/ServletCRUDMVC.war bằng quy trình deploy của bạn.
6. Khởi động Tomcat bằng bin/catalina.bat run (Windows) hoặc bin/catalina.sh run (Linux).
7. Mở http://localhost:8080/ServletCRUDMVC/login, thay cổng nếu cấu hình khác.

Không có mvnw.cmd trong project nên dùng Maven đã cài. Không tải/đóng gói Tomcat bằng project.
Servlet/JSP API vẫn provided, Jakarta, không chuyển sang javax Servlet.
javax.imageio là API ảnh có sẵn của Java SE, không phải dependency Java EE cũ.

## Kiểm thử đã chạy

- mvn clean package: BUILD SUCCESS.
- CheckProfile: 21 kiểm tra JPA/SQL Server/controller/upload PASS.
- CheckForgotPassword: 39 kiểm tra activation/login/reset PASS, mail giả lập.
- CheckProduct: 25 kiểm tra giả lập/validation/query/auth PASS.

CheckProfile tạo User thử trong transaction, thao tác qua JPA, đọc lại qua JDBC giống login;
rollback toàn bộ cuối test. Không sửa tài khoản có sẵn; identity có thể có khoảng trống sau rollback.
Ảnh thử nằm trong target/profile-test-upload, file thử được dọn. Không chạy migration/gửi mail.

Sau build, chạy tùy chọn trong project phát triển:

    java -cp "target/classes;target/ServletCRUDMVC/WEB-INF/lib/*;.m2/repository/jakarta/servlet/jakarta.servlet-api/6.1.0/jakarta.servlet-api-6.1.0.jar" scripts/CheckProfile.java

Nếu dùng cache Maven chuẩn, thay phần đường dẫn Servlet API bằng jar trong cache của máy;
hoặc chạy build.ps1 để chuẩn bị cache riêng của project như hướng dẫn README.
Test tự động không thay thế chạy Servlet/JSP trên Tomcat và kiểm tra restart/deploy thực tế.

## Checklist thủ công

1. Login; mở /profile bằng link Hồ sơ.
2. Username/email khớp tài khoản đăng nhập; không có trường chỉnh sửa các giá trị này.
3. Đổi fullname (thử tiếng Việt), phone hợp lệ riêng; upload PNG/JPG/GIF và Save.
4. Redirect về /profile, có thông báo thành công, header hiển thị tên mới.
5. Refresh: dữ liệu/avatar vẫn đúng.
6. Save lần nữa không chọn file: avatar cũ còn nguyên.
7. Logout rồi login lại: fullname/phone/avatar vẫn đúng trên Profile.
8. Restart Tomcat và deploy lại WAR, giữ APP_UPLOAD_DIR: avatar vẫn hiển thị.
9. Thử tên rỗng, phone quá dài/trùng, file giả ảnh, file quá 5 MB: báo lỗi; dữ liệu/ảnh cũ còn.
10. Thử sửa request thêm id của user khác, email/password/roleid/active: không cập nhật các trường này.
11. Không login, GET và POST /profile phải về login; POST thiếu/sai CSRF bị chặn.
12. Product upload, Category upload, Home newest, phân trang và Product Detail vẫn hoạt động.
13. Register/activation OTP, Login/Logout, Forgot Password/reset OTP vẫn hoạt động với SMTP hiện tại.

Chưa kiểm thử Profile JSP và restart/deploy trực tiếp trên Tomcat trong lần sửa này.
