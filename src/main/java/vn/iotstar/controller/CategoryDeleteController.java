package vn.iotstar.controller;

import java.io.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.model.Category;
import vn.iotstar.service.CategoryService;
import vn.iotstar.service.impl.CategoryServiceImpl;
import vn.iotstar.util.Constant;

@WebServlet("/admin/category/delete")
public class CategoryDeleteController extends HttpServlet {
    private final CategoryService service;
    public CategoryDeleteController(){this(new CategoryServiceImpl());}
    public CategoryDeleteController(CategoryService service){this.service=service;}
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Allow","POST");resp.sendError(405,"Vui lòng dùng nút Xóa trong danh sách.");
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        if(!vn.iotstar.util.Csrf.require(req,resp,"categoryCsrf"))return;
        try { Category category=service.get(Integer.parseInt(req.getParameter("id"))); if(category==null){resp.sendError(404);return;} service.delete(category.getId()); resp.sendRedirect(req.getContextPath()+"/admin/category/list"); }
        catch (NumberFormatException e) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh mục không hợp lệ"); }
        catch (IllegalStateException e) { resp.sendError(HttpServletResponse.SC_CONFLICT, "Không thể xóa danh mục. Nếu danh mục có sản phẩm, hãy chuyển hoặc xóa sản phẩm trước."); }
    }
}
