<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<div class="product-grid">
<c:forEach items="${products}" var="item">
<article class="product-card">
<c:url value="/product/detail" var="detailUrl"><c:param name="id" value="${item.id}"/></c:url>
<a href="${detailUrl}">
<c:choose><c:when test="${not empty item.image}">
<c:url value="/image" var="imageUrl"><c:param name="fname" value="${item.image}"/></c:url>
<img class="product-image" src="<c:out value='${imageUrl}'/>" alt="<c:out value='${item.name}'/>">
</c:when><c:otherwise><div class="product-image no-image">Chưa có ảnh</div></c:otherwise></c:choose>
<h3><c:out value="${item.name}"/></h3></a>
<p><fmt:formatNumber value="${item.price}" maxFractionDigits="2"/> ₫</p>
<p><c:out value="${item.category.name}"/></p><a href="${detailUrl}">Xem chi tiết</a>
</article>
</c:forEach>
</div>
<c:if test="${empty products}"><p>Chưa có sản phẩm.</p></c:if>
