# Servlet CRUD MVC - Project 02

## Giới thiệu

Ứng dụng Java Web Servlet/JSP phục vụ bài thực hành quản lý tài khoản, danh mục và sản phẩm.
Kiến trúc Controller → Service → DAO → SQL Server, giao diện tiếng Việt UTF-8.

## Chức năng

- Register và kích hoạt tài khoản bằng OTP email; tài khoản inactive không đăng nhập được.
- Login, Logout (invalidate session), ghi nhớ đăng nhập.
- Forgot Password: OTP → xác nhận → đặt mật khẩu mới.
- Category CRUD, upload icon.
- Product CRUD cho admin, quan hệ Category 1–n Product, upload ảnh.
- Product Detail; danh sách phân trang 6 sản phẩm/trang.
- Home hiển thị tối đa 10 sản phẩm mới nhất, createdDate DESC rồi id DESC.

## Công nghệ

- Java target **17** trong pom.xml; dùng JDK 17 hoặc mới hơn, Maven 3.
- Tomcat **11**, Jakarta Servlet **6.1.0**, JSP API **4.0.0** (provided bởi container).
- JSP/JSTL: API **3.0.2**, implementation **3.0.1**.
- Hibernate **6.6.49.Final**, Jakarta Persistence **3.1.0**.
- SQL Server; Microsoft JDBC **13.2.0.jre11**.
- Jakarta Mail qua Eclipse Angus **2.0.4**, Resend SMTP.
- WAR `ServletCRUDMVC.war`, không dùng Spring Boot hoặc web.xml; mapping bằng annotation.

## Cấu trúc project

```text
pom.xml                          Maven dependencies/build
src/main/java/vn/iotstar/
  controller/                    Servlet
  model/                         User/Category/Product JPA entity
  dao/ và dao/impl/              User auth JDBC; Profile/Category/Product JPA
  service/ và service/impl/      Validation, OTP, nghiệp vụ
  config/ và connection/        AppConfig, JPAConfig, JDBC
  filter/                       Kiểm tra session/quyền Product
  util/                         Hằng số, upload ảnh
src/main/resources/              application.properties, persistence.xml
src/main/webapp/                 JSP và các include UTF-8
 database/                       SQL setup/migration, hướng dẫn
scripts/                         Kiểm tra thủ công bằng Java/PowerShell
PROJECT02_*.md                   Chi tiết từng giai đoạn và checklist
```

`.project`, `.classpath`, `.settings/` được giữ để import STS/Eclipse; không gắn đường dẫn máy cá nhân.
Facet Web hiện lưu 6.0 trong metadata IDE; runtime mục tiêu vẫn là Tomcat 11. Dùng STS/WTP hỗ trợ
Tomcat 11 và kiểm tra Targeted Runtimes/Project Facets nếu IDE báo không tương thích.

## Clone và import STS

```sh
git clone <REPOSITORY_URL> ServletCRUDMVC
cd ServletCRUDMVC
```

Trong STS: **File → Import → Maven → Existing Maven Projects**, chọn thư mục chứa pom.xml.
Chọn JDK phù hợp; Maven → Update Project khi cần đồng bộ dependencies. Không cần cache `.m2/`
của tác giả; Maven tự tải dependencies trong lần build đầu có kết nối mạng.

## Database

Database: `ServletCRUDMVC`. User dùng bảng `dbo.[User]`; Category ánh xạ
`Category(cate_id, cate_name, icons)`; `products.category_id` tham chiếu `Category.cate_id`.
Hibernate tắt tự động tạo/sửa schema.

Chạy thủ công trong SSMS theo thứ tự:

1. `database/ServletCRUDMVC.sql`: database, User, Category.
2. `database/project02_otp_update.sql`: active, otp, otpExpiry, otpAttempts.
3. `database/project02_products.sql`: products và FK/constraint/index.

Xem [database/README.md](database/README.md) để tạo SQL login/quyền và tài khoản admin.
Không có tài khoản hoặc mật khẩu mặc định. Không tự chạy migration khi build.
Máy clone không có dữ liệu demo của tác giả: tạo Category và 13 Product bằng UI để demo phân trang.

## Cấu hình database và environment variables

Ưu tiên đặt các biến trong **Tomcat Launch Configuration → Environment**:

```text
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=ServletCRUDMVC;encrypt=true;trustServerCertificate=true;loginTimeout=5
DB_USER=YOUR_DB_USERNAME
DB_PASSWORD=YOUR_DB_PASSWORD
APP_UPLOAD_DIR=upload
```

`APP_UPLOAD_DIR=upload` tương đối với working directory của JVM. Có thể đặt đường dẫn thư mục upload
riêng của máy trong launch environment, không lưu đường dẫn cá nhân vào repo.
Thư mục này cần quyền ghi và phải được giữ khi redeploy WAR.

