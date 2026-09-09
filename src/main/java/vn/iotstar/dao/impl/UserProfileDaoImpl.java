package vn.iotstar.dao.impl;

import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import vn.iotstar.config.JPAConfig;
import vn.iotstar.dao.UserProfileDao;
import vn.iotstar.model.User;

/** Profile uses JPA; authentication and OTP keep their existing JDBC implementation. */
public class UserProfileDaoImpl implements UserProfileDao {
    private final Supplier<EntityManager> managers;
    public UserProfileDaoImpl(){this(JPAConfig::getEntityManager);}
    public UserProfileDaoImpl(Supplier<EntityManager> managers){this.managers=managers;}
    public User findById(int id){
        EntityManager em=managers.get();
        try{return em.find(User.class,id);}
        catch(RuntimeException e){throw new IllegalStateException("Không thể tải hồ sơ. Vui lòng thử lại sau.");}
        finally{em.close();}
    }
    public User updateProfile(int id,String fullname,String phone,String newAvatar){
        EntityManager em=managers.get();EntityTransaction tx=em.getTransaction();
        try {
            tx.begin();
            String condition=phone==null?"u.phone IS NULL":"u.phone=:phone";
            var check=em.createQuery("SELECT COUNT(u) FROM User u WHERE u.id<>:id AND "+condition,Long.class).setParameter("id",id);
            if(phone!=null)check.setParameter("phone",phone);
            if(check.getSingleResult()>0)throw new IllegalArgumentException("Số điện thoại không thể sử dụng. Vui lòng nhập số khác.");
            // Explicit allowlist prevents stale session values overwriting role/password/OTP.
            String jpql="UPDATE User u SET u.fullName=:name,u.phone=:phone"
                +(newAvatar==null?"":",u.avatar=:avatar")+" WHERE u.id=:id AND u.active=true";
            var update=em.createQuery(jpql).setParameter("name",fullname).setParameter("phone",phone).setParameter("id",id);
            if(newAvatar!=null)update.setParameter("avatar",newAvatar);
            if(update.executeUpdate()!=1){tx.rollback();return null;}
            em.clear();User latest=em.find(User.class,id);tx.commit();return latest;
        }catch(IllegalArgumentException e){if(tx.isActive())tx.rollback();throw e;}
        catch(RuntimeException e){
            if(tx.isActive())tx.rollback();
            throw new IllegalStateException("Không thể lưu hồ sơ. Vui lòng kiểm tra số điện thoại hoặc thử lại sau.");
        }finally{em.close();}
    }
}
