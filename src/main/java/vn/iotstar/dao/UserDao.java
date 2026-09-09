package vn.iotstar.dao;
import vn.iotstar.model.User;
public interface UserDao {
    User get(String username);
    User getByEmail(String email);
    void insert(User user);
    boolean checkExistEmail(String email);
    boolean checkExistUsername(String username);
    boolean checkExistPhone(String phone);
    boolean saveOtp(String username, String otp);
    String verifyOtp(String username, String otp);
    boolean saveResetOtp(String username, String otp);
    String verifyResetOtp(String username, String otp, String grantHash);
    boolean resetPassword(String username, String grantHash, String password);
}