`AppConfig` ưu tiên Java system property → environment variable → `application.properties`.
Các khóa tương ứng: `db.url`, `db.user`, `db.password`, `app.upload.dir`.
Chỉ `application.example.properties` được commit, credentials để trống. File `application.properties` chứa cấu hình máy được ignore; giữ credentials ở environment khi có thể.
`.env.example` chỉ là tài liệu mẫu: **ứng dụng không tự đọc .env**.

## Cấu hình Resend SMTP

Đặt cùng launch environment của Tomcat:

```text
MAIL_HOST=smtp.resend.com
MAIL_PORT=587
MAIL_USERNAME=resend
MAIL_PASSWORD=<YOUR_RESEND_API_KEY>
```

Không commit API key. Restart tiến trình Tomcat sau khi thay biến môi trường.
MailService dùng STARTTLS, cổng 587; sender hiện tại `onboarding@resend.dev` dành cho testing.
Địa chỉ nhận phải tuân theo giới hạn testing domain của tài khoản Resend; không giả định gửi được
đến mọi địa chỉ. Khi cần gửi rộng hơn, cấu hình domain/sender đã xác minh theo Resend.
File mẫu đặt Resend; MAIL_HOST luôn ghi đè cấu hình properties cục bộ.

## Tạo cấu hình local sau clone

```powershell
Copy-Item src/main/resources/application.example.properties src/main/resources/application.properties
```

Chỉ copy ở clone mới khi chưa có application.properties. Không ghi đè file local đang dùng.
Cấu hình DB_*, MAIL_* trong môi trường Tomcat theo hướng dẫn trên.

## Build

```sh
mvn clean package
```

Kết quả: `target/ServletCRUDMVC.war`. `target/` không commit.
Tùy chọn PowerShell: `./build.ps1`, hoặc `./build.ps1 -Offline` khi cache đã đủ.
Script này dùng `.mvn/settings.xml` an toàn và cache `.m2/repository` riêng trong project (được ignore).

## Tomcat và khởi động

