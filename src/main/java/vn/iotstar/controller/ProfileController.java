package vn.iotstar.controller;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.model.User;
import vn.iotstar.service.UserProfileService;
import vn.iotstar.service.impl.UserProfileServiceImpl;
import vn.iotstar.util.Constant;
import vn.iotstar.util.ImageUpload;

@WebServlet("/profile")
@MultipartConfig(fileSizeThreshold=1024*1024,maxFileSize=5*1024*1024,maxRequestSize=10*1024*1024)
public class ProfileController extends HttpServlet {
    private static final long serialVersionUID=1L;
    private final UserProfileService service;
    public ProfileController(){this(new UserProfileServiceImpl());}
    public ProfileController(UserProfileService service){this.service=service;}
    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        resp.setHeader("Cache-Control","no-store");
        try{
            User current=loadCurrent(req,resp);if(current==null)return;
            HttpSession session=req.getSession(false);
            session.setAttribute(Constant.SESSION_ACCOUNT,current);
            if(session.getAttribute("profileCsrf")==null)session.setAttribute("profileCsrf",UUID.randomUUID().toString());
            req.setAttribute("profileFullname",current.getFullName());req.setAttribute("profilePhone",current.getPhone());
            req.setAttribute("message",session.getAttribute("profileMessage"));session.removeAttribute("profileMessage");
            show(req,resp,current);
        }catch(IllegalStateException e){resp.sendError(503,"Không thể tải hồ sơ. Vui lòng thử lại sau.");}
    }
    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");resp.setHeader("Cache-Control","no-store");
        User current=null;String uploaded=null;boolean saved=false;
        try{
            current=loadCurrent(req,resp);if(current==null)return;
            HttpSession session=req.getSession(false);
            if(session.getAttribute("profileCsrf")==null||!session.getAttribute("profileCsrf").equals(req.getParameter("csrf"))){
                resp.sendError(403,"Phiên làm việc không hợp lệ. Vui lòng mở lại trang hồ sơ.");return;
            }
            String fullname=req.getParameter("fullname"),phone=req.getParameter("phone");
            req.setAttribute("profileFullname",fullname);req.setAttribute("profilePhone",phone);
            service.validate(fullname,phone);
            Part avatar=req.getPart("avatar");
            if(avatar!=null&&avatar.getSize()>0)uploaded=ImageUpload.save(avatar,"avatar");
            // Identity comes exclusively from the authenticated session, never from form fields.
            User latest=service.updateProfile(current.getId(),fullname,phone,uploaded);
            if(latest==null){session.invalidate();resp.sendRedirect(req.getContextPath()+"/login");return;}
            saved=true;
            session.setAttribute(Constant.SESSION_ACCOUNT,latest);
            session.setAttribute("profileMessage","Cập nhật hồ sơ thành công.");
            resp.sendRedirect(req.getContextPath()+"/profile");
        }catch(IllegalArgumentException e){
            req.setAttribute("alert",e.getMessage());show(req,resp,current);
        }catch(Exception e){
            if(current==null){resp.sendError(503,"Không thể tải hồ sơ. Vui lòng thử lại sau.");return;}
            req.setAttribute("alert","Không thể lưu hồ sơ. Kiểm tra số điện thoại và ảnh hợp lệ, tối đa 5 MB, rồi thử lại.");
            show(req,resp,current);
        }finally{if(!saved)ImageUpload.discardNewAvatar(uploaded);}
    }
    private User loadCurrent(HttpServletRequest req,HttpServletResponse resp)throws IOException {
        HttpSession session=req.getSession(false);
        if(session==null||!(session.getAttribute(Constant.SESSION_ACCOUNT) instanceof User account)){
            resp.sendRedirect(req.getContextPath()+"/login");return null;
        }
        User user=service.findById(account.getId());
        if(user==null){session.invalidate();resp.sendRedirect(req.getContextPath()+"/login");}
        return user;
    }
    private void show(HttpServletRequest req, HttpServletResponse resp, User profile)
            throws ServletException, IOException {

        req.setAttribute("profile", profile);
        resp.setContentType("text/html; charset=UTF-8");

        req.getRequestDispatcher("/WEB-INF/views/profile.jsp")
                .forward(req, resp);
    }
}
