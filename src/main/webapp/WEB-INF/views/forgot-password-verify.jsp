<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html><head><title>Xác nhận OTP đặt lại mật khẩu</title><%@ include file="/views/includes/style.jsp" %></head>
<body><main class="card auth"><h1>Xác nhận OTP</h1>
<p>Nhập OTP 6 chữ số từ email. Mã có hiệu lực 5 phút, tối đa 5 lần nhập sai.</p>
<c:if test="${not empty message}"><p class="success"><c:out value="${message}"/></p></c:if>
<c:if test="${not empty alert}"><p class="alert"><c:out value="${alert}"/></p></c:if>
<form action="${pageContext.request.contextPath}/forgot-password/verify" method="post">
<input type="hidden" name="csrf" value="${sessionScope.forgotCsrf}">
<label class="field">OTP<input name="otp" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" required></label>
<button type="submit" name="action" value="verify">Xác nhận</button></form>
<form action="${pageContext.request.contextPath}/forgot-password/verify" method="post">
<input type="hidden" name="csrf" value="${sessionScope.forgotCsrf}">
<p><button type="submit" name="action" value="resend">Gửi lại OTP</button></p></form>
<p><small>Chờ ít nhất 60 giây giữa các lần gửi. Mã mới thay thế mã cũ.</small></p>
<p><a href="${pageContext.request.contextPath}/forgot-password">Nhập lại email</a> · <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
</main></body></html>
