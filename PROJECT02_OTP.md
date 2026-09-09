# Project 02 - OTP kích hoạt tài khoản

> Ghi chú trước khi clone: đây là tài liệu của giai đoạn triển khai. Tác giả đã kiểm thử các flow trên Tomcat sau đó; các dòng ghi "chưa test"/"chưa áp dụng migration" bên dưới là trạng thái tại thời điểm viết. Dùng README.md và database/README.md làm hướng dẫn setup hiện tại. SMTP đang dùng Resend qua MAIL_*; không điền credentials thật vào properties để commit.

## Kết quả và phạm vi

Đăng ký tạo User inactive; OTP 6 số từ SecureRandom, hạn 5 phút theo giờ UTC của SQL Server.
Database lưu SHA-256 của username + OTP. Xác nhận đúng kích hoạt và xóa OTP/expiry.
Tối đa 5 lần sai; gửi lại sau ít nhất 60 giây, reset số lần sai.
Login bằng mật khẩu và login qua cookie đều kiểm tra active. Logout không thay đổi.
Chưa làm Forgot Password hoặc Product. Giữ nguyên mật khẩu tài khoản hiện có.

## File tạo

- database/project02_otp_update.sql
- src/main/java/vn/iotstar/service/MailService.java
- src/main/java/vn/iotstar/controller/VerifyOtpController.java
- src/main/webapp/views/verify-otp.jsp
- scripts/CheckActivation.java
- PROJECT02_OTP.md

## File sửa

- pom.xml
- src/main/resources/application.properties
- src/main/java/vn/iotstar/model/User.java
- src/main/java/vn/iotstar/dao/UserDao.java
- src/main/java/vn/iotstar/dao/impl/UserDaoImpl.java
- src/main/java/vn/iotstar/service/UserService.java
- src/main/java/vn/iotstar/service/impl/UserServiceImpl.java
- src/main/java/vn/iotstar/controller/RegisterController.java
- src/main/java/vn/iotstar/controller/LoginController.java
- src/main/webapp/views/register.jsp
- src/main/webapp/views/login.jsp

## Trước khi publish trong STS

1. Chạy database/project02_otp_update.sql trong SSMS trên database ServletCRUDMVC.
   Script thêm active BIT, otp VARCHAR(64), otpExpiry DATETIME2, otpAttempts INT nếu chưa tồn tại.
   User cũ được active=1 khi thêm cột; đăng ký mới INSERT active=0 rõ ràng.
   Chạy lại không reset dữ liệu hoặc kích hoạt các User đang inactive.
   Migration chưa được áp dụng chính thức: lần kiểm thử được rollback.
2. Điền cấu hình SMTP bằng biến môi trường của tiến trình Tomcat (khuyến nghị):
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=<địa chỉ gửi thư>
   MAIL_PASSWORD=<App Password>
   Hoặc dùng mail.host, mail.port, mail.username, mail.password trong application.properties.
   Không đưa mật khẩu thật vào source hoặc log. Nếu dùng properties, cần build/publish lại.
   Nếu dùng environment, đặt trong Run Configuration của Tomcat/STS và restart tiến trình.
   Gmail: bật xác minh hai bước và tạo App Password theo hướng dẫn của Google.
   Implementation này sử dụng SMTP STARTTLS (587), không phải implicit SSL cổng 465.
3. Chạy mvn clean package, sau đó publish project trong STS và restart/reload ứng dụng.

Dependency thêm: org.eclipse.angus:jakarta.mail:2.0.4 (Jakarta Mail API và implementation).
Không đổi dependency hiện có, không sửa web.xml hoặc cấu hình Tomcat.

## URL

Với context mặc định /ServletCRUDMVC và cổng Tomcat 8080:

- http://localhost:8080/ServletCRUDMVC/register
- http://localhost:8080/ServletCRUDMVC/verify-otp
- http://localhost:8080/ServletCRUDMVC/login
- http://localhost:8080/ServletCRUDMVC/logout

Điều chỉnh cổng/context theo cấu hình Tomcat hiện tại nếu khác.

## Test thủ công trên Tomcat

1. Đăng nhập tài khoản cũ sau migration: vẫn thành công; logout vẫn hoạt động.
2. Đăng ký username/email mới, dùng số điện thoại chưa tồn tại:
   chuyển /verify-otp, nhận email, database active=0, OTP là digest, expiry có giá trị.
   Không đưa giá trị OTP, password hoặc địa chỉ email vào ảnh/log kiểm thử.
3. Trước kích hoạt, đăng nhập đúng username/password:
   báo chưa kích hoạt, có link Nhập OTP / Gửi lại OTP; không cấp session account.
4. Nhập 6 số sai: hiện lỗi; tài khoản vẫn inactive.
5. Nhập đúng mã email trong 5 phút: redirect /login?activated=1,
   active=1, otp và otpExpiry NULL; đăng nhập thành công.
6. Với tài khoản mới khác, đợi quá 5 phút rồi nhập mã: báo hết hạn, không kích hoạt.
7. Gửi lại ngay trong 60 giây: báo chờ. Sau 60 giây: email mới,
   expiry mới, attempts=0; mã mới khác mã hiện tại, mã trước không hợp lệ; mã mới kích hoạt thành công.
8. Nhập sai 5 lần: mã đúng cũng bị chặn cho tới khi gửi mã mới.
9. Gửi lại hoặc refresh sau kích hoạt: không tạo lại OTP cho tài khoản active.
10. Bỏ trống/sai cấu hình SMTP: đăng ký vẫn tạo User inactive, trang OTP báo lỗi chung;
    sửa cấu hình và restart ứng dụng, đăng nhập đúng thông tin để mở lại trang OTP rồi gửi lại.
11. Session hết hạn hoặc mở /verify-otp trên trình duyệt mới: chuyển login;
    login đúng mật khẩu của tài khoản inactive để tiếp tục xác nhận.
12. Đăng ký trùng username/email: báo trùng; không tạo tài khoản khác.

Lưu ý schema cũ có UNIQUE(phone): nhiều tài khoản dùng cùng giá trị phone rỗng có thể
bị từ chối. Giai đoạn này giữ nguyên constraint; dùng số điện thoại riêng khi kiểm thử.

## Kiểm tra tự động đã chạy

mvn clean package: BUILD SUCCESS. Maven chưa có bộ JUnit; test riêng được chạy bằng:

    java -cp "target/classes;target/ServletCRUDMVC/WEB-INF/lib/*" scripts/CheckActivation.java

Script dùng JDBC thật, migration và dữ liệu thử trong một transaction rồi rollback.
MailService được thay bằng fake, không gửi email; không in OTP/password/email.
22 kiểm tra đã pass, bao gồm migration chạy lại, inactive login, mã sai/hết hạn,
resend/cooldown, giới hạn 5 lần, lỗi SMTP giả lập, kích hoạt, xóa OTP, login sau kích hoạt.
Chạy script trên môi trường phát triển, tránh lúc ứng dụng đang có request vì DDL giữ lock.
Chưa kiểm thử email SMTP thật và JSP flow trực tiếp trên Tomcat cho phiên bản này.

