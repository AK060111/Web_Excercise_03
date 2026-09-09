package vn.iotstar.service.impl;
import java.sql.Date;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import vn.iotstar.dao.UserDao;
import vn.iotstar.dao.impl.UserDaoImpl;
import vn.iotstar.model.User;
import vn.iotstar.service.UserService;
import vn.iotstar.service.MailService;
public class UserServiceImpl implements UserService {
    private final UserDao dao;
    private final MailService mail;
    private static final SecureRandom RANDOM=new SecureRandom();
    public UserServiceImpl(){this(new UserDaoImpl(),new MailService());}
    public UserServiceImpl(UserDao dao,MailService mail){this.dao=dao;this.mail=mail;}
    public User login(String username,String password){
        User user=dao.get(username);
        if(user==null || password==null || !password.equals(user.getPassWord())) return null;
        if(!user.isActive()) throw new IllegalArgumentException("Tài khoản chưa kích hoạt. Vui lòng xác nhận OTP.");
        return user;
    }
    public User get(String username){return dao.get(username);}
    public void insert(User user){dao.insert(user);}
    public boolean checkExistEmail(String v){return dao.checkExistEmail(v);}
    public boolean checkExistUsername(String v){return dao.checkExistUsername(v);}
    public boolean checkExistPhone(String v){return dao.checkExistPhone(v);}
    public boolean register(String username,String password,String email,String fullname,String phone){
        username=vn.iotstar.util.ValidationUtil.required(username,"Tài khoản",100);
        fullname=vn.iotstar.util.ValidationUtil.fullname(fullname);
        email=vn.iotstar.util.ValidationUtil.email(email);vn.iotstar.util.ValidationUtil.password(password);
        phone=vn.iotstar.util.ValidationUtil.phone(phone);
        if(checkExistEmail(email)||checkExistUsername(username)||(phone!=null&&!phone.isBlank()&&checkExistPhone(phone))) return false;
        User user=new User(email,username,fullname,password,null,5,phone,Date.valueOf(java.time.LocalDate.now()));
        user.setActive(false); dao.insert(user); return true;
    }
    public void sendActivationOtp(String username){
        User user=dao.get(username);
        if(user==null || user.isActive()) throw new IllegalArgumentException("Tài khoản không cần kích hoạt.");
        String otp=newOtp(user, user.getUserName());
        if(!dao.saveOtp(user.getUserName(),digest(user.getUserName(),otp)))
            throw new IllegalArgumentException("Vui lòng chờ ít nhất 60 giây giữa các lần gửi OTP.");
        mail.sendOtp(user.getEmail(),otp);
    }
    public String verifyOtp(String username,String otp){
        if(!vn.iotstar.util.ValidationUtil.otp(otp)) return "FORMAT";
        User user=dao.get(username);
        if(user==null) return "INVALID";
        return dao.verifyOtp(user.getUserName(),digest(user.getUserName(),otp));
    }
    public void sendResetOtp(String email){
        User user=dao.getByEmail(email);
        if(user==null || !user.isActive()) return;
        String identity="reset:"+user.getUserName();
        String otp=newOtp(user,identity);
        if(!dao.saveResetOtp(user.getUserName(),digest(identity,otp))) return;
        mail.sendResetOtp(user.getEmail(),otp);
    }
    public String verifyResetOtp(String email,String otp,String grant){
        if(!vn.iotstar.util.ValidationUtil.otp(otp)) return "FORMAT";
        if(grant==null || grant.length()<32) return "INVALID";
        User user=dao.getByEmail(email);
        if(user==null || !user.isActive()) return "INVALID";
        return dao.verifyResetOtp(user.getUserName(),digest("reset:"+user.getUserName(),otp),
            digest("grant:"+user.getUserName(),grant));
    }
    public boolean resetPassword(String email,String grant,String password){
        if(grant==null || grant.length()<32) return false;
        try{vn.iotstar.util.ValidationUtil.password(password);}catch(IllegalArgumentException e){return false;}
        User user=dao.getByEmail(email);
        return user!=null && user.isActive() && dao.resetPassword(user.getUserName(),
            digest("grant:"+user.getUserName(),grant),password);
    }
    private String newOtp(User user,String identity){
        String otp;
        do {otp=String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1000000));}
        while(digest(identity,otp).equals(user.getOtp()));
        return otp;
    }
    private String digest(String username,String otp){
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((username+":"+otp).getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("Không thể tạo OTP");}
    }
}
