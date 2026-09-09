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
    private final CategoryService service;
    public CategoryAddController(){this(new CategoryServiceImpl());}
    public CategoryAddController(CategoryService service){this.service=service;}

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        vn.iotstar.util.Csrf.prepare(req,"categoryCsrf");
        req.getRequestDispatcher("/views/admin/add-category.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Category category = new Category();
        String uploaded=null;boolean saved=false;
        try {
            if(!vn.iotstar.util.Csrf.require(req,resp,"categoryCsrf"))return;
            category.setName(req.getParameter("name") == null ? null : req.getParameter("name").trim());
            req.setAttribute("categoryName",category.getName());
            service.validate(category);
            Part icon = req.getPart("icon");
            if (icon != null && icon.getSize() > 0) {
                uploaded=saveImage(icon);category.setIcon(uploaded);
            }
            service.insert(category);
            saved=true;
            resp.sendRedirect(req.getContextPath() + "/admin/category/list");
        } catch (IllegalArgumentException e) {
            error(req, resp, e.getMessage());
        } catch (Exception e) {
            error(req,resp,"Không thể thêm danh mục. Kiểm tra dữ liệu và ảnh tối đa 5 MB rồi thử lại.");
        }
        finally{if(!saved)vn.iotstar.util.ImageUpload.discardNewCategoryImage(uploaded);}
    }

    static String saveImage(Part part) throws Exception {
        return vn.iotstar.util.ImageUpload.save(part, "category");
    }

    private void error(HttpServletRequest req, HttpServletResponse resp, String message) throws ServletException, IOException {
        req.setAttribute("alert", message == null ? "Dữ liệu không hợp lệ" : message);
        req.getRequestDispatcher("/views/admin/add-category.jsp").forward(req, resp);
    }
}

