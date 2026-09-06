<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html><head><title>Chi tiết sản phẩm</title><%@ include file="/views/includes/style.jsp" %></head>
<body><%@ include file="/views/includes/header.jsp" %><main class="wrap card">
<h1><c:out value="${product.name}"/></h1>
<c:choose><c:when test="${not empty product.image}"><c:url value="/image" var="imageUrl"><c:param name="fname" value="${product.image}"/></c:url>
<img class="product-detail-image" src="<c:out value='${imageUrl}'/>" alt="<c:out value='${product.name}'/>"></c:when><c:otherwise><p>Chưa có ảnh</p></c:otherwise></c:choose>
<p><strong>Giá:</strong> <fmt:formatNumber value="${product.price}" maxFractionDigits="2"/> ₫</p>
<p><strong>Danh mục:</strong> <c:out value="${product.category.name}"/></p>
<p><strong>Ngày tạo:</strong> <c:out value="${product.createdDateDisplay}"/></p>
<h2>Mô tả</h2><p class="product-description"><c:out value="${empty product.description ? 'Chưa có mô tả.' : product.description}"/></p>
<a href="${pageContext.request.contextPath}/product">Tất cả sản phẩm</a>
</main></body></html>
