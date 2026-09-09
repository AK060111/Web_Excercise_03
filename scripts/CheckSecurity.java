import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import vn.iotstar.controller.*;
import vn.iotstar.model.*;
import vn.iotstar.service.*;
import vn.iotstar.service.impl.*;
import vn.iotstar.dao.*;
import vn.iotstar.util.*;
import vn.iotstar.filter.ProfileLayoutFilter;

/** No SQL, mail or real upload data. Servlet proxies exercise security boundaries. */
public class CheckSecurity {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
    interface Action { void run() throws Exception; }
    static void rejects(Action action,String label)throws Exception{
        try{action.run();throw new AssertionError(label);}catch(IllegalArgumentException|IOException expected){check(true,label);}
    }
    static Object zero(Class<?> type){
        if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;
    }
    static <T>T proxy(Class<T> type,InvocationHandler handler){return type.cast(Proxy.newProxyInstance(CheckSecurity.class.getClassLoader(),new Class[]{type},handler));}
    static Part part(String filename,byte[] bytes){return proxy(Part.class,(p,m,a)->switch(m.getName()){
        case "getSubmittedFileName"->filename;case "getSize"->(long)bytes.length;
        case "getInputStream"->new ByteArrayInputStream(bytes);default->zero(m.getReturnType());
    });}
    static class Http {
        Map<String,Object> session=new HashMap<>(),attrs=new HashMap<>();Map<String,String> params=new HashMap<>();
        Map<String,String> headers=new HashMap<>();List<Cookie> addedCookies=new ArrayList<>();
        boolean hasSession=true,changed,committed;int decoratorIncludes;String method="POST",path="/login",view,redirect,type;
        int status=200,parts;Part upload;Cookie[] cookies;ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        PrintWriter writer=new PrintWriter(new OutputStreamWriter(bytes,java.nio.charset.StandardCharsets.UTF_8),true);
        ServletOutputStream output=new ServletOutputStream(){public void write(int b){bytes.write(b);}public boolean isReady(){return true;}public void setWriteListener(WriteListener l){}};
        HttpSession sess=proxy(HttpSession.class,(p,m,a)->{
            switch(m.getName()){
                case "getAttribute":return session.get(a[0]);case "setAttribute":session.put((String)a[0],a[1]);return null;
                case "removeAttribute":session.remove(a[0]);return null;case "invalidate":session.clear();hasSession=false;return null;
                case "getId":return "test-session";default:return zero(m.getReturnType());
            }
        });
        ServletContext context=proxy(ServletContext.class,(p,m,a)->switch(m.getName()){
            case "getFilterRegistrations"->Map.of();case "getContextPath"->"/ServletCRUDMVC";
            case "getServerInfo"->"Apache Tomcat/11.0.25";
            case "getRequestDispatcher"-> {
                if(!"/WEB-INF/decorators/main.jsp".equals(a[0]))throw new AssertionError("Decorator path must resolve exactly");
                // Simulate the container rejecting a second forward at decorator dispatch.
                committed=true;
                yield proxy(RequestDispatcher.class,(x,n,v)->{
                    if(n.getName().equals("forward")&&committed)throw new IllegalStateException("Cannot forward after response has been committed");
                    if(n.getName().equals("include"))decoratorIncludes++;
                    if(n.getName().equals("forward")||n.getName().equals("include"))((ServletResponse)v[1]).getWriter().write("<!DOCTYPE html><html><head><title>Profile</title></head><body><header>Header</header><sitemesh:write property='body'/><footer>Footer</footer></body></html>");
                    return null;
                });
            }
            default->zero(m.getReturnType());
        });
        HttpServletRequest req=proxy(HttpServletRequest.class,(p,m,a)->{
            switch(m.getName()){
                case "getSession":if(a==null||a.length==0||Boolean.TRUE.equals(a[0]))hasSession=true;return hasSession?sess:null;
                case "changeSessionId":changed=true;return "rotated";
                case "getParameter":return params.get(a[0]);case "getPart":parts++;return upload;
                case "getMethod":return method;case "getServletPath":return path;
                case "getRequestURI":return "/ServletCRUDMVC"+path;case "getContextPath":return "/ServletCRUDMVC";
                case "getServletContext":return context;case "getDispatcherType":return DispatcherType.REQUEST;
                case "getCookies":return cookies;case "getAttribute":return attrs.get(a[0]);
                case "setAttribute":attrs.put((String)a[0],a[1]);return null;case "removeAttribute":attrs.remove(a[0]);return null;
                case "getCharacterEncoding":return "UTF-8";
                case "getRequestDispatcher":return proxy(RequestDispatcher.class,(x,n,v)->{
                    if(n.getName().equals("forward")){view=(String)a[0];}
                    if(n.getName().equals("include")){
                        ((ServletResponse)v[1]).getWriter().write("<!DOCTYPE html><html><head><title>Profile</title></head><body><header>Header</header><sitemesh:write property='body'/><footer>Footer</footer></body></html>");
                    }
                    return null;
                });
                default:return zero(m.getReturnType());
            }
        });
        HttpServletResponse resp=proxy(HttpServletResponse.class,(p,m,a)->{
            switch(m.getName()){
                case "sendError":status=(Integer)a[0];return null;case "sendRedirect":status=302;redirect=(String)a[0];return null;
                case "setStatus":status=(Integer)a[0];return null;case "getStatus":return status;
                case "setHeader":headers.put((String)a[0],(String)a[1]);return null;
                case "setContentType":type=(String)a[0];return null;case "getContentType":return type;
                case "getCharacterEncoding":return "UTF-8";case "getWriter":return writer;case "getOutputStream":return output;
                case "isCommitted":return committed;
                case "addCookie":addedCookies.add((Cookie)a[0]);return null;case "encodeURL":case "encodeRedirectURL":return a[0];
                default:return zero(m.getReturnType());
            }
        });
        String body(){writer.flush();return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);}
    }
    static class Login extends LoginController {Login(UserService s){super(s);}void call(Http h,boolean post)throws Exception{if(post)doPost(h.req,h.resp);else doGet(h.req,h.resp);}}
    static class Register extends RegisterController {void call(Http h)throws Exception{doPost(h.req,h.resp);}}
    static class Add extends CategoryAddController {Add(CategoryService s){super(s);}void call(Http h)throws Exception{doPost(h.req,h.resp);}}
    static class Edit extends CategoryEditController {Edit(CategoryService s){super(s);}void call(Http h)throws Exception{doPost(h.req,h.resp);}}
    static class Delete extends CategoryDeleteController {Delete(CategoryService s){super(s);}void call(Http h,boolean post)throws Exception{if(post)doPost(h.req,h.resp);else doGet(h.req,h.resp);}}
    static class Image extends DownloadImageController {void call(Http h)throws Exception{doGet(h.req,h.resp);}}
    static class MemoryCategory implements InvocationHandler {
        Category row=new Category(1,"Existing","category/old.png");int writes;boolean fail;
        public Object invoke(Object p,Method m,Object[] a){
            switch(m.getName()){
                case "findById":return (Integer)a[0]==1?new Category(row.getId(),row.getName(),row.getIcon()):null;
                case "findByCategoryname":return row.getName().equalsIgnoreCase((String)a[0])?row:null;
                case "insert":case "update":case "delete":if(fail)throw new IllegalStateException("simulated");writes++;return null;
                default:return zero(m.getReturnType());
            }
        }
    }
    public static void main(String[] args)throws Exception{
        for(String name:List.of("Nguyễn Văn An","Trần Anh Khoa","Đặng Thị Ánh","Khoa"))
            check(ValidationUtil.fullname(name).equals(name),"Unicode fullname accepted");
        check(ValidationUtil.fullname("  Nguyễn\t Văn\u00a0  An  ").equals("Nguyễn Văn An"),"fullname whitespace normalized");
        check(ValidationUtil.fullname(java.text.Normalizer.normalize("Đặng Thị Ánh",java.text.Normalizer.Form.NFD)).equals("Đặng Thị Ánh"),"decomposed Vietnamese normalized to NFC");
        check(ValidationUtil.fullname("K".repeat(255)).length()==255,"fullname maximum accepted");
        for(String name:List.of("Khoa123","Nguyễn Văn An1","Khoa@","Nguyễn-Văn-An","Nguyễn Văn An!","@@@","12345","","   ","K".repeat(256),"Khoa😀"))
            rejects(()->ValidationUtil.fullname(name),"invalid fullname rejected");
        User[] registered={null};
        var registration=new UserServiceImpl(proxy(UserDao.class,(p,m,a)->{
            if(m.getName().equals("insert"))registered[0]=(User)a[0];return zero(m.getReturnType());
        }),new MailService());
        check(registration.register("fixture","password123","fixture@example.invalid","  Trần   Anh Khoa ",null)
            &&registered[0].getFullName().equals("Trần Anh Khoa"),"registration service persists normalized fullname without mail");
        registered[0]=null;rejects(()->registration.register("fixture","password123","fixture@example.invalid","Khoa123",null),"registration service rejects invalid fullname");
        check(registered[0]==null,"invalid fullname never reaches registration DAO");
        Http invalidName=new Http();invalidName.params=Map.of("username","fixture","password","password123","email","fixture@example.invalid","fullname","Khoa@","phone","");
        new Register().call(invalidName);
        check(invalidName.view.endsWith("register.jsp")&&invalidName.attrs.get("registerFullname").equals("Khoa@")&&invalidName.attrs.containsKey("alert"),"registration invalid fullname preserved for escaped redisplay");
        Path root=Path.of("target/security-test-upload").toAbsolutePath();Files.createDirectories(root);
        System.setProperty("app.upload.dir",root.toString());
        List<Path> created=new ArrayList<>();
        try {
            User account=new User();account.setId(1);account.setActive(true);account.setRoleid(1);
            int[] lookups={0};UserService users=proxy(UserService.class,(p,m,a)->{
                if(m.getName().equals("get")){lookups[0]++;throw new AssertionError("Cookie lookup must not run");}
                if(m.getName().equals("login"))return "tester".equals(a[0])&&"test-pass-123".equals(a[1])?account:null;
                return zero(m.getReturnType());
            });
            Login login=new Login(users);Http forged=new Http();forged.hasSession=false;forged.cookies=new Cookie[]{new Cookie("username","admin")};
            login.call(forged,false);check(lookups[0]==0&&!forged.session.containsKey("account")&&forged.view.endsWith("login.jsp"),"forged username cookie does not authenticate");
            check(forged.addedCookies.stream().anyMatch(c->c.getName().equals("username")&&c.getMaxAge()==0),"legacy cookie expired");
            Http normal=new Http();normal.params=Map.of("username"," tester ","password","test-pass-123");login.call(normal,true);
            check(normal.session.get("account")==account&&normal.changed&&normal.redirect.endsWith("/waiting"),"password login trims identity and rotates session");
            Http wrong=new Http();wrong.params=Map.of("username","tester","password","wrong");login.call(wrong,true);
            check(wrong.attrs.get("loginUsername").equals("tester")&&!wrong.attrs.containsKey("password"),"login preserves only safe input");
            MemoryCategory memory=new MemoryCategory();CategoryService categories=new CategoryServiceImpl(proxy(CategoryDao.class,memory));
            Add add=new Add(categories);Edit edit=new Edit(categories);Delete delete=new Delete(categories);
            for(String action:List.of("add","edit","delete")){
                Http h=new Http();h.params=Map.of("id","1","name","New");
                if(action.equals("add"))add.call(h);else if(action.equals("edit"))edit.call(h);else delete.call(h,true);
                check(h.status==403&&memory.writes==0&&h.parts==0,"Category "+action+" requires CSRF before mutation/upload");
            }
            Http get=new Http();get.params=Map.of("id","1");delete.call(get,false);check(get.status==405&&memory.writes==0,"GET Category delete cannot mutate");
            Http duplicate=new Http();duplicate.session.put("categoryCsrf","token");duplicate.params=Map.of("csrf","token","name"," Existing ");add.call(duplicate);
            check(memory.writes==0&&duplicate.parts==0&&duplicate.attrs.get("alert")!=null&&duplicate.attrs.get("categoryName").equals("Existing"),"duplicate Category returns form preserving trimmed name before upload");
            rejects(()->categories.validate(new Category(0,"x".repeat(256),null)),"Category maximum 255");
            Http empty=new Http();empty.session.put("categoryCsrf","token");empty.params=Map.of("csrf","token","id","1","name"," ");edit.call(empty);
            check(memory.writes==0&&empty.view.endsWith("edit-category.jsp")&&empty.attrs.containsKey("alert"),"invalid Category edit forwards form");
            var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);byte[] png=out.toByteArray();
            for(String folder:List.of("avatar","category","product")){
                rejects(()->ImageUpload.save(part("fake.png",new byte[]{1,2,3}),folder),"fake "+folder+" image rejected");
                rejects(()->ImageUpload.save(part("wrong.jpg",png),folder),"mismatched "+folder+" image rejected");
                String name=ImageUpload.save(part("../../valid.png",png),folder);created.add(root.resolve(name));
                check(name.startsWith(folder+"/")&&!name.contains("..")&&Files.exists(root.resolve(name)),"valid "+folder+" image gets generated filename");
            }
            memory.fail=true;Http failed=new Http();failed.session.put("categoryCsrf","token");failed.params=Map.of("csrf","token","name","New");failed.upload=part("valid.png",png);
            long before;try(var paths=Files.walk(root)){before=paths.filter(Files::isRegularFile).count();}
            add.call(failed);long after;try(var paths=Files.walk(root)){after=paths.filter(Files::isRegularFile).count();}
            check(before==after&&memory.writes==0&&failed.view.endsWith("add-category.jsp"),"failed Category insert cleans new image");
            Http failedEdit=new Http();failedEdit.session.put("categoryCsrf","token");failedEdit.params=Map.of("csrf","token","id","1","name","Changed");failedEdit.upload=part("valid.png",png);edit.call(failedEdit);
            try(var paths=Files.walk(root)){after=paths.filter(Files::isRegularFile).count();}
            check(before==after&&((Category)failedEdit.attrs.get("category")).getIcon().equals("category/old.png"),"failed Category edit preserves old image");
            memory.fail=false;Http validDelete=new Http();validDelete.session.put("categoryCsrf","token");validDelete.params=Map.of("csrf","token","id","1");delete.call(validDelete,true);
            check(memory.writes==1&&validDelete.redirect.endsWith("/admin/category/list"),"Category POST delete with CSRF works");
            Http validAdd=new Http();validAdd.session.put("categoryCsrf","token");validAdd.params=Map.of("csrf","token","name"," New ");add.call(validAdd);
            check(memory.writes==2&&validAdd.redirect.endsWith("/admin/category/list"),"Category valid Add uses PRG");
            Http validEdit=new Http();validEdit.session.put("categoryCsrf","token");validEdit.params=Map.of("csrf","token","id","1","name"," Updated ");edit.call(validEdit);
            check(memory.writes==3&&validEdit.redirect.endsWith("/admin/category/list"),"Category valid Edit uses PRG");
            rejects(()->ImageUpload.save(part("large.png",new byte[5*1024*1024+1]),"product"),"oversized image rejected");
            ByteArrayOutputStream large=new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4097,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",large);
            rejects(()->ImageUpload.save(part("wide.png",large.toByteArray()),"category"),"oversized image dimensions rejected");
            byte[] webp=Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");
            String webpName=ImageUpload.save(part("valid.webp",webp),"product");created.add(root.resolve(webpName));
            check(Files.isRegularFile(root.resolve(webpName)),"valid WEBP decoder works");
            check(ValidationUtil.phone(" 0912345678 ").equals("0912345678"),"Vietnamese 0 form accepted");
            check(ValidationUtil.phone("+84912345678").equals("+84912345678"),"Vietnamese +84 form accepted");
            check(ValidationUtil.phone(" ")==null,"phone optional");
            for(String phone:List.of("abc","123456","0123456789","09123456789","+840912345678","09<script>"))rejects(()->ValidationUtil.phone(phone),"invalid Vietnamese phone rejected");
            rejects(()->ValidationUtil.password("short"),"short password rejected");
            rejects(()->ValidationUtil.email("Name <a@example.invalid>"),"email display name rejected");
            check(ValidationUtil.otp("012345")&&!ValidationUtil.otp("12345x"),"OTP six digits preserved");
            Image serve=new Image();
            for(String name:List.of("../secret.png","/absolute.png","C:/secret.png","C:secret.png","a\\b.png","file.txt","product/../../x.png")){
                Http h=new Http();h.params=Map.of("fname",name);serve.call(h);check(h.status==400,"image unsafe path rejected");
            }
            Http missing=new Http();missing.params=Map.of("fname","missing.png");serve.call(missing);check(missing.status==404,"missing image 404");
            Path outside=Files.createTempFile(Path.of("target").toAbsolutePath(),"security-outside-",".png");
            Path link=root.resolve("escape-"+UUID.randomUUID()+".png");
            try{
                Files.write(outside,png);
                try{
                    Files.createSymbolicLink(link,outside);
                    Http escape=new Http();escape.params=Map.of("fname",link.getFileName().toString());serve.call(escape);
                    check(escape.status==400,"symlink escape rejected by real-path containment");
                }catch(FileSystemException|UnsupportedOperationException e){System.out.println("SKIPPED: symlink creation unavailable on this Windows account");}
            }finally{Files.deleteIfExists(link);Files.deleteIfExists(outside);}
            Http served=new Http();served.params=Map.of("fname",root.relativize(created.get(0)).toString().replace('\\','/'));serve.call(served);
            check(served.status==200&&Arrays.equals(png,served.bytes.toByteArray())&&"nosniff".equals(served.headers.get("X-Content-Type-Options")),"valid uploaded image served with nosniff");
            String categoryJsp=Files.readString(Path.of("src/main/webapp/views/admin/list-category.jsp"));
            check(categoryJsp.contains("alt=\"<c:out value='${cate.name}'/>\"")&&!categoryJsp.contains("href=\"${pageContext.request.contextPath}/admin/category/delete"),"Category XSS attribute escaped and delete form POST (source check)");
            String profile=Files.readString(Path.of("src/main/webapp/WEB-INF/views/profile.jsp"));
            check(!profile.matches("(?is).*<(?:html|head|body)(?:>|\\s).*" )&&!profile.contains("header.jsp")&&profile.contains("multipart/form-data"),"Profile content-only multipart JSP");
            ProfileLayoutFilter filter=new ProfileLayoutFilter();Http decorated=new Http();decorated.path="/profile";decorated.upload=part("valid.png",png);
            FilterConfig config=proxy(FilterConfig.class,(p,m,a)->switch(m.getName()){
                case "getFilterName"->"profileLayout";case "getServletContext"->decorated.context;
                case "getInitParameterNames"->Collections.emptyEnumeration();default->zero(m.getReturnType());
            });
            filter.init(config);
            try{
                filter.doFilter(decorated.req,decorated.resp,(r,s)->{
                    check(((HttpServletRequest)r).getPart("avatar")==decorated.upload,"SiteMesh retains multipart access");
                    s.setContentType("text/html;charset=UTF-8");s.getWriter().write("<main>Profile content</main>");
                });
                String rendered=decorated.body();check(rendered.contains("<header>Header</header>")&&rendered.contains("<main>Profile content</main>")&&rendered.indexOf("<html>")==rendered.lastIndexOf("<html>"),"SiteMesh decorates content-only response once");
                check(decorated.decoratorIncludes==1,"Tomcat 11 detected: decorator uses include after response commit");
                Http redirect=new Http();redirect.path="/profile";
                filter.doFilter(redirect.req,redirect.resp,(r,s)->((HttpServletResponse)s).sendRedirect("/ServletCRUDMVC/login"));
                check(redirect.status==302&&redirect.body().isEmpty(),"SiteMesh preserves login redirect");
                Http denied=new Http();denied.path="/profile";
                filter.doFilter(denied.req,denied.resp,(r,s)->((HttpServletResponse)s).sendError(403));
                check(denied.status==403&&denied.body().isEmpty(),"SiteMesh preserves CSRF rejection");
            }finally{filter.destroy();}
            System.out.println("CHECKS PASSED: "+checks);
        }finally{
            for(Path file:created)if(file.normalize().startsWith(root))Files.deleteIfExists(file);
        }
    }
}
