package vn.iotstar.service;
import vn.iotstar.model.User;
public interface UserService {
    User login(String username,String password);
    User get(String username);
    void insert(User user);
    boolean register(String username,String password,String email,String fullname,String phone);
    boolean checkExistEmail(String email);
    boolean checkExistUsername(String username);
    boolean checkExistPhone(String phone);
    void sendActivationOtp(String username);
    String verifyOtp(String username,String otp);
    void sendResetOtp(String email);
    String verifyResetOtp(String email,String otp,String grant);
    boolean resetPassword(String email,String grant,String password);
}
