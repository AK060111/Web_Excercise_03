package vn.iotstar.model;

import java.io.Serializable;
import java.sql.Date;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id, roleid;
    private String email, userName, fullName, passWord, avatar, phone;
    private Date createdDate;
    private boolean active;
    private String otp;
    private java.sql.Timestamp otpExpiry;
    private int otpAttempts;
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public String getOtp(){return otp;} public void setOtp(String v){otp=v;}
    public java.sql.Timestamp getOtpExpiry(){return otpExpiry;} public void setOtpExpiry(java.sql.Timestamp v){otpExpiry=v;}
    public int getOtpAttempts(){return otpAttempts;} public void setOtpAttempts(int v){otpAttempts=v;}
    public User() { }
    public User(String email, String userName, String fullName, String passWord, String avatar, int roleid, String phone, Date createdDate) {
        this.email=email; this.userName=userName; this.fullName=fullName; this.passWord=passWord; this.avatar=avatar; this.roleid=roleid; this.phone=phone; this.createdDate=createdDate;
    }
    public int getId(){return id;} public void setId(int v){id=v;} public int getRoleid(){return roleid;} public void setRoleid(int v){roleid=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getUserName(){return userName;} public void setUserName(String v){userName=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;} public String getPassWord(){return passWord;} public void setPassWord(String v){passWord=v;}
    public String getAvatar(){return avatar;} public void setAvatar(String v){avatar=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public Date getCreatedDate(){return createdDate;} public void setCreatedDate(Date v){createdDate=v;}
}
