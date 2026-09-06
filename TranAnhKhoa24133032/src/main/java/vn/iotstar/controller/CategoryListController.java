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
        String keyword = req.getParameter("keyword");
        req.setAttribute("cateList", keyword == null || keyword.trim().isEmpty() ? service.getAll() : service.search(keyword.trim()));
        req.getRequestDispatcher("/views/admin/list-category.jsp").forward(req, resp);
    }
}
