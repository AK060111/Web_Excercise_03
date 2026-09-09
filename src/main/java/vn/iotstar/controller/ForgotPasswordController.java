package vn.iotstar.controller;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.iotstar.service.UserService;
import vn.iotstar.service.impl.UserServiceImpl;
import vn.iotstar.util.Constant;

@WebServlet(urlPatterns={"/forgot-password","/forgot-password/verify","/reset-password"})
public class ForgotPasswordController extends HttpServlet {
    private static final long serialVersionUID=1L;
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final String EMAIL="forgotEmail", GRANT="resetGrant", UNTIL="resetUntil";
    private static final String CSRF="forgotCsrf", SENT="forgotSentAt", MESSAGE="forgotMessage";
    private static final String NOTICE="Nếu email thuộc tài khoản đã kích hoạt, hệ thống sẽ gửi OTP. "
        +"Vui lòng kiểm tra hộp thư/Spam và chờ ít nhất 60 giây trước khi gửi lại. "
        +"Nếu chưa nhận được thư, hãy thử lại sau hoặc liên hệ quản trị viên.";
    private final UserService service;
    public ForgotPasswordController(){this(new UserServiceImpl());}
    public ForgotPasswordController(UserService service){this.service=service;}

    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        resp.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession(true);
        if(session.getAttribute(CSRF)==null)session.setAttribute(CSRF,token());
        String path=req.getServletPath();
        if("/reset-password".equals(path)){
            if(!verified(session)){clearGrant(session);resp.sendRedirect(req.getContextPath()+"/forgot-password");return;}
            render(req,resp,"reset-password");
        } else if("/forgot-password/verify".equals(path)){
            if(session.getAttribute(EMAIL)==null){resp.sendRedirect(req.getContextPath()+"/forgot-password");return;}
            req.setAttribute("message",session.getAttribute(MESSAGE));session.removeAttribute(MESSAGE);
            render(req,resp,"forgot-password-verify");
        } else render(req,resp,"forgot-password");
    }

    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8");resp.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession(false);
        if(session==null || session.getAttribute(CSRF)==null || !session.getAttribute(CSRF).equals(req.getParameter("csrf"))){
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Phiên làm việc không hợp lệ. Vui lòng mở lại trang quên mật khẩu.");return;
        }
        switch(req.getServletPath()){
            case "/forgot-password" -> requestOtp(req,resp,session);
            case "/forgot-password/verify" -> verify(req,resp,session);
            case "/reset-password" -> reset(req,resp,session);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    private void requestOtp(HttpServletRequest req,HttpServletResponse resp,HttpSession session)throws ServletException,IOException {
        String email=req.getParameter("email"); email=email==null?"":email.trim();
        req.setAttribute("forgotEmailInput",email);
        try {
            email=vn.iotstar.util.ValidationUtil.email(email);
        } catch(Exception e){req.setAttribute("alert","Vui lòng nhập email hợp lệ.");render(req,resp,"forgot-password");return;}
        clearGrant(session);
        session.setAttribute(EMAIL,email);
        send(session,email);
        resp.sendRedirect(req.getContextPath()+"/forgot-password/verify");
    }
    private void send(HttpSession session,String email){
        Long sent=(Long)session.getAttribute(SENT);
        long now=System.currentTimeMillis();
        if(sent==null || now-sent>=60000){
            session.setAttribute(SENT,now);
            try{service.sendResetOtp(email);}catch(IllegalStateException e){/* Same public result; no sensitive SMTP exception. */}
        }
        session.setAttribute(MESSAGE,NOTICE);
    }
    private void verify(HttpServletRequest req,HttpServletResponse resp,HttpSession session)throws ServletException,IOException {
        String email=(String)session.getAttribute(EMAIL);
        if(email==null){resp.sendRedirect(req.getContextPath()+"/forgot-password");return;}
        if("resend".equals(req.getParameter("action"))){
            clearGrant(session);send(session,email);
            resp.sendRedirect(req.getContextPath()+"/forgot-password/verify");return;
        }
        clearGrant(session);
        String grant=token();
        String result;
        try{result=service.verifyResetOtp(email,req.getParameter("otp"),grant);}
        catch(IllegalStateException e){result="INVALID";}
        if("SUCCESS".equals(result)){
            req.changeSessionId();
            session.setAttribute(GRANT,grant);session.setAttribute(UNTIL,System.currentTimeMillis()+300000);
            session.setAttribute(CSRF,token());
            resp.sendRedirect(req.getContextPath()+"/reset-password");return;
        }
        req.setAttribute("alert","FORMAT".equals(result)?"Vui lòng nhập OTP gồm đúng 6 chữ số.":
            "OTP không đúng, đã hết hạn hoặc đã vượt quá 5 lần thử. Vui lòng kiểm tra mã hoặc gửi lại OTP sau 60 giây.");
        render(req,resp,"forgot-password-verify");
    }
    private void reset(HttpServletRequest req,HttpServletResponse resp,HttpSession session)throws ServletException,IOException {
        if(!verified(session)){clearGrant(session);resp.sendRedirect(req.getContextPath()+"/forgot-password");return;}
        String password=req.getParameter("password"),confirm=req.getParameter("confirmPassword");
        try{vn.iotstar.util.ValidationUtil.password(password);}
        catch(IllegalArgumentException e){req.setAttribute("alert",e.getMessage());render(req,resp,"reset-password");return;}
        if(!password.equals(confirm)){req.setAttribute("alert","Xác nhận mật khẩu không khớp.");render(req,resp,"reset-password");return;}
        boolean success;
        try{success=service.resetPassword((String)session.getAttribute(EMAIL),(String)session.getAttribute(GRANT),password);}
        catch(IllegalStateException e){
            req.setAttribute("alert","Chưa thể đặt lại mật khẩu. Vui lòng thử lại hoặc yêu cầu OTP mới.");render(req,resp,"reset-password");return;
        }
        if(!success){clearGrant(session);session.setAttribute(MESSAGE,"Quyền đặt lại mật khẩu đã hết hạn hoặc không còn hiệu lực. Vui lòng yêu cầu OTP mới.");resp.sendRedirect(req.getContextPath()+"/forgot-password/verify");return;}
        session.invalidate();
        Cookie remember=new Cookie(Constant.COOKIE_REMEMBER,"");remember.setMaxAge(0);
        remember.setPath(req.getContextPath().isEmpty()?"/":req.getContextPath());resp.addCookie(remember);
        resp.sendRedirect(req.getContextPath()+"/login?reset=1");
    }
    private boolean verified(HttpSession session){
        Long until=(Long)session.getAttribute(UNTIL);
        return session.getAttribute(EMAIL)!=null && session.getAttribute(GRANT)!=null && until!=null && until>System.currentTimeMillis();
    }
    private void clearGrant(HttpSession session){session.removeAttribute(GRANT);session.removeAttribute(UNTIL);}
    private static String token(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
    private void render(HttpServletRequest req,HttpServletResponse resp,String view)throws ServletException,IOException {
        req.getRequestDispatcher("/WEB-INF/views/"+view+".jsp").forward(req,resp);
    }
}
