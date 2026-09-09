> Current audit: see [AUDIT_RESULTS.md](AUDIT_RESULTS.md). The real Tomcat 11 test requires the packaged context.xml response-suspension setting as well as decorator INCLUDE; earlier mock-only rendering conclusions below are historical.

# Bảo mật, validation và layout Profile

## Phạm vi

Giữ Servlet/JSP + DAO/Service, Profile JPA, SQL schema, cấu hình local và Tomcat 11.
SiteMesh `org.sitemesh:sitemesh:3.3.0-RC1` dùng Jakarta Servlet (artifact đã resolve từ Maven Central).
Nâng từ 3.2.1 sau lỗi Tomcat thực tế: decorator gọi forward khi response đã committed.
Bytecode 3.3.0-RC1 có DispatchMode.DETECT, nhận diện Apache Tomcat 11+ qua getServerInfo()
và chọn RequestDispatcher.include(). API filter cũ vẫn tương thích; không đổi ProfileLayoutFilter.
CheckSecurity mô phỏng Tomcat 11 và response committed, kiểm tra decorator dùng include.
Đây là kiểm tra proxy, vẫn cần Clean/Publish/Restart và xác minh trên Tomcat thật.
Filter annotation chỉ bao `/profile`
ở dispatcher REQUEST, bao gồm nội dung JSP được controller forward. Không tạo web.xml.
Decorator nằm trong WEB-INF; Profile là fragment, không có html/head/body/header riêng.
Đặt decoratorPrefix rỗng để đường dẫn tuyệt đối không bị thêm tiền tố hai lần.
Bootstrap 5.3.8 tải từ CDN; stylesheet sẵn có vẫn được include sau Bootstrap.

ImageIO kiểm tra nội dung, format và kích thước cho cả avatar/category/product.
Thêm TwelveMonkeys imageio-webp 3.15.0 để tiếp tục đọc WEBP.
Ảnh cũ giữ nguyên; chỉ file mới sinh của request thất bại được dọn. Không tự xóa upload cũ.
Điện thoại optional, nhận 0 + 9 số hoặc +84 + 9 số với đầu số di động 3/5/7/8/9.
Không tự sửa số điện thoại cũ; khi lưu hồ sơ, người dùng cần nhập theo quy tắc mới.

## Mật khẩu plaintext chưa được giải quyết

Theo phạm vi yêu cầu, không thay đổi định dạng password hiện tại:

- `UserServiceImpl.login`: so sánh trực tiếp password với `User.getPassWord()`.
- `UserServiceImpl.register` và `UserDaoImpl.insert`: chuyển và ghi password gốc.
- `UserServiceImpl.resetPassword` và `UserDaoImpl.resetPassword`: ghi password gốc sau grant hợp lệ.
- `UserDaoImpl.find` / `User.passWord`: đọc/chứa password hiện tại.

Đề xuất tác vụ riêng: chọn password KDF phù hợp, định dạng hash có version, xử lý
legacy password khi đăng nhập thành công, luôn hash đăng ký/reset mới, kiểm thử
tương thích và kế hoạch rollback. Không dùng SHA-256 OTP để thay cho password KDF.

## Các giới hạn vận hành

- UNIQUE phone hiện có có thể hạn chế số tài khoản để trống; không sửa schema.
- Chưa thêm giới hạn tốc độ đăng nhập toàn hệ thống.
- Local application.properties vẫn được Maven copy vào WAR; không chia sẻ WAR chứa cấu hình local.
- Giữ nguyên staged repair Git trước đó; thay đổi chức năng chưa stage/commit.

## Chạy kiểm tra

