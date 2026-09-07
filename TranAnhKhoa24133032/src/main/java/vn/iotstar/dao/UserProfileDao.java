package vn.iotstar.dao;
import vn.iotstar.model.User;
public interface UserProfileDao {
    User findById(int id);
    User updateProfile(int id,String fullname,String phone,String newAvatar);
}
