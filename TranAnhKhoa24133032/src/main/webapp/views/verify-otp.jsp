<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html><head><title>Kích hoạt tài khoản</title><%@ include file="includes/style.jsp" %></head>
<body><main class="card auth">
<h1>Kích hoạt tài khoản</h1>
<p>Nhập mã 6 chữ số được gửi tới email đăng ký. Mã có hiệu lực 5 phút.</p>
<c:if test="${not empty message}"><p class="success"><c:out value="${message}"/></p></c:if>
<c:if test="${not empty alert}"><p class="alert"><c:out value="${alert}"/></p></c:if>
<form action="${pageContext.request.contextPath}/verify-otp" method="post">
<label class="field">OTP<input name="otp" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" required></label>
<button name="action" value="verify" type="submit">Xác nhận</button>
</form>
<form action="${pageContext.request.contextPath}/verify-otp" method="post">
<p><button name="action" value="resend" type="submit">Gửi lại OTP</button></p>
</form>
<p><small>Mỗi lần gửi cách nhau ít nhất 60 giây. Kiểm tra cả thư mục Spam. Sau 5 lần nhập sai, cần gửi mã mới.</small></p>
<p><a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
</main></body></html>
