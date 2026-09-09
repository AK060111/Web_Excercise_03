<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<main class="wrap card">

    <h1>Hồ sơ cá nhân</h1>

    <c:if test="${not empty message}">
        <p class="success">
            <c:out value="${message}"/>
        </p>
    </c:if>

    <c:if test="${not empty alert}">
        <p class="alert">
            <c:out value="${alert}"/>
        </p>
    </c:if>

    <c:choose>

        <c:when test="${not empty profile.avatar}">

            <c:url value="/image" var="avatarUrl">
                <c:param
                    name="fname"
                    value="${profile.avatar}"/>
            </c:url>

            <p>
                <img
                    class="thumb"
                    src="<c:out value='${avatarUrl}'/>"
                    alt="Ảnh đại diện hiện tại">
            </p>

        </c:when>

        <c:otherwise>
            <p>Chưa có ảnh đại diện.</p>
        </c:otherwise>

    </c:choose>

    <p>
        <strong>Username:</strong>
        <c:out value="${profile.userName}"/>
    </p>

    <p>
        <strong>Email:</strong>
        <c:out value="${profile.email}"/>
    </p>

    <form
        action="${pageContext.request.contextPath}/profile"
        method="post"
        enctype="multipart/form-data">

        <input
            type="hidden"
            name="csrf"
            value="<c:out value='${sessionScope.profileCsrf}'/>">

        <label class="field">
            Họ và tên

            <input
                name="fullname"
                value="<c:out value='${profileFullname}'/>"
                maxlength="255"
                autocomplete="name"
                required>
        </label>

        <label class="field">
            Số điện thoại

            <input
                name="phone"
                type="tel"
                value="<c:out value='${profilePhone}'/>"
                maxlength="12"
                autocomplete="tel"
                placeholder="0912345678 hoặc +84912345678">
        </label>

        <label class="field">
            Ảnh đại diện

            <input
                name="avatar"
                type="file"
                accept="image/png,image/jpeg,image/gif">
        </label>

        <p>
            <small>
                JPG, JPEG, PNG hoặc GIF; tối đa 5 MB và
                4096 x 4096 pixel.
                Không chọn ảnh mới để giữ avatar cũ.
            </small>
        </p>

        <button type="submit">
            Lưu hồ sơ
        </button>

        <a
            class="btn secondary"
            href="${pageContext.request.contextPath}/home">
            Trang chủ
        </a>

    </form>

</main>