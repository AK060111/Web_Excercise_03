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

@WebServlet("/admin/category/edit")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024)
public class CategoryEditController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final CategoryService service;
    public CategoryEditController(){this(new CategoryServiceImpl());}
    public CategoryEditController(CategoryService service){this.service=service;}

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            vn.iotstar.util.Csrf.prepare(req,"categoryCsrf");
            Category category = service.get(Integer.parseInt(req.getParameter("id")));
            if (category == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("category", category);
            req.getRequestDispatcher("/views/admin/edit-category.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh mục không hợp lệ");
        } catch(RuntimeException e){
            resp.sendError(503,"Chưa thể tải danh mục. Vui lòng thử lại sau.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Category category = new Category();
        String uploaded=null,oldIcon=null;boolean saved=false;
        try {
            if(!vn.iotstar.util.Csrf.require(req,resp,"categoryCsrf"))return;
            category.setId(Integer.parseInt(req.getParameter("id")));
            category.setName(req.getParameter("name") == null ? null : req.getParameter("name").trim());
            Category old=service.get(category.getId());
            if(old==null){resp.sendError(404);return;}
            oldIcon=old.getIcon();category.setIcon(oldIcon);req.setAttribute("category",category);
            service.validate(category);
            Part icon = req.getPart("icon");
            if (icon != null && icon.getSize() > 0) {
                uploaded=CategoryAddController.saveImage(icon);category.setIcon(uploaded);
            }
            service.edit(category);
            saved=true;
            resp.sendRedirect(req.getContextPath() + "/admin/category/list");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh mục không hợp lệ");
        } catch (IllegalArgumentException e) {
            category.setIcon(oldIcon);
            error(req,resp,e.getMessage());
        } catch (Exception e) {
            category.setIcon(oldIcon);
            error(req,resp,"Không thể sửa danh mục. Kiểm tra dữ liệu và ảnh tối đa 5 MB rồi thử lại.");
        }
        finally{if(!saved)vn.iotstar.util.ImageUpload.discardNewCategoryImage(uploaded);}
    }
    private void error(HttpServletRequest req,HttpServletResponse resp,String message)throws ServletException,IOException{
        req.setAttribute("alert",message);
        req.getRequestDispatcher("/views/admin/edit-category.jsp").forward(req,resp);
    }
}
