package vn.iotstar.controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.service.UserService;
import vn.iotstar.service.impl.UserServiceImpl;

@WebServlet("/verify-otp")
public class VerifyOtpController extends HttpServlet {
    private static final long serialVersionUID=1L;
    private final UserService service=new UserServiceImpl();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        resp.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession(false);
        if(session==null||session.getAttribute("pendingActivation")==null){resp.sendRedirect(req.getContextPath()+"/login?activation=1");return;}
        req.setAttribute("message",session.getAttribute("otpMessage"));session.removeAttribute("otpMessage");
        req.setAttribute("alert",session.getAttribute("otpError"));session.removeAttribute("otpError");
        req.getRequestDispatcher("/views/verify-otp.jsp").forward(req,resp);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session=req.getSession(false);
        if(session==null||session.getAttribute("pendingActivation")==null){resp.sendRedirect(req.getContextPath()+"/login?activation=1");return;}
        String username=(String)session.getAttribute("pendingActivation");
        try {
            if("resend".equals(req.getParameter("action"))){
                service.sendActivationOtp(username);
                session.setAttribute("otpMessage","Đã gửi OTP mới. Mã cũ không còn hiệu lực; mã mới có hạn 5 phút.");
            } else {
                String result=service.verifyOtp(username,req.getParameter("otp"));
                if("SUCCESS".equals(result)||"ACTIVE".equals(result)){
                    session.removeAttribute("pendingActivation");session.removeAttribute("otpMessage");session.removeAttribute("otpError");
                    resp.sendRedirect(req.getContextPath()+"/login?activated=1");return;
                }
                String message=switch(result){
                    case "FORMAT" -> "Vui lòng nhập OTP gồm đúng 6 chữ số.";
                    case "EXPIRED" -> "OTP đã hết hạn hoặc chưa được gửi. Vui lòng gửi lại OTP.";
                    case "LOCKED" -> "Đã nhập sai 5 lần. Vui lòng gửi lại OTP.";
                    default -> "OTP không đúng. Vui lòng kiểm tra lại (tối đa 5 lần).";
                };
                session.setAttribute("otpError",message);
            }
        } catch(IllegalArgumentException | IllegalStateException e){session.setAttribute("otpError",e.getMessage());}
        resp.sendRedirect(req.getContextPath()+"/verify-otp");
    }
}
