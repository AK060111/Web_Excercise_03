<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html><html><head><title>Sản phẩm</title><%@ include file="/views/includes/style.jsp" %></head>
<body><%@ include file="/views/includes/header.jsp" %><main class="wrap card"><h1>Sản phẩm</h1>
<%@ include file="/views/includes/product-cards.jsp" %>
<nav class="pagination" aria-label="Phân trang">
<c:if test="${page > 1}"><a class="btn secondary" href="${pageContext.request.contextPath}/product?page=${page-1}">Trang trước</a></c:if>
<span>Trang ${page} / ${totalPages}</span>
<c:if test="${page < totalPages}"><a class="btn" href="${pageContext.request.contextPath}/product?page=${page+1}">Trang sau</a></c:if>
</nav></main></body></html>
