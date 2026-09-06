package vn.iotstar.controller;

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

@WebServlet("/admin/category/add")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024)
public class CategoryAddController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final CategoryService service = new CategoryServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/views/admin/add-category.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Category category = new Category();
        try {
            category.setName(req.getParameter("name") == null ? null : req.getParameter("name").trim());
            Part icon = req.getPart("icon");
            if (icon != null && icon.getSize() > 0) {
                category.setIcon(saveImage(icon));
            }
            if (category.getName() == null || category.getName().isEmpty()) {
                error(req, resp, "Tên danh mục không được rỗng");
                return;
            }
            service.insert(category);
            resp.sendRedirect(req.getContextPath() + "/admin/category/list");
        } catch (IllegalArgumentException e) {
            error(req, resp, e.getMessage());
        } catch (Exception e) {
            throw new ServletException("Không thể thêm danh mục", e);
        }
    }

    static String saveImage(Part part) throws Exception {
        return vn.iotstar.util.ImageUpload.save(part, "category");
    }

    private void error(HttpServletRequest req, HttpServletResponse resp, String message) throws ServletException, IOException {
        req.setAttribute("alert", message == null ? "Dữ liệu không hợp lệ" : message);
        req.getRequestDispatcher("/views/admin/add-category.jsp").forward(req, resp);
    }
}

