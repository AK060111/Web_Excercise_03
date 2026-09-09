<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html><html><head><title>Trang chủ</title><%@ include file="includes/style.jsp" %></head>
<body><%@ include file="includes/header.jsp" %><main class="wrap card"><h1>Trang chủ</h1>
<c:if test="${not empty sessionScope.account}"><p>Bạn đã đăng nhập thành công.</p></c:if>
<h2>Sản phẩm mới nhất</h2>
<c:choose><c:when test="${not empty productError}"><p class="alert"><c:out value="${productError}"/></p></c:when>
<c:otherwise><%@ include file="includes/product-cards.jsp" %></c:otherwise></c:choose>
<p><a href="${pageContext.request.contextPath}/product">Xem tất cả sản phẩm</a></p>
</main></body></html>
