# Project 02 — Quên mật khẩu bằng OTP

> Ghi chú trước khi clone: đây là tài liệu của giai đoạn triển khai. Tác giả đã kiểm thử các flow trên Tomcat sau đó; các dòng ghi "chưa test"/"chưa áp dụng migration" bên dưới là trạng thái tại thời điểm viết. Dùng README.md và database/README.md làm hướng dẫn setup hiện tại. SMTP đang dùng Resend qua MAIL_*; không điền credentials thật vào properties để commit.

## Phạm vi và kết quả

- mvn clean package: BUILD SUCCESS.
- scripts/CheckForgotPassword.java: 39 kiểm tra JDBC/controller PASS.
- Test dùng SQL Server thật, dữ liệu mới trong transaction rồi rollback; MailService giả lập.
- Không chạy migration, không DROP/DELETE, không thay đổi tài khoản thật.
- Chưa kiểm thử JSP trên Tomcat hoặc gửi thư Resend thật cho flow mới.
- Không đổi Tomcat/context path, web.xml, dependency, SMTP credentials hoặc triển khai Products.

## File tạo

- src/main/java/vn/iotstar/controller/ForgotPasswordController.java
- src/main/webapp/WEB-INF/views/forgot-password.jsp
- src/main/webapp/WEB-INF/views/forgot-password-verify.jsp
- src/main/webapp/WEB-INF/views/reset-password.jsp
- scripts/CheckForgotPassword.java
- PROJECT02_FORGOT_PASSWORD.md

## File sửa

- src/main/java/vn/iotstar/dao/UserDao.java
- src/main/java/vn/iotstar/dao/impl/UserDaoImpl.java
- src/main/java/vn/iotstar/service/UserService.java
- src/main/java/vn/iotstar/service/impl/UserServiceImpl.java
- src/main/java/vn/iotstar/service/MailService.java
- src/main/webapp/views/login.jsp

## Database và OTP

Schema đã kiểm tra có active BIT, otp VARCHAR(64), otpExpiry DATETIME2, otpAttempts INT.
Không cần SQL migration hoặc cột mới.

Activation chỉ dùng tài khoản inactive; reset chỉ dùng tài khoản active. User inactive cần
hoàn tất kích hoạt trước khi quên mật khẩu; yêu cầu reset không ghi đè OTP activation.
Giữ nguyên hash activation. Reset dùng SHA-256 với tiền tố mục đích reset và username.
Dùng chung hàm sinh mã, hash, SQL cooldown, expiry và đếm lần sai.

- OTP 6 chữ số: SecureRandom; hạn 5 phút theo SQL Server UTC.
- 5 lần nhập sai mã có định dạng hợp lệ: chặn tới khi yêu cầu OTP mới.
- Cooldown 60 giây theo tài khoản trong DB; thêm giới hạn gửi trong session.
- Khi verify đúng, transaction thay OTP bằng hash token ngẫu nhiên 256 bit, hạn 5 phút.
  Token chỉ lưu phía server trong session; otpAttempts=5 đánh dấu OTP đã được tiêu thụ.
- Reset cần session đã verify và UPDATE có điều kiện khớp token/expiry trong DB.
  Token không thể dùng lần hai; gửi mã mới làm mất hiệu lực token cũ.
- Thành công: cập nhật password, xóa otp/otpExpiry, otpAttempts=0, invalidate session,
  xóa cookie ghi nhớ của trình duyệt hiện tại và redirect login?reset=1.
- Form có CSRF token; JSP mới đặt dưới WEB-INF, không truy cập trực tiếp.
- Mật khẩu mới 8–255 ký tự, xác nhận phải khớp; không thay cơ chế plaintext của project.

Thông báo yêu cầu/gửi lại và lỗi SMTP được thống nhất, không xác nhận email tồn tại.
Gửi SMTP vẫn đồng bộ như hiện tại; không cam kết che hoàn toàn khác biệt thời gian phản hồi.
Các session/cookie ghi nhớ trên trình duyệt khác vẫn theo cơ chế Login/Logout hiện có.

## Chạy trên Tomcat

1. Publish lại project trong STS rồi reload/restart ứng dụng theo cách hiện tại.
2. Giữ nguyên MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD đang dùng cho Resend.
   MailService giữ nguyên sender Resend đã có, chỉ thêm subject/body cho reset.
3. Không cần chạy SQL, không cần thêm dependency.

URL dưới context /ServletCRUDMVC (dùng cổng Tomcat đang chạy):

- /ServletCRUDMVC/login → link Quên mật khẩu?
- /ServletCRUDMVC/forgot-password
- /ServletCRUDMVC/forgot-password/verify
- /ServletCRUDMVC/reset-password

## Checklist test thủ công

1. Email hợp lệ của tài khoản đã active: nhập email, nhận thông báo chung, tới trang OTP;
   kiểm tra nhận được email có nội dung đặt lại mật khẩu. Với giới hạn sender Resend hiện tại,
   dùng địa chỉ nhận mà cấu hình Resend hiện tại cho phép.
2. Email không tồn tại: cùng thông báo/đường chuyển trang như bước 1, không nhận thư;
   nhập mã bất kỳ không được tới bước reset.
3. OTP đúng trong 5 phút: chuyển /reset-password, không thay mật khẩu ngay tại bước verify.
4. OTP sai: có lỗi tiếng Việt, không được reset; format khác 6 chữ số bị từ chối.
5. OTP hết hạn: đợi hơn 5 phút, mã đúng cũng bị từ chối; yêu cầu OTP mới.
6. Quá 5 lần sai: nhập sai 5 lần rồi nhập mã đúng vẫn bị chặn; resend tạo lượt mới.
7. Resend trước 60 giây: thông báo chung yêu cầu chờ; không gửi thư mới hoặc thay mã hiện tại.
8. Resend sau 60 giây: có mã mới, mã trước bị từ chối, attempts reset; mã mới verify được.
9. Truy cập /reset-password khi chưa verify (cả GET và POST): không đổi mật khẩu;
   GET chuyển về forgot-password; POST thiếu CSRF bị 403, có CSRF nhưng chưa verify cũng bị chặn.
10. Reset thành công: nhập mật khẩu mới và xác nhận giống nhau (8–255 ký tự), về login có
    thông báo thành công; đăng nhập mật khẩu mới thành công. Thử xác nhận không khớp trước đó:
    hiện lỗi và mật khẩu không đổi. Back/submit lại sau thành công không reset lần hai được.
11. Logout rồi đăng nhập bằng mật khẩu cũ: thất bại. Kiểm tra đăng nhập bằng mật khẩu mới vẫn đúng.

Kiểm tra thêm: quyền reset hết hạn sau 5 phút; phiên browser khác không tự truy cập được reset;
activation của tài khoản mới vẫn chạy; SMTP lỗi không lộ exception/API key; không ghi OTP vào log.
Không đưa email, OTP hoặc password vào ảnh chụp/log báo cáo kiểm thử.

## Chạy lại bộ kiểm tra

Từ thư mục project, sau mvn clean package:

    java -cp "target/classes;target/ServletCRUDMVC/WEB-INF/lib/*;.m2/repository/jakarta/servlet/jakarta.servlet-api/6.1.0/jakarta.servlet-api-6.1.0.jar" scripts/CheckForgotPassword.java

Classpath dùng Servlet API hiện có trong cache Maven của project. Nếu máy khác dùng cache Maven
khác, thay đúng đường dẫn jar; không thay dependency. Script không chạy migration và không gửi email.
