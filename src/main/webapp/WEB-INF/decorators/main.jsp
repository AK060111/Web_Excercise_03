<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- SiteMesh 3 processes sitemesh:write in the rendered markup; no JSP taglib is needed. --%>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1">

    <title>
        Hồ sơ cá nhân
        <sitemesh:write property="title"/>
    </title>

    <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css">

    <%@ include file="/views/includes/style.jsp" %>

    <sitemesh:write property="head"/>
</head>

<body>

    <%@ include file="/views/includes/header.jsp" %>

    <sitemesh:write property="body"/>

    <footer class="text-center py-3">
        <small>Servlet CRUD MVC</small>
    </footer>

</body>
</html>
