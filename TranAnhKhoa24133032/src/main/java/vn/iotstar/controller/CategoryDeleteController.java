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
    private final CategoryService service = new CategoryServiceImpl();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try { Category category=service.get(Integer.parseInt(req.getParameter("id"))); if(category==null){resp.sendError(404);return;} service.delete(category.getId()); if(category.getIcon()!=null){File image=new File(Constant.DIR, category.getIcon());if(image.exists())image.delete();} resp.sendRedirect(req.getContextPath()+"/admin/category/list"); }
        catch (NumberFormatException e) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID danh mục không hợp lệ"); }
        catch (IllegalStateException e) { resp.sendError(HttpServletResponse.SC_CONFLICT, "Không thể xóa danh mục. Nếu danh mục có sản phẩm, hãy chuyển hoặc xóa sản phẩm trước."); }
    }
}
