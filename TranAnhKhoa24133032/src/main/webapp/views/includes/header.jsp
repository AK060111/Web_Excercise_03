<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<header class="topbar">
  <a class="brand" href="${pageContext.request.contextPath}/home">Servlet CRUD MVC</a>
  <nav>
    <a href="${pageContext.request.contextPath}/product">Sản phẩm</a>
    <c:if test="${sessionScope.account.roleid == 1}">
      <a href="${pageContext.request.contextPath}/admin/product/list">Quản lý sản phẩm</a>
      <a href="${pageContext.request.contextPath}/admin/category/list">Danh mục</a>
    </c:if>
    <c:choose>
      <c:when test="${sessionScope.account == null}"><a href="${pageContext.request.contextPath}/login">Đăng nhập</a><a href="${pageContext.request.contextPath}/register">Đăng ký</a></c:when>
      <c:otherwise><span>Xin chào, <c:out value="${sessionScope.account.fullName}"/></span><a href="${pageContext.request.contextPath}/profile">Hồ sơ</a><a href="${pageContext.request.contextPath}/logout">Đăng xuất</a></c:otherwise>
    </c:choose>
  </nav>
</header>
