package vn.iotstar.service.impl;
import vn.iotstar.dao.UserProfileDao;
import vn.iotstar.dao.impl.UserProfileDaoImpl;
import vn.iotstar.model.User;
import vn.iotstar.service.UserProfileService;
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileDao dao;
    public UserProfileServiceImpl(){this(new UserProfileDaoImpl());}
    public UserProfileServiceImpl(UserProfileDao dao){this.dao=dao;}
    public User findById(int id){
        if(id<1)return null;
        User user=dao.findById(id);return user!=null&&user.isActive()?user:null;
    }
    public void validate(String fullname,String phone){
        if(fullname==null||fullname.isBlank()||fullname.trim().length()>255)
            throw new IllegalArgumentException("Họ tên không được rỗng và tối đa 255 ký tự.");
        if(phone!=null&&!phone.isBlank()&&(phone.trim().length()>30||!phone.trim().matches("[+0-9() .-]{6,30}")))
            throw new IllegalArgumentException("Số điện thoại từ 6 đến 30 ký tự, chỉ gồm số và dấu + ( ) . - hoặc khoảng trắng.");
        if(phone!=null&&!phone.isBlank()&&!phone.matches(".*[0-9].*"))
            throw new IllegalArgumentException("Số điện thoại phải chứa chữ số.");
    }
    public User updateProfile(int id,String fullname,String phone,String newAvatar){
        if(id<1)throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ.");
        validate(fullname,phone);
        if(newAvatar!=null&&!newAvatar.matches("avatar/[0-9]+-[a-f0-9-]+\\.(jpg|jpeg|png|gif)"))
            throw new IllegalArgumentException("Ảnh đại diện không hợp lệ.");
        return dao.updateProfile(id,fullname.trim(),phone==null||phone.isBlank()?null:phone.trim(),newAvatar);
    }
}
