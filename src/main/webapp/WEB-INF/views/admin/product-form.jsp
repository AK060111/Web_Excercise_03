<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty alert}"><p class="alert"><c:out value="${alert}"/></p></c:if>
<c:if test="${empty categories}"><p class="alert">Chưa có danh mục. Hãy thêm danh mục trước khi tạo sản phẩm.</p></c:if>
<form action="${pageContext.request.contextPath}/admin/product/${product.id > 0 ? 'edit' : 'add'}" method="post" enctype="multipart/form-data">
<input type="hidden" name="csrf" value="${sessionScope.productCsrf}"><input type="hidden" name="id" value="${product.id}">
<label class="field">Tên sản phẩm<input name="name" value="<c:out value='${product.name}'/>" maxlength="255" required></label>
<label class="field">Mô tả<textarea name="description" rows="5"><c:out value="${product.description}"/></textarea></label>
<label class="field">Giá (₫, lớn hơn 0)<input name="price" type="text" inputmode="decimal" pattern="[0-9]+([.][0-9]{1,2})?" maxlength="32" value="<c:out value='${productSubmitted ? productPriceInput : product.price}'/>" required></label>
<label class="field">Danh mục<select name="categoryId" required><option value="">Chọn danh mục</option>
<c:forEach items="${categories}" var="category"><option value="${category.id}" ${(productSubmitted ? productCategoryInput == category.id.toString() : product.category.id == category.id) ? 'selected' : ''}><c:out value="${category.name}"/></option></c:forEach>
</select></label>
<c:set var="currentImage" value="${empty oldImage ? product.image : oldImage}"/>
<c:if test="${not empty currentImage}"><c:url value="/image" var="imageUrl"><c:param name="fname" value="${currentImage}"/></c:url><p><img class="thumb" src="<c:out value='${imageUrl}'/>" alt="Ảnh hiện tại"></p></c:if>
<label class="field">Ảnh (JPG, JPEG, PNG, WEBP; tối đa 5 MB)<input type="file" name="image" accept="image/jpeg,image/png,image/webp"></label>
<p><small>Để trống để giữ ảnh cũ. Khi nhập lại do lỗi, cần chọn lại file nếu muốn thay ảnh.</small></p>
<button type="submit">Lưu sản phẩm</button> <a class="btn secondary" href="${pageContext.request.contextPath}/admin/product/list">Hủy</a>
</form>