Sau `mvn clean package`, chạy Java source scripts với classpath gồm target/classes,
target/ServletCRUDMVC/WEB-INF/lib/* và Jakarta Servlet API 6.1.0 provided trong Maven cache.
CheckSecurity không dùng database/mail thật, chỉ tạo ảnh thử dưới target.
CheckForgotPassword, CheckProfile và CheckProduct --database dùng fixture rollback.
Fixture phone của các script đã đổi sang số di động hợp lệ, không nới lỏng validation.
Không chạy CheckActivation vì script đó thực thi migration.

Build không precompile JSP. Cần kiểm tra trên Tomcat hiện tại: guest/admin/user thường,
login/logout, POST+CSRF Category, Product giá >0, các ảnh thật/giả/quá cỡ, OTP/reset bằng
email thử được phép, Profile multipart và decorator chỉ có một layout, avatar sau restart.

## Danh sách file thay đổi

### Đã sửa (30)

- [pom.xml](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/pom.xml)
- [scripts/CheckForgotPassword.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/scripts/CheckForgotPassword.java)
- [scripts/CheckProduct.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/scripts/CheckProduct.java)
- [scripts/CheckProfile.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/scripts/CheckProfile.java)
- [src/main/java/vn/iotstar/controller/CategoryAddController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/CategoryAddController.java)
- [src/main/java/vn/iotstar/controller/CategoryDeleteController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/CategoryDeleteController.java)
- [src/main/java/vn/iotstar/controller/CategoryEditController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/CategoryEditController.java)
- [src/main/java/vn/iotstar/controller/CategoryListController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/CategoryListController.java)
- [src/main/java/vn/iotstar/controller/DownloadImageController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/DownloadImageController.java)
- [src/main/java/vn/iotstar/controller/ForgotPasswordController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/ForgotPasswordController.java)
- [src/main/java/vn/iotstar/controller/LoginController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/LoginController.java)
- [src/main/java/vn/iotstar/controller/ProductAdminController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/ProductAdminController.java)
- [src/main/java/vn/iotstar/controller/RegisterController.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/controller/RegisterController.java)
- [src/main/java/vn/iotstar/dao/impl/CategoryDaoImpl.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/dao/impl/CategoryDaoImpl.java)
- [src/main/java/vn/iotstar/filter/AuthenticationFilter.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/filter/AuthenticationFilter.java)
- [src/main/java/vn/iotstar/service/CategoryService.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/service/CategoryService.java)
- [src/main/java/vn/iotstar/service/MailService.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/service/MailService.java)
- [src/main/java/vn/iotstar/service/impl/CategoryServiceImpl.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/service/impl/CategoryServiceImpl.java)
- [src/main/java/vn/iotstar/service/impl/ProductServiceImpl.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/service/impl/ProductServiceImpl.java)
- [src/main/java/vn/iotstar/service/impl/UserProfileServiceImpl.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/service/impl/UserProfileServiceImpl.java)
- [src/main/java/vn/iotstar/service/impl/UserServiceImpl.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/service/impl/UserServiceImpl.java)
- [src/main/java/vn/iotstar/util/ImageUpload.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/util/ImageUpload.java)
- [src/main/webapp/WEB-INF/views/admin/product-form.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/WEB-INF/views/admin/product-form.jsp)
- [src/main/webapp/WEB-INF/views/forgot-password.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/WEB-INF/views/forgot-password.jsp)
- [src/main/webapp/WEB-INF/views/profile.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/WEB-INF/views/profile.jsp)
- [src/main/webapp/views/admin/add-category.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/views/admin/add-category.jsp)
- [src/main/webapp/views/admin/edit-category.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/views/admin/edit-category.jsp)
- [src/main/webapp/views/admin/list-category.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/views/admin/list-category.jsp)
- [src/main/webapp/views/login.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/views/login.jsp)
- [src/main/webapp/views/register.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/views/register.jsp)

### Tạo mới (6)

- [SECURITY_VALIDATION_SITEMESH.md](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/SECURITY_VALIDATION_SITEMESH.md)
- [scripts/CheckSecurity.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/scripts/CheckSecurity.java)
- [src/main/java/vn/iotstar/filter/ProfileLayoutFilter.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/filter/ProfileLayoutFilter.java)
- [src/main/java/vn/iotstar/util/Csrf.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/util/Csrf.java)
- [src/main/java/vn/iotstar/util/ValidationUtil.java](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/java/vn/iotstar/util/ValidationUtil.java)
- [src/main/webapp/WEB-INF/decorators/main.jsp](D:/UTE/Nam_3/Lap_Trinh_Web/TranAnhKhoa24133032/TranAnhKhoa24133032/src/main/webapp/WEB-INF/decorators/main.jsp)
