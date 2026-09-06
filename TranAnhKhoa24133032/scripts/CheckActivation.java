import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import vn.iotstar.connection.DBConnection;
import vn.iotstar.dao.impl.UserDaoImpl;
import vn.iotstar.model.User;
import vn.iotstar.service.MailService;
import vn.iotstar.service.impl.UserServiceImpl;

/** Real JDBC checks, with all schema/data changes rolled back. Never sends email. */
public class CheckActivation {
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS: "+label);}
    static class FakeMail extends MailService {
        String code; boolean fail;
        public void sendOtp(String email,String otp){code=otp;if(fail)throw new IllegalStateException("Simulated SMTP failure");}
    }
    public static void main(String[] args) throws Exception {
        try(Connection real=new DBConnection().getConnection()) {
            real.setAutoCommit(false);
            try {
                String migration=Files.readString(Path.of("database/project02_otp_update.sql"));
                migration=migration.replaceAll("(?im)^\\s*(USE .*|GO|SET XACT_ABORT ON;|BEGIN TRANSACTION;|COMMIT TRANSACTION;)\\s*$", "");
                try(Statement st=real.createStatement()){st.execute(migration);st.execute(migration);}
                check(true,"migration can run twice");
                Connection borrowed=(Connection)Proxy.newProxyInstance(CheckActivation.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,values)->{
                    if(java.util.Set.of("close","commit","rollback","setAutoCommit").contains(method.getName())) return null;
                    try{return method.invoke(real,values);}catch(InvocationTargetException e){throw e.getCause();}
                });
                UserDaoImpl dao=new UserDaoImpl(){public Connection getConnection(){return borrowed;}};
                FakeMail mail=new FakeMail(); UserServiceImpl service=new UserServiceImpl(dao,mail);
                String name="otp_test_"+java.util.UUID.randomUUID().toString().replace("-","");
                String password=java.util.UUID.randomUUID().toString();
                check(service.register(name,password,name+"@example.invalid","OTP test",name.substring(0,25)),"register");
                check(!dao.get(name).isActive(),"new account inactive");
                check(service.login(name,"wrong")==null,"wrong password rejected");
                try{service.login(name,password);throw new AssertionError("inactive login allowed");}catch(IllegalArgumentException expected){check(true,"inactive login blocked");}
                service.sendActivationOtp(name);
                String first=mail.code;
                check(first.matches("[0-9]{6}") && !first.equals(dao.get(name).getOtp()) && dao.get(name).getOtp().length()==64,"six-digit OTP stored as digest");
                try{service.sendActivationOtp(name);throw new AssertionError("cooldown bypassed");}catch(IllegalArgumentException expected){check(true,"resend cooldown");}
                check("FORMAT".equals(service.verifyOtp(name,"bad")),"invalid format");
                String wrong=first.equals("000000")?"000001":"000000";
                for(int i=0;i<5;i++)check("WRONG".equals(service.verifyOtp(name,wrong)),"wrong OTP attempt "+(i+1));
                check("LOCKED".equals(service.verifyOtp(name,first)),"five-attempt limit");
                try(PreparedStatement ps=real.prepareStatement("UPDATE dbo.[User] SET otpExpiry=DATEADD(SECOND,-1,SYSUTCDATETIME()),otpAttempts=0 WHERE username=?")){ps.setString(1,name);ps.executeUpdate();}
                check("EXPIRED".equals(service.verifyOtp(name,first)),"expired OTP");
                mail.fail=true;
                try{service.sendActivationOtp(name);throw new AssertionError("SMTP failure missing");}catch(IllegalStateException expected){check(!dao.get(name).isActive(),"SMTP failure leaves account inactive");}
                try(PreparedStatement ps=real.prepareStatement("UPDATE dbo.[User] SET otpExpiry=DATEADD(SECOND,-1,SYSUTCDATETIME()) WHERE username=?")){ps.setString(1,name);ps.executeUpdate();}
                mail.fail=false;service.sendActivationOtp(name);
                check(dao.get(name).getOtpAttempts()==0,"resend resets attempts");
                if(!first.equals(mail.code))check("WRONG".equals(service.verifyOtp(name,first)),"old OTP rejected after resend");
                check("SUCCESS".equals(service.verifyOtp(name,mail.code)),"correct OTP activates");
                User activated=dao.get(name);
                check(activated.isActive()&&activated.getOtp()==null&&activated.getOtpExpiry()==null,"activation persists and clears OTP");
                check(service.login(name,password)!=null,"activated account can login");
                check("ACTIVE".equals(service.verifyOtp(name,mail.code)),"used OTP cannot activate again");
            } finally {real.rollback();System.out.println("ROLLBACK: test schema and records removed; no email sent.");}
        } catch(Exception e){System.err.println("Activation check failed (details suppressed).");System.exit(1);}
    }
}
