<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html><head><title>Quản lý sản phẩm</title><%@ include file="/views/includes/style.jsp" %></head>
<body><%@ include file="/views/includes/header.jsp" %><main class="wrap card"><h1>Quản lý sản phẩm</h1>
<c:if test="${param.done == '1'}"><p class="success">Thao tác thành công.</p></c:if>
<a class="btn" href="${pageContext.request.contextPath}/admin/product/add">Thêm sản phẩm</a>
<div class="table-scroll"><table><thead><tr><th>ID</th><th>Ảnh</th><th>Tên</th><th>Giá</th><th>Danh mục</th><th>Ngày tạo</th><th>Thao tác</th></tr></thead><tbody>
<c:forEach items="${products}" var="item"><tr>
<td>${item.id}</td><td><c:if test="${not empty item.image}"><c:url value="/image" var="imageUrl"><c:param name="fname" value="${item.image}"/></c:url><img class="thumb" src="<c:out value='${imageUrl}'/>" alt="<c:out value='${item.name}'/>"></c:if></td>
<td><a href="${pageContext.request.contextPath}/product/detail?id=${item.id}"><c:out value="${item.name}"/></a></td>
<td><fmt:formatNumber value="${item.price}" maxFractionDigits="2"/> ₫</td><td><c:out value="${item.category.name}"/></td><td><c:out value="${item.createdDateDisplay}"/></td>
<td><a class="btn secondary" href="${pageContext.request.contextPath}/admin/product/edit?id=${item.id}">Sửa</a>
<form action="${pageContext.request.contextPath}/admin/product/delete" method="post" onsubmit="return confirm('Xóa sản phẩm này?')">
<input type="hidden" name="id" value="${item.id}"><input type="hidden" name="csrf" value="${sessionScope.productCsrf}"><button class="btn danger" type="submit">Xóa</button></form></td>
</tr></c:forEach>
<c:if test="${empty products}"><tr><td colspan="7">Chưa có sản phẩm.</td></tr></c:if>
</tbody></table></div></main></body></html>
