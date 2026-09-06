<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html><head><title>Quên mật khẩu</title><%@ include file="/views/includes/style.jsp" %></head>
<body><main class="card auth"><h1>Quên mật khẩu</h1>
<p>Nhập email của tài khoản đã kích hoạt để nhận OTP đặt lại mật khẩu.</p>
<c:if test="${not empty alert}"><p class="alert"><c:out value="${alert}"/></p></c:if>
<form action="${pageContext.request.contextPath}/forgot-password" method="post">
<input type="hidden" name="csrf" value="${sessionScope.forgotCsrf}">
<label class="field">Email<input type="email" name="email" autocomplete="email" maxlength="255" required></label>
<button type="submit">Gửi OTP</button></form>
<p><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
</main></body></html>
