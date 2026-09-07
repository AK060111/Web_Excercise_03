package vn.iotstar.model;

import java.io.Serializable;
import java.sql.Date;
import jakarta.persistence.*;

@Entity
@Table(name="[User]", schema="dbo")
@Cacheable(false)
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private int id;
    @Column(name="roleid",nullable=false,updatable=false)
    private int roleid;
    @Column(name="email",nullable=false,updatable=false,columnDefinition="nvarchar(255)")
    private String email;
    @Column(name="username",nullable=false,updatable=false,columnDefinition="nvarchar(100)")
    private String userName;
    @Column(name="fullname",nullable=false,columnDefinition="nvarchar(255)")
    private String fullName;
    @Column(name="password",nullable=false,updatable=false,columnDefinition="nvarchar(255)")
    private String passWord;
    @Column(name="avatar",columnDefinition="nvarchar(255)")
    private String avatar;
    @Column(name="phone",columnDefinition="nvarchar(30)")
    private String phone;
    @Column(name="createdDate",nullable=false,updatable=false)
    private Date createdDate;
    @Column(name="active",nullable=false,updatable=false)
    private boolean active;
    @Column(name="otp",length=64,updatable=false)
    private String otp;
    @Column(name="otpExpiry",updatable=false,columnDefinition="datetime2")
    private java.sql.Timestamp otpExpiry;
    @Column(name="otpAttempts",nullable=false,updatable=false)
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
