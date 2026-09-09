package vn.iotstar.controller;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.service.ProductService;
import vn.iotstar.service.impl.ProductServiceImpl;
@WebServlet("/home")
public class HomeController extends HttpServlet {
    private final ProductService products=new ProductServiceImpl();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        try{req.setAttribute("products",products.findNewest(10));}
        catch(RuntimeException e){req.setAttribute("productError","Chưa tải được sản phẩm. Vui lòng thử lại sau.");}
        req.getRequestDispatcher("/views/home.jsp").forward(req,resp);
    }
}
