package vn.iotstar.controller;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import vn.iotstar.model.*;
import vn.iotstar.service.*;
import vn.iotstar.service.impl.*;
import vn.iotstar.util.ImageUpload;
@WebServlet(urlPatterns={"/admin/product","/admin/product/list","/admin/product/add","/admin/product/edit","/admin/product/delete"})
@MultipartConfig(fileSizeThreshold=1024*1024,maxFileSize=5*1024*1024,maxRequestSize=10*1024*1024)
public class ProductAdminController extends HttpServlet {
    private static final long serialVersionUID=1L;
    private final ProductService service=new ProductServiceImpl();
    private final CategoryService categories=new CategoryServiceImpl();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        HttpSession session=req.getSession();
        if(session.getAttribute("productCsrf")==null)session.setAttribute("productCsrf",UUID.randomUUID().toString());
        String path=req.getServletPath();
        try {
            if(path.endsWith("/delete")){resp.sendError(405,"Vui lòng dùng nút Xóa trong danh sách.");return;}
            if(path.endsWith("/add")){req.setAttribute("product",new Product());form(req,resp,false);return;}
            if(path.endsWith("/edit")){
                Product p=service.findById(id(req));if(p==null){resp.sendError(404,"Sản phẩm không tồn tại");return;}
                req.setAttribute("product",p);form(req,resp,true);return;
            }
            req.setAttribute("products",service.findAll());req.getRequestDispatcher("/WEB-INF/views/admin/product-list.jsp").forward(req,resp);
        }catch(IllegalArgumentException e){resp.sendError(400,"ID sản phẩm không hợp lệ");}
        catch(RuntimeException e){resp.sendError(503,"Không thể tải sản phẩm. Kiểm tra bảng products và kết nối database.");}
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");String path=req.getServletPath();
        boolean edit=path.endsWith("/edit");String uploaded=null;
        try {
            HttpSession session=req.getSession(false);
            if(session==null||session.getAttribute("productCsrf")==null||!session.getAttribute("productCsrf").equals(req.getParameter("csrf"))){resp.sendError(403,"Phiên làm việc không hợp lệ");return;}
            if(path.endsWith("/delete")){
                if(!service.delete(id(req))){resp.sendError(404,"Sản phẩm không tồn tại");return;}
                resp.sendRedirect(req.getContextPath()+"/admin/product/list?done=1");return;
            }
            if(!edit&&!path.endsWith("/add")){resp.sendError(405);return;}
            Product old=new Product();
            if(edit){old=service.findById(id(req));if(old==null){resp.sendError(404,"Sản phẩm không tồn tại");return;}}
            Product input=new Product();input.setId(old.getId());input.setName(req.getParameter("name"));input.setDescription(req.getParameter("description"));
            req.setAttribute("product",input);req.setAttribute("oldImage",old.getImage());
            try {input.setPrice(new BigDecimal(req.getParameter("price")));Category c=new Category();c.setId(Integer.parseInt(req.getParameter("categoryId")));input.setCategory(c);}
            catch(NumberFormatException|NullPointerException e){throw new IllegalArgumentException("Giá hoặc danh mục không hợp lệ.");}
            service.validate(input);
            Part image=req.getPart("image");if(image!=null&&image.getSize()>0){uploaded=ImageUpload.save(image,"product");input.setImage(uploaded);}
            if(edit){if(!service.update(input)){ImageUpload.discardNewProductImage(uploaded);resp.sendError(404,"Sản phẩm không còn tồn tại");return;}}
            else service.insert(input);
            resp.sendRedirect(req.getContextPath()+"/admin/product/list?done=1");
        }catch(IllegalArgumentException e){
            ImageUpload.discardNewProductImage(uploaded);
            if(req.getAttribute("product")==null){resp.sendError(400,"ID sản phẩm không hợp lệ");return;}
            req.setAttribute("alert",e.getMessage());form(req,resp,edit);
        }catch(Exception e){ImageUpload.discardNewProductImage(uploaded);resp.sendError(400,"Không thể lưu thay đổi. Kiểm tra dữ liệu, ảnh tối đa 5 MB và danh mục còn tồn tại.");}
    }
    private int id(HttpServletRequest req){int id=Integer.parseInt(req.getParameter("id"));if(id<1)throw new IllegalArgumentException();return id;}
    private void form(HttpServletRequest req,HttpServletResponse resp,boolean edit)throws ServletException,IOException {
        req.setAttribute("categories",categories.findAll());
        req.getRequestDispatcher("/WEB-INF/views/admin/"+(edit?"edit-product":"add-product")+".jsp").forward(req,resp);
    }
}
