package vn.iotstar.controller;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.service.CategoryService;
import vn.iotstar.service.impl.CategoryServiceImpl;

@WebServlet("/admin/category/list")
public class CategoryListController extends HttpServlet {
    private final CategoryService service = new CategoryServiceImpl();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        vn.iotstar.util.Csrf.prepare(req,"categoryCsrf");
        String keyword = req.getParameter("keyword");
        try{req.setAttribute("cateList", keyword == null || keyword.trim().isEmpty() ? service.getAll() : service.search(keyword.trim()));}
        catch(RuntimeException e){resp.sendError(503,"Chưa thể tải danh mục. Vui lòng thử lại sau.");return;}
        req.getRequestDispatcher("/views/admin/list-category.jsp").forward(req, resp);
    }
}
