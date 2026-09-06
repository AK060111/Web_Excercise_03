package vn.iotstar.controller;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import vn.iotstar.model.Category;
import vn.iotstar.service.CategoryService;
import vn.iotstar.service.impl.CategoryServiceImpl;
import vn.iotstar.util.Constant;

@WebServlet("/admin/category/edit")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024)
public class CategoryEditController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final CategoryService service = new CategoryServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Category category = service.get(Integer.parseInt(req.getParameter("id")));
            if (category == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("category", category);
            req.getRequestDispatcher("/views/admin/edit-category.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh mục không hợp lệ");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Category category = new Category();
        try {
            category.setId(Integer.parseInt(req.getParameter("id")));
            category.setName(req.getParameter("name") == null ? null : req.getParameter("name").trim());
            Part icon = req.getPart("icon");
            if (icon != null && icon.getSize() > 0) {
                category.setIcon(CategoryAddController.saveImage(icon));
            }
            if (category.getName() == null || category.getName().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tên danh mục không được rỗng");
                return;
            }
            Category old = service.get(category.getId());
            if (old == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            service.edit(category);
            if (category.getIcon() != null && old.getIcon() != null) {
                File oldImage = new File(Constant.DIR, old.getIcon());
                if (oldImage.exists()) oldImage.delete();
            }
            resp.sendRedirect(req.getContextPath() + "/admin/category/list");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh mục không hợp lệ");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ServletException("Không thể sửa danh mục", e);
        }
    }
}
