package vn.iotstar.controller;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.model.Product;
import vn.iotstar.service.ProductService;
import vn.iotstar.service.impl.ProductServiceImpl;
@WebServlet(urlPatterns={"/product","/product/detail"})
public class ProductController extends HttpServlet {
    private static final long serialVersionUID=1L;
    private final ProductService service=new ProductServiceImpl();
    public static int page(String value,long totalPages){
        long p=1;try{p=Long.parseLong(value);}catch(NumberFormatException e){/* First page. */}
        return (int)Math.max(1,Math.min(Math.min(Math.max(1,totalPages),357913942L),p));
    }
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        try {
            if("/product/detail".equals(req.getServletPath())){
                int id=Integer.parseInt(req.getParameter("id"));
                if(id<1){resp.sendError(400,"ID sản phẩm không hợp lệ");return;}
                Product p=service.findById(id);if(p==null){resp.sendError(404,"Không tìm thấy sản phẩm");return;}
                req.setAttribute("product",p);req.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(req,resp);return;
            }
            long count=service.count(),totalPages=Math.max(1,(count+5)/6);
            int page=page(req.getParameter("page"),totalPages);
            req.setAttribute("products",service.findByPage(page,6));req.setAttribute("page",page);req.setAttribute("totalPages",totalPages);
            req.getRequestDispatcher("/WEB-INF/views/product-list.jsp").forward(req,resp);
        }catch(NumberFormatException e){resp.sendError(400,"ID sản phẩm không hợp lệ");}
        catch(RuntimeException e){resp.sendError(503,"Không thể tải sản phẩm. Vui lòng kiểm tra database hoặc thử lại sau.");}
    }
}
