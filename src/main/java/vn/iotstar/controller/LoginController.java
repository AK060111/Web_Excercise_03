package vn.iotstar.controller;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.model.User;
import vn.iotstar.service.UserService;
import vn.iotstar.service.impl.UserServiceImpl;
import vn.iotstar.util.Constant;

@WebServlet("/login")
public class LoginController extends HttpServlet {
    private final UserService service;
    public LoginController(){this(new UserServiceImpl());}
    public LoginController(UserService service){this.service=service;}
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        resp.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession(false); if(session!=null&&session.getAttribute(Constant.SESSION_ACCOUNT)!=null){resp.sendRedirect(req.getContextPath()+"/waiting");return;}
        clearLegacyCookie(req,resp);
        req.getRequestDispatcher(Constant.LOGIN).forward(req,resp);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        resp.setHeader("Cache-Control","no-store");
        req.setCharacterEncoding("UTF-8");String username=vn.iotstar.util.ValidationUtil.trim(req.getParameter("username"));String password=req.getParameter("password");
        req.setAttribute("loginUsername",username);
        if(blank(username)||blank(password)||username.length()>100||password.length()>255){show(req,resp,"Vui lòng nhập tài khoản và mật khẩu hợp lệ.");return;}
        User user;
        try { user=service.login(username,password); }
        catch(IllegalArgumentException e){
            req.getSession(true).setAttribute("pendingActivation",username);
            req.setAttribute("activationRequired",true);
            show(req,resp,e.getMessage());return;
        }
        catch(IllegalStateException e){show(req,resp,"Chưa thể đăng nhập. Vui lòng thử lại sau.");return;}
        if(user==null){show(req,resp,"Tài khoản hoặc mật khẩu không đúng");return;}
        req.getSession(true);req.changeSessionId();
        req.getSession(true).setAttribute(Constant.SESSION_ACCOUNT,user);
        clearLegacyCookie(req,resp);
        resp.sendRedirect(req.getContextPath()+"/waiting");
    }
    private void clearLegacyCookie(HttpServletRequest req,HttpServletResponse resp){Cookie cookie=new Cookie(Constant.COOKIE_REMEMBER,"");cookie.setMaxAge(0);cookie.setPath(req.getContextPath().isEmpty()?"/":req.getContextPath());resp.addCookie(cookie);}
    private boolean blank(String v){return v==null||v.trim().isEmpty();} private void show(HttpServletRequest req,HttpServletResponse resp,String message)throws ServletException,IOException{req.setAttribute("alert",message);req.getRequestDispatcher(Constant.LOGIN).forward(req,resp);}
}
