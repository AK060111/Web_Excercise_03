import java.lang.reflect.*;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import vn.iotstar.config.JPAConfig;
import vn.iotstar.model.User;
import vn.iotstar.dao.impl.*;
import vn.iotstar.service.impl.*;
import vn.iotstar.service.MailService;
import vn.iotstar.controller.ProfileController;
import vn.iotstar.util.ImageUpload;

/** JPA/JDBC regression and servlet checks; test rows always rolled back, no migration/mail. */
public class CheckProfile {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
    static void rejects(Runnable action,String label){try{action.run();throw new AssertionError(label);}catch(IllegalArgumentException expected){check(true,label);}}
    static User fixture(String suffix){
        String name="profile_"+UUID.randomUUID().toString().replace("-","");
        User u=new User(name+"@example.invalid",name,"Test "+suffix,UUID.randomUUID().toString(),"avatar/existing.png",5,
            "09"+String.format(Locale.ROOT,"%08d",new java.security.SecureRandom().nextInt(100000000)),java.sql.Date.valueOf(java.time.LocalDate.now()));
        u.setActive(true);u.setOtp("fixture-digest");u.setOtpAttempts(2);u.setOtpExpiry(Timestamp.valueOf("2030-01-01 00:00:00"));return u;
    }
    static Part part(String name,byte[] bytes){return (Part)Proxy.newProxyInstance(CheckProfile.class.getClassLoader(),new Class[]{Part.class},(p,m,a)->switch(m.getName()){
        case "getSubmittedFileName"->name;case "getSize"->(long)bytes.length;case "getInputStream"->new ByteArrayInputStream(bytes);default->null;
    });}
    static class Controller extends ProfileController {
        Controller(UserProfileServiceImpl s){super(s);}
        void call(boolean post,HttpServletRequest req,HttpServletResponse resp)throws Exception{if(post)super.doPost(req,resp);else super.doGet(req,resp);}
    }
    static class Http {
        Map<String,Object> session=new HashMap<>(),attrs=new HashMap<>();Map<String,String> params=new HashMap<>();
        Part upload;String redirect,view;int status;boolean authenticated=true;
        HttpSession sess=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->{
            switch(m.getName()){case "getAttribute":return session.get(a[0]);case "setAttribute":session.put((String)a[0],a[1]);return null;case "removeAttribute":session.remove(a[0]);return null;case "invalidate":session.clear();authenticated=false;return null;default:return null;}
        });
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->{
            switch(m.getName()){
                case "getSession":return authenticated?sess:null;case "getContextPath":return "/ServletCRUDMVC";
                case "getParameter":return params.get(a[0]);case "getPart":return upload;
                case "setAttribute":attrs.put((String)a[0],a[1]);return null;
                case "getRequestDispatcher":view=(String)a[0];return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{RequestDispatcher.class},(x,y,z)->null);
                default:return null;
            }
        });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{
            if(m.getName().equals("sendRedirect"))redirect=(String)a[0];if(m.getName().equals("sendError"))status=(Integer)a[0];return null;
        });
        void call(Controller c,boolean post,Map<String,String> params)throws Exception{this.params=params;attrs.clear();redirect=null;view=null;status=200;c.call(post,req,resp);}
    }
    public static void main(String[] args)throws Exception {
        Path uploadRoot=Path.of("target/profile-test-upload").toAbsolutePath();
        System.setProperty("app.upload.dir",uploadRoot.toString());
        EntityManager em=JPAConfig.getEntityManager();EntityTransaction tx=em.getTransaction();
        List<Path> files=new ArrayList<>();
        try {
            tx.begin();User a=fixture("A"),b=fixture("B");em.persist(a);em.persist(b);em.flush();
            int id=a.getId(),otherId=b.getId();String originalPassword=a.getPassWord(),originalEmail=a.getEmail(),originalUsername=a.getUserName();
            em.clear();
            EntityTransaction borrowedTx=(EntityTransaction)Proxy.newProxyInstance(CheckProfile.class.getClassLoader(),new Class[]{EntityTransaction.class},(p,m,v)->m.getName().equals("isActive")?tx.isActive():null);
            EntityManager borrowed=(EntityManager)Proxy.newProxyInstance(CheckProfile.class.getClassLoader(),new Class[]{EntityManager.class},(p,m,v)->{
                if(m.getName().equals("close"))return null;if(m.getName().equals("getTransaction"))return borrowedTx;
                try{return m.invoke(em,v);}catch(InvocationTargetException e){throw e.getCause();}
            });
            UserProfileDaoImpl dao=new UserProfileDaoImpl(()->borrowed);UserProfileServiceImpl service=new UserProfileServiceImpl(dao);
            check(service.findById(id).getEmail().equals(originalEmail),"JPA reads User mapping");
            rejects(()->service.updateProfile(id," ",a.getPhone(),null),"empty fullname rejected");
            rejects(()->service.updateProfile(id,"Khoa123",a.getPhone(),null),"profile rejects digits in fullname");
            User normalized=service.updateProfile(id,"  Nguyễn   Văn An ",a.getPhone(),null);
            check(normalized.getFullName().equals("Nguyễn Văn An"),"profile persists normalized Vietnamese fullname");
            rejects(()->service.updateProfile(id,"Name","abc",null),"invalid phone rejected");
            rejects(()->service.updateProfile(id,"Name",b.getPhone(),null),"duplicate phone rejected");
            rejects(()->service.updateProfile(id,"Name",a.getPhone(),"../other.png"),"avatar path traversal rejected");
            String newPhone="08"+String.format(Locale.ROOT,"%08d",new java.security.SecureRandom().nextInt(100000000));
            User updated=service.updateProfile(id,"Updated name",newPhone,null);
            check(updated.getFullName().equals("Updated name")&&updated.getPhone().equals(newPhone)&&updated.getAvatar().equals("avatar/existing.png"),"JPA update keeps old avatar");
            check(updated.getPassWord().equals(originalPassword)&&updated.getEmail().equals(originalEmail)&&updated.getRoleid()==5&&updated.isActive()&&updated.getOtpAttempts()==2&&updated.getOtp().equals("fixture-digest"),"profile leaves protected fields unchanged");
            Connection raw=em.unwrap(org.hibernate.Session.class).doReturningWork(c->c);
            Connection borrowedConnection=(Connection)Proxy.newProxyInstance(CheckProfile.class.getClassLoader(),new Class[]{Connection.class},(p,m,v)->{
                if(m.getName().equals("close"))return null;try{return m.invoke(raw,v);}catch(InvocationTargetException e){throw e.getCause();}
            });
            UserDaoImpl jdbc=new UserDaoImpl(){public Connection getConnection(){return borrowedConnection;}};
            UserServiceImpl login=new UserServiceImpl(jdbc,new MailService());
            check(login.login(originalUsername,originalPassword).getFullName().equals("Updated name"),"existing JDBC login sees JPA update");
            Controller controller=new Controller(service);Http http=new Http();http.session.put("account",a);
            http.call(controller,false,Map.of());check(http.view.endsWith("profile.jsp")&&((User)http.attrs.get("profile")).getId()==id,"GET current profile");
            http.call(controller,true,Map.of("csrf",(String)http.session.get("profileCsrf"),"fullname","Khoa@","phone",newPhone));
            check(http.view.endsWith("profile.jsp")&&http.attrs.get("profileFullname").equals("Khoa@")&&service.findById(id).getFullName().equals("Updated name"),"invalid profile fullname preserved without database mutation");
            http.call(controller,true,Map.of("fullname","Other"));check(http.status==403,"CSRF required");
            var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);byte[] png=output.toByteArray();
            try{ImageUpload.save(part("fake.png",new byte[]{1,2,3}),"avatar");throw new AssertionError("fake accepted");}catch(IllegalArgumentException expected){check(true,"non-image content rejected");}
            try{ImageUpload.save(part("avatar.svg",png),"avatar");throw new AssertionError("svg accepted");}catch(IllegalArgumentException expected){check(true,"unsupported extension rejected");}
            http.upload=part("../../avatar.png",png);
            Map<String,String> form=new HashMap<>();form.put("csrf",(String)http.session.get("profileCsrf"));form.put("fullname","Profile saved");form.put("phone",newPhone);
            form.put("id",Integer.toString(otherId));form.put("roleid","1");form.put("password","forged");form.put("email","forged@example.invalid");form.put("active","false");
            http.call(controller,true,form);check(http.redirect!=null&&http.redirect.endsWith("/profile"),"multipart profile save redirect");
            User saved=(User)http.session.get("account");Path avatar=uploadRoot.resolve(saved.getAvatar());files.add(avatar);
            check(saved.getId()==id&&saved.getFullName().equals("Profile saved")&&service.findById(otherId).getFullName().equals(b.getFullName()),"posted foreign ID ignored");
            check(saved.getRoleid()==5&&saved.getEmail().equals(originalEmail)&&saved.getPassWord().equals(originalPassword)&&saved.isActive(),"posted protected fields ignored");
            check(saved.getAvatar().startsWith("avatar/")&&!saved.getAvatar().contains("..")&&Files.isRegularFile(avatar),"avatar has safe persistent relative path");
            em.clear();User reloaded=login.login(originalUsername,originalPassword);
            check(reloaded.getAvatar().equals(saved.getAvatar())&&reloaded.getFullName().equals(saved.getFullName()),"logout/login data reload compatibility");
            http.upload=null;http.call(controller,true,form);check(((User)http.session.get("account")).getAvatar().equals(saved.getAvatar()),"empty upload preserves avatar");
            String productImage=ImageUpload.save(part("product.png",png),"product");files.add(uploadRoot.resolve(productImage));
            check(Files.isRegularFile(uploadRoot.resolve(productImage)),"shared Product upload regression");
            Http guest=new Http();guest.authenticated=false;guest.call(controller,false,Map.of());check(guest.redirect.endsWith("/login"),"guest profile GET blocked");
            guest.call(controller,true,form);check(guest.redirect.endsWith("/login"),"guest profile POST blocked");
            System.out.println("CHECKS PASSED: "+checks);
        }finally{
            if(tx.isActive())tx.rollback();em.close();JPAConfig.close();
            for(Path file:files){if(file.normalize().startsWith(uploadRoot))Files.deleteIfExists(file);}
            System.out.println("Test data rolled back; test uploads cleaned; no migration or email.");
        }
    }
}