1. Bật SQL Server, hoàn tất schema và biến môi trường.
2. Mở STS → Servers → New → Server → Apache Tomcat v11.0, chọn Tomcat 11 đã cài và JDK.
3. Add and Remove → thêm project ServletCRUDMVC, giữ context path `ServletCRUDMVC`.
4. Mở Launch Configuration → Environment, thêm DB_*, MAIL_* và APP_UPLOAD_DIR.
5. Publish → Start Tomcat; truy cập [trang chủ](http://localhost:8080/ServletCRUDMVC/home).

Cổng ví dụ 8080; dùng cổng đang cấu hình nếu khác. Không tạo web.xml.
`run-local.ps1 -TomcatHome <TOMCAT_HOME>` là tùy chọn chạy CLI, tạo CATALINA_BASE riêng trong
`.runtime/tomcat-base`, copy WAR và bind localhost. Script không tải Tomcat; không dùng nó để
thay thế cấu hình STS đang hoạt động. Cleanup repo không chạy script này.

## URL chính

Tất cả URL dưới `/ServletCRUDMVC`:

| Public / tài khoản | Chức năng |
|---|---|
| `/home` | 10 sản phẩm mới nhất |
| `/product?page=1` | 6 sản phẩm/trang |
| `/product/detail?id=ID` | Chi tiết |
| `/login`, `/register`, `/logout` | Tài khoản |
| `/verify-otp` | Kích hoạt |
| `/forgot-password`, `/forgot-password/verify`, `/reset-password` | Quên mật khẩu |
| `/image?fname=product/...` | Ảnh upload |

| Admin | Chức năng |
|---|---|
| `/admin/product/list` (alias `/admin/product`) | Danh sách |
| `/admin/product/add`, `/admin/product/edit?id=ID` | Thêm/sửa |
| POST `/admin/product/delete` | Xóa Product |
| `/admin/category/list`, `/admin/category/add`, `/admin/category/edit?id=ID` | Category |
| `/admin/category/delete?id=ID` | Xóa Category theo code hiện tại |

Không có route `/public` hoặc trang `/admin` chung.

## Phân quyền

`roleid=1` là admin; đăng ký mới mặc định roleid=5. `/waiting` chuyển admin tới Category list,
user thường tới Home. Filter kiểm tra session cho `/admin/*`; kiểm tra roleid=1 riêng cho CRUD Product.
Category hiện chỉ được kiểm tra đăng nhập ở server; menu quản trị hiển thị cho admin.
Đây là phạm vi phân quyền thực tế, không tuyên bố Category đã được khóa role toàn bộ.

## OTP

OTP 6 số, hạn 5 phút, lưu SHA-256, tối đa 5 lần sai mã hợp lệ, resend cooldown 60 giây.
Activation áp dụng inactive; reset áp dụng active, tách hash theo mục đích. Verify reset cấp token
một lần trong session/database; reset thành công xóa trạng thái OTP và invalidate session.

## Product và upload

Product thuộc một Category; không cascade xóa Category. Giá không âm, tối đa 2 số thập phân.
Upload Product nhận JPG/JPEG/PNG/WEBP tối đa 5 MB; Category còn nhận GIF. Tên file ngẫu nhiên,
lưu dưới `product/` hoặc `category/` trong APP_UPLOAD_DIR, hiển thị qua servlet `/image`.
Edit không chọn ảnh mới giữ ảnh cũ. File ảnh cũ sau edit/delete có thể còn trên disk.
Các JSP include có khai báo UTF-8 riêng; không chuyển đổi Unicode ở runtime.

## Kiểm thử

Theo kiểm thử của tác giả trên Tomcat: Login/Logout/Register OTP/Forgot Password OTP,
Category/Product CRUD, ảnh, FK, phân trang và Home đã hoạt động.
Các kết quả tự động và giới hạn nằm trong PROJECT02_*.md, không thay thế test trên môi trường clone.

- `scripts/CheckMapping.java`: mapping offline, không ghi DB.
- `scripts/check-database.ps1`: SELECT metadata/count và Category JPA; cần DB environment.
- `scripts/CheckProduct.java`: mặc định giả lập; `--mapping` kiểm tra Hibernate/JPQL;
  `--database` chỉ dùng sau migration, ghi dữ liệu thử rồi rollback.
- `scripts/CheckForgotPassword.java`: JDBC/controller với mail giả lập, rollback dữ liệu thử.
- `scripts/CheckActivation.java`: utility cũ thử migration trong transaction rồi rollback;
  chỉ chạy chủ động trên database phát triển, không dùng như lệnh setup.

Ví dụ sau build:

```powershell
java -cp 'target/classes;target/ServletCRUDMVC/WEB-INF/lib/*' scripts/CheckMapping.java
```

Các CheckProduct/CheckForgotPassword cần thêm Servlet API provided vào classpath; xem tài liệu
PROJECT02 tương ứng. Có thể dùng `./build.ps1` để tải jar vào `.m2/repository` của project trước.
Không chạy kiểm thử ghi dữ liệu trên database thật đang phục vụ người dùng.

## Troubleshooting

- **SQL localhost:1433:** bật TCP/IP cho SQL Server, kiểm tra port/instance/firewall; sửa DB_URL đúng máy.
- **SQL login thất bại:** kiểm tra Mixed Mode, SQL login/database user, mật khẩu/quyền và DB_* trong tiến trình Tomcat.
- **Tomcat 404:** kiểm tra project đã Add/Publish, context ServletCRUDMVC, cổng và URL trong bảng trên.
- **SMTP chưa cấu hình:** MAIL_* phải nằm trong launch environment Tomcat; restart sau chỉnh sửa.
- **Resend không gửi được:** kiểm tra API key, sender/testing-domain và địa chỉ nhận được phép; không đưa key vào log.
- **Maven fail:** dùng JDK 17+, kiểm tra mạng/cache; đọc lỗi đầu tiên rồi chạy lại `mvn clean package`.
- **Không có products:** chạy migration thứ ba bằng SSMS rồi publish lại, không bật auto-DDL để chữa tạm.
- **Chữ tiếng Việt lỗi:** lưu JSP/include UTF-8, publish lại source; không encode/decode runtime.

## Security notes

Không commit API key, DB password, OTP, env secrets, WAR/cache/log hoặc ảnh upload runtime.
Đây là project thực hành: mật khẩu ứng dụng hiện lưu plaintext và remember cookie chứa username;
chưa phù hợp triển khai production. Cleanup này không đổi cơ chế login/reset đang hoạt động.
Nếu trước đây đã chia sẻ credentials ra ngoài, cần thu hồi/đổi credentials đó riêng; ignore không xóa lịch sử Git.

## Tác giả

Trần Anh Khoa.

## Profile User và triển khai WAR độc lập

Đã bổ sung `/ServletCRUDMVC/profile`: xem hồ sơ mới nhất từ database, sửa fullname/phone/avatar
bằng JPA/Hibernate. ID lấy từ session; không cho form thay username/email/password/role/OTP.
Header có link Hồ sơ; lưu thành công cập nhật session. Login/OTP tiếp tục dùng JDBC tương thích.

Build không cần STS: `mvn clean package`, deploy `target/ServletCRUDMVC.war` lên Tomcat 11 đã cài
bên ngoài project. Không có Maven Wrapper trong project. Đặt `APP_UPLOAD_DIR` ngoài WAR/target và
giữ nguyên thư mục qua restart/redeploy để ảnh bền vững. Không đóng gói hoặc tự tải Tomcat.

SMTP hiện tác giả dùng Gmail qua MAIL_*; sender lấy từ username cấu hình. Phần Resend phía trên
là lựa chọn cấu hình trước đây, không yêu cầu đổi SMTP đang chạy. Không sửa MailService khi dùng Profile.

Xem [PROFILE_USER.md](PROFILE_USER.md) để biết file thay đổi, cách deploy Tomcat độc lập,
kiểm tra JPA và checklist Profile/ảnh sau logout/login/restart.