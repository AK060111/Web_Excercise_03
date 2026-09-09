package vn.iotstar.dao.impl;

import java.sql.*;
import vn.iotstar.connection.DBConnection;
import vn.iotstar.dao.UserDao;
import vn.iotstar.model.User;

public class UserDaoImpl extends DBConnection implements UserDao {
    public User get(String username) {
        return find("username",username);
    }
    public User getByEmail(String email) { return find("email",email); }
    private User find(String field,String value) {
        try (Connection cn=getConnection(); PreparedStatement ps=cn.prepareStatement("SELECT * FROM dbo.[User] WHERE "+field+"=?")) {
            ps.setString(1,value);
            try(ResultSet rs=ps.executeQuery()) {
                if(!rs.next()) return null;
                User u=new User();
                u.setId(rs.getInt("id")); u.setEmail(rs.getString("email"));
                u.setUserName(rs.getString("username")); u.setFullName(rs.getString("fullname"));
                u.setPassWord(rs.getString("password")); u.setAvatar(rs.getString("avatar"));
                u.setRoleid(rs.getInt("roleid")); u.setPhone(rs.getString("phone"));
                u.setCreatedDate(rs.getDate("createdDate")); u.setActive(rs.getBoolean("active"));
                u.setOtp(rs.getString("otp")); u.setOtpExpiry(rs.getTimestamp("otpExpiry"));
                u.setOtpAttempts(rs.getInt("otpAttempts")); return u;
            }
        } catch(Exception e) { throw new IllegalStateException("Không thể đọc User"); }
    }
    public void insert(User u) {
        String sql="INSERT INTO dbo.[User](email,username,fullname,password,avatar,roleid,phone,createdDate,active) VALUES (?,?,?,?,?,?,?,?,?)";
        try(Connection cn=getConnection(); PreparedStatement ps=cn.prepareStatement(sql)) {
            ps.setString(1,u.getEmail()); ps.setString(2,u.getUserName()); ps.setString(3,u.getFullName());
            ps.setString(4,u.getPassWord()); ps.setString(5,u.getAvatar()); ps.setInt(6,u.getRoleid());
            ps.setString(7,u.getPhone()); ps.setDate(8,u.getCreatedDate()); ps.setBoolean(9,u.isActive());
            ps.executeUpdate();
        } catch(Exception e) { throw new IllegalStateException("Không thể thêm User"); }
    }
    public boolean checkExistEmail(String v){return exists("email",v);}
    public boolean checkExistUsername(String v){return exists("username",v);}
    public boolean checkExistPhone(String v){return exists("phone",v);}
    private boolean exists(String field,String value) {
        try(Connection cn=getConnection(); PreparedStatement ps=cn.prepareStatement("SELECT 1 FROM dbo.[User] WHERE "+field+"=?")) {
            ps.setString(1,value); try(ResultSet rs=ps.executeQuery()){return rs.next();}
        } catch(Exception e) { throw new IllegalStateException("Không thể kiểm tra User"); }
    }
    public boolean saveOtp(String username,String otp) {
        return saveOtp(username,otp,false);
    }
    public boolean saveResetOtp(String username,String otp) { return saveOtp(username,otp,true); }
    private boolean saveOtp(String username,String otp,boolean reset) {
        // The database clock controls expiry and a per-account resend cooldown.
        String sql="UPDATE dbo.[User] SET otp=?, otpExpiry=DATEADD(MINUTE,5,SYSUTCDATETIME()), otpAttempts=0 "
            +"WHERE username=? AND active=? AND (otpExpiry IS NULL OR otpExpiry<=DATEADD(MINUTE,4,SYSUTCDATETIME()))";
        try(Connection cn=getConnection(); PreparedStatement ps=cn.prepareStatement(sql)) {
            ps.setString(1,otp); ps.setString(2,username); ps.setBoolean(3,reset); return ps.executeUpdate()==1;
        } catch(Exception e) { throw new IllegalStateException("Không thể lưu OTP"); }
    }
    public String verifyOtp(String username,String otp) {
        return verifyOtp(username,otp,null);
    }
    public String verifyResetOtp(String username,String otp,String grantHash) {
        if(grantHash==null) throw new IllegalArgumentException("Thiếu quyền đặt lại mật khẩu");
        return verifyOtp(username,otp,grantHash);
    }
    private String verifyOtp(String username,String otp,String grantHash) {
        boolean reset=grantHash!=null;
        // Lock this account until activation/failed-attempt update commits.
        try(Connection cn=getConnection()) {
            cn.setAutoCommit(false);
            try {
                String result;
                String sql="SELECT active,otp,otpAttempts,CASE WHEN otpExpiry>SYSUTCDATETIME() THEN 1 ELSE 0 END AS valid "
                    +"FROM dbo.[User] WITH (UPDLOCK,ROWLOCK) WHERE username=?";
                try(PreparedStatement ps=cn.prepareStatement(sql)) {
                    ps.setString(1,username);
                    try(ResultSet rs=ps.executeQuery()) {
                        if(!rs.next()) result="INVALID";
                        else if(rs.getBoolean("active")!=reset) result=reset?"INVALID":"ACTIVE";
                        else if(rs.getInt("otpAttempts")>=5) result="LOCKED";
                        else if(rs.getInt("valid")==0) result="EXPIRED";
                        else if(otp.equals(rs.getString("otp"))) result="SUCCESS";
                        else result="WRONG";
                    }
                }
                if("SUCCESS".equals(result) || "WRONG".equals(result)) {
                    String update="SUCCESS".equals(result)
                        ? (reset ? "UPDATE dbo.[User] SET otp=?,otpExpiry=DATEADD(MINUTE,5,SYSUTCDATETIME()),otpAttempts=5 WHERE username=?"
                            : "UPDATE dbo.[User] SET active=1,otp=NULL,otpExpiry=NULL,otpAttempts=0 WHERE username=?")
                        : "UPDATE dbo.[User] SET otpAttempts=otpAttempts+1 WHERE username=?";
                    try(PreparedStatement ps=cn.prepareStatement(update)){
                        int index=1;
                        if(reset&&"SUCCESS".equals(result)) ps.setString(index++,grantHash);
                        ps.setString(index,username);ps.executeUpdate();
                    }
                }
                cn.commit(); return result;
            } catch(Exception e) {cn.rollback();throw e;}
        } catch(Exception e) { throw new IllegalStateException("Không thể xác nhận OTP"); }
    }
    public boolean resetPassword(String username,String grantHash,String password) {
        // One atomic update consumes the verified grant. Resend revokes previous grants.
        String sql="UPDATE dbo.[User] SET password=?,otp=NULL,otpExpiry=NULL,otpAttempts=0 "
            +"WHERE username=? AND active=1 AND otp=? AND otpAttempts=5 AND otpExpiry>SYSUTCDATETIME()";
        try(Connection cn=getConnection();PreparedStatement ps=cn.prepareStatement(sql)) {
            ps.setString(1,password);ps.setString(2,username);ps.setString(3,grantHash);
            return ps.executeUpdate()==1;
        } catch(Exception e){throw new IllegalStateException("Không thể đặt lại mật khẩu");}
    }
}
