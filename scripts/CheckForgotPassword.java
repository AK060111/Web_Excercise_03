import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import vn.iotstar.connection.DBConnection;
import vn.iotstar.dao.impl.UserDaoImpl;
import vn.iotstar.service.MailService;
import vn.iotstar.service.impl.UserServiceImpl;
import vn.iotstar.controller.ForgotPasswordController;

/** Development-only JDBC/controller regression checks. No migration or real email. */
public class CheckForgotPassword {
    static int checks;
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
    static String token(){return UUID.randomUUID().toString()+UUID.randomUUID();}
    static class FakeMail extends MailService {
        String code; int sends; boolean fail;
        public void sendOtp(String email,String otp){code=otp;sends++;}
        public void sendResetOtp(String email,String otp){if(fail)throw new IllegalStateException("simulated");code=otp;sends++;}
    }
    static void expiry(Connection cn,String username,int seconds)throws Exception {
        try(PreparedStatement ps=cn.prepareStatement("UPDATE dbo.[User] SET otpExpiry=DATEADD(SECOND,?,SYSUTCDATETIME()) WHERE username=?")){
            ps.setInt(1,seconds);ps.setString(2,username);ps.executeUpdate();
        }
    }
    static class Controller extends ForgotPasswordController {
        Controller(UserServiceImpl service){super(service);}
        void call(boolean post,HttpServletRequest req,HttpServletResponse resp)throws Exception{if(post)super.doPost(req,resp);else super.doGet(req,resp);}
    }
    static class Http {
        Map<String,Object> session=new HashMap<>(),attributes=new HashMap<>();
        Map<String,String> params=new HashMap<>();String path,redirect,view;int status;boolean invalid;
        List<Cookie> cookies=new ArrayList<>();
        HttpSession sess=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{
            switch(m.getName()){
                case "getAttribute":return session.get(a[0]);
                case "setAttribute":session.put((String)a[0],a[1]);return null;
                case "removeAttribute":session.remove(a[0]);return null;
                case "invalidate":session.clear();invalid=true;return null;
                default:return null;
            }
        });
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            switch(m.getName()){
                case "getSession":return invalid?null:sess;
                case "getServletPath":return path;
                case "getContextPath":return "/ServletCRUDMVC";
                case "getParameter":return params.get(a[0]);
                case "changeSessionId":return "rotated";
                case "setAttribute":attributes.put((String)a[0],a[1]);return null;
                case "getRequestDispatcher":view=(String)a[0];return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{RequestDispatcher.class},(x,y,z)->null);
                default:return null;
            }
        });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            switch(m.getName()){
                case "sendRedirect":redirect=(String)a[0];break;
                case "sendError":status=(Integer)a[0];break;
                case "addCookie":cookies.add((Cookie)a[0]);break;
            }return null;
        });
        void call(Controller c,boolean post,String path,Map<String,String> params)throws Exception {
            this.path=path;this.params=new HashMap<>(params);redirect=null;view=null;status=200;attributes.clear();c.call(post,req,resp);
        }
        Map<String,String> form(String...pairs){Map<String,String> result=new HashMap<>();result.put("csrf",(String)session.get("forgotCsrf"));for(int i=0;i<pairs.length;i+=2)result.put(pairs[i],pairs[i+1]);return result;}
    }
    public static void main(String[] args)throws Exception {
        try(Connection real=new DBConnection().getConnection()) {
            real.setAutoCommit(false);
            try {
                Connection borrowed=(Connection)Proxy.newProxyInstance(CheckForgotPassword.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                    if(Set.of("close","commit","rollback","setAutoCommit").contains(m.getName()))return null;
                    try{return m.invoke(real,a);}catch(InvocationTargetException e){throw e.getCause();}
                });
                UserDaoImpl dao=new UserDaoImpl(){public Connection getConnection(){return borrowed;}};
                FakeMail mail=new FakeMail();UserServiceImpl service=new UserServiceImpl(dao,mail);
                String name="reset_test_"+UUID.randomUUID().toString().replace("-","");
                String email=name+"@example.invalid",oldPassword=token(),newPassword=token();
                String phone="09"+String.format(java.util.Locale.ROOT,"%08d",new java.security.SecureRandom().nextInt(100000000));
                check(service.register(name,oldPassword,email,"Test",phone),"register inactive fixture");
                service.sendActivationOtp(name);String activation=mail.code;
                int sent=mail.sends;service.sendResetOtp(email);
                check(mail.sends==sent && "INVALID".equals(service.verifyResetOtp(email,activation,token())),"reset cannot consume activation OTP");
                try{service.login(name,oldPassword);throw new AssertionError("inactive login");}catch(IllegalArgumentException expected){check(true,"inactive login remains blocked");}
                check("SUCCESS".equals(service.verifyOtp(name,activation)),"activation regression");
                service.sendResetOtp(email);String first=mail.code;String grant=token();
                check(first.matches("[0-9]{6}") && dao.get(name).getOtp().length()==64 && !dao.get(name).getOtp().equals(first),"six-digit OTP stored as SHA-256");
                check(!service.resetPassword(email,grant,newPassword),"no reset before verification");
                sent=mail.sends;service.sendResetOtp(email);check(sent==mail.sends,"resend before 60 seconds blocked");
                check("ACTIVE".equals(service.verifyOtp(name,first)) && !service.resetPassword(email,grant,newPassword),"activation endpoint cannot authorize reset");
                check("FORMAT".equals(service.verifyResetOtp(email,"abc",grant)),"OTP format");
                String wrong=first.equals("000000")?"000001":"000000";
                for(int i=0;i<5;i++)check("WRONG".equals(service.verifyResetOtp(email,wrong,grant)),"wrong attempt "+(i+1));
                check("LOCKED".equals(service.verifyResetOtp(email,first,grant)),"five wrong attempts block correct OTP");
                expiry(real,name,230);service.sendResetOtp(email);check(mail.sends==sent+1 && dao.get(name).getOtpAttempts()==0,"resend after cooldown resets attempts");
                check("WRONG".equals(service.verifyResetOtp(email,first,grant)),"old OTP invalid after resend");
                expiry(real,name,-1);check("EXPIRED".equals(service.verifyResetOtp(email,mail.code,grant)),"expired OTP");
                service.sendResetOtp(email);
                check("SUCCESS".equals(service.verifyResetOtp(email,mail.code,grant)),"correct reset OTP verified");
                check("LOCKED".equals(service.verifyResetOtp(email,mail.code,token())),"OTP consumed by verification");
                check(!service.resetPassword(email,token(),newPassword),"wrong grant rejected");
                expiry(real,name,-1);check(!service.resetPassword(email,grant,newPassword),"expired grant rejected");
                service.sendResetOtp(email);grant=token();service.verifyResetOtp(email,mail.code,grant);
                expiry(real,name,230);service.sendResetOtp(email);check(!service.resetPassword(email,grant,newPassword),"resend revokes verified grant");
                sent=mail.sends;service.sendResetOtp("missing-"+email);check(mail.sends==sent,"unknown email sends no mail");
                Controller controller=new Controller(service);Http http=new Http();
                http.call(controller,false,"/reset-password",Map.of());check(http.redirect.endsWith("/forgot-password"),"GET reset URL without verification");
                http.call(controller,true,"/reset-password",Map.of("password",newPassword));check(http.status==403,"missing CSRF rejected");
                http.call(controller,true,"/reset-password",http.form("password",newPassword,"confirmPassword",newPassword));check(http.redirect.endsWith("/forgot-password"),"POST reset without verification");
                http.call(controller,true,"/forgot-password",http.form("email","missing-"+email));
                http.call(controller,false,"/forgot-password/verify",Map.of());Object notice=http.attributes.get("message");
                http.session.remove("forgotSentAt");expiry(real,name,-1);
                http.call(controller,true,"/forgot-password",http.form("email",email));
                http.call(controller,false,"/forgot-password/verify",Map.of());check(notice.equals(http.attributes.get("message")),"same public notice for existing and unknown email");
                sent=mail.sends;http.call(controller,true,"/forgot-password/verify",http.form("action","resend"));check(sent==mail.sends,"controller resend cooldown");
                http.call(controller,true,"/forgot-password/verify",http.form("otp",mail.code));check(http.redirect.endsWith("/reset-password"),"controller verification grants reset");
                http.call(controller,true,"/reset-password",http.form("password",newPassword,"confirmPassword","mismatch"));check(http.view.endsWith("reset-password.jsp") && service.login(name,oldPassword)!=null,"password confirmation mismatch preserves password");
                http.call(controller,true,"/reset-password",http.form("password",newPassword,"confirmPassword",newPassword));
                check(http.redirect.endsWith("/login?reset=1")&&http.invalid&&http.session.isEmpty(),"successful reset invalidates session");
                check(http.cookies.stream().anyMatch(c->c.getMaxAge()==0),"reset clears current remember cookie");
                check(dao.get(name).getOtp()==null&&dao.get(name).getOtpExpiry()==null&&dao.get(name).getOtpAttempts()==0,"reset clears OTP state");
                check(service.login(name,newPassword)!=null,"new password login");check(service.login(name,oldPassword)==null,"old password rejected");
                check(!service.resetPassword(email,grant,oldPassword),"grant cannot be reused");
                http.call(controller,true,"/reset-password",Map.of());check(http.status==403,"reset replay after session invalidation");
                mail.fail=true;Http failed=new Http();failed.call(controller,false,"/forgot-password",Map.of());
                failed.call(controller,true,"/forgot-password",failed.form("email",email));
                failed.call(controller,false,"/forgot-password/verify",Map.of());check(notice.equals(failed.attributes.get("message")),"SMTP failure exposes no exception");
                System.out.println("CHECKS PASSED: "+checks);
            } finally {real.rollback();System.out.println("Test inserts/updates rolled back. No migration or email performed.");}
        } catch(Exception e){System.err.println("Test failed; sensitive details suppressed.");System.exit(1);}
    }
}
