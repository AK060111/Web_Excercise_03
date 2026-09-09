<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html><head><title>Đặt mật khẩu mới</title><%@ include file="/views/includes/style.jsp" %></head>
<body><main class="card auth"><h1>Đặt mật khẩu mới</h1>
<p>Hoàn tất trong vòng 5 phút sau khi xác nhận OTP.</p>
<c:if test="${not empty alert}"><p class="alert"><c:out value="${alert}"/></p></c:if>
<form action="${pageContext.request.contextPath}/reset-password" method="post">
<input type="hidden" name="csrf" value="${sessionScope.forgotCsrf}">
<label class="field">Mật khẩu mới<input type="password" name="password" autocomplete="new-password" minlength="8" maxlength="255" required></label>
<label class="field">Xác nhận mật khẩu<input type="password" name="confirmPassword" autocomplete="new-password" minlength="8" maxlength="255" required></label>
<button type="submit">Đổi mật khẩu</button></form>
<p><a href="${pageContext.request.contextPath}/forgot-password">Yêu cầu OTP mới</a></p>
</main></body></html>
