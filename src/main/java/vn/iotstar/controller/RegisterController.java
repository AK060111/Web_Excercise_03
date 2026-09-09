package vn.iotstar.controller;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.service.UserService;
import vn.iotstar.service.impl.UserServiceImpl;
import vn.iotstar.util.Constant;

@WebServlet("/register")
public class RegisterController extends HttpServlet {
    private final UserService service=new UserServiceImpl();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        if(req.getSession(false)!=null&&req.getSession(false).getAttribute(Constant.SESSION_ACCOUNT)!=null){resp.sendRedirect(req.getContextPath()+"/waiting");return;}
        req.getRequestDispatcher(Constant.REGISTER).forward(req,resp);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");
        String username=trim(req.getParameter("username")),password=req.getParameter("password"),
            email=trim(req.getParameter("email")),fullname=trim(req.getParameter("fullname")),phone=trim(req.getParameter("phone"));
        req.setAttribute("registerUsername",username);req.setAttribute("registerEmail",email);
        req.setAttribute("registerFullname",fullname);req.setAttribute("registerPhone",phone);
        if(username.isEmpty()||password==null||password.isBlank()||email.isEmpty()||fullname.isEmpty()){
            show(req,resp,"Vui lòng nhập đủ các trường bắt buộc");return;
        }
        if(username.length()>100||password.length()>255||email.length()>255||phone.length()>30){
            show(req,resp,"Thông tin nhập vượt quá độ dài cho phép");return;
        }
        try {fullname=vn.iotstar.util.ValidationUtil.fullname(fullname);req.setAttribute("registerFullname",fullname);vn.iotstar.util.ValidationUtil.email(email);vn.iotstar.util.ValidationUtil.password(password);vn.iotstar.util.ValidationUtil.phone(phone);}
        catch(IllegalArgumentException e){show(req,resp,e.getMessage());return;}
        try {
            if(service.checkExistEmail(email)){show(req,resp,"Email đã tồn tại!");return;}
            if(service.checkExistUsername(username)){show(req,resp,"Tài khoản đã tồn tại!");return;}
            if(!phone.isEmpty()&&service.checkExistPhone(phone)){show(req,resp,"Số điện thoại đã tồn tại!");return;}
            if(!service.register(username,password,email,fullname,phone)){show(req,resp,"Thông tin đăng ký đã tồn tại");return;}
        } catch(IllegalStateException e){show(req,resp,"Không thể đăng ký. Vui lòng kiểm tra thông tin hoặc liên hệ quản trị viên.");return;}
        HttpSession session=req.getSession(true);
        session.setAttribute("pendingActivation",username);
        try {
            service.sendActivationOtp(username);
            session.setAttribute("otpMessage","Đã gửi OTP tới email đăng ký. Mã có hiệu lực 5 phút.");
        } catch(IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("otpError","Tài khoản đã được tạo nhưng chưa gửi được OTP. Kiểm tra cấu hình email và thử gửi lại sau 60 giây.");
        }
        resp.sendRedirect(req.getContextPath()+"/verify-otp");
    }
    private String trim(String v){return v==null?"":v.trim();}
    private void show(HttpServletRequest req,HttpServletResponse resp,String m)throws ServletException,IOException{
        req.setAttribute("alert",m);req.getRequestDispatcher(Constant.REGISTER).forward(req,resp);
    }
}
