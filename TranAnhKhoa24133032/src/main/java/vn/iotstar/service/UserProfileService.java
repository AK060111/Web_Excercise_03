package vn.iotstar.service;
import vn.iotstar.model.User;
public interface UserProfileService {
    User findById(int id);
    void validate(String fullname,String phone);
    User updateProfile(int id,String fullname,String phone,String newAvatar);
}
