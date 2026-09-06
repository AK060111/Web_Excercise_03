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
    private final UserService service=new UserServiceImpl();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        HttpSession session=req.getSession(false); if(session!=null&&session.getAttribute(Constant.SESSION_ACCOUNT)!=null){resp.sendRedirect(req.getContextPath()+"/waiting");return;}
        Cookie[] cookies=req.getCookies();if(cookies!=null)for(Cookie c:cookies)if(Constant.COOKIE_REMEMBER.equals(c.getName())){User account=service.get(c.getValue());if(account!=null&&account.isActive()){req.getSession(true).setAttribute(Constant.SESSION_ACCOUNT,account);resp.sendRedirect(req.getContextPath()+"/waiting");return;}}
        req.getRequestDispatcher(Constant.LOGIN).forward(req,resp);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");String username=req.getParameter("username");String password=req.getParameter("password");
        if(blank(username)||blank(password)){show(req,resp,"Tài khoản hoặc mật khẩu không được rỗng");return;}
        User user;
        try { user=service.login(username,password); }
        catch(IllegalArgumentException e){
            req.getSession(true).setAttribute("pendingActivation",username);
            req.setAttribute("activationRequired",true);
            show(req,resp,e.getMessage());return;
        }
        if(user==null){show(req,resp,"Tài khoản hoặc mật khẩu không đúng");return;}
        req.getSession(true).setAttribute(Constant.SESSION_ACCOUNT,user);
        if("on".equals(req.getParameter("remember"))){Cookie cookie=new Cookie(Constant.COOKIE_REMEMBER,username);cookie.setMaxAge(30*60);cookie.setPath(req.getContextPath().isEmpty()?"/":req.getContextPath());resp.addCookie(cookie);}
        resp.sendRedirect(req.getContextPath()+"/waiting");
    }
    private boolean blank(String v){return v==null||v.trim().isEmpty();} private void show(HttpServletRequest req,HttpServletResponse resp,String message)throws ServletException,IOException{req.setAttribute("alert",message);req.getRequestDispatcher(Constant.LOGIN).forward(req,resp);}
}
