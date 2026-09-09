import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import vn.iotstar.model.*;
import vn.iotstar.dao.*;
import vn.iotstar.dao.impl.*;
import vn.iotstar.service.impl.ProductServiceImpl;
import vn.iotstar.controller.ProductController;
import vn.iotstar.filter.AuthenticationFilter;
import vn.iotstar.config.JPAConfig;

/** Default checks do not connect to SQL Server. --database requires the migration already applied. */
public class CheckProduct {
    static int tests;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS: "+label);tests++;}
    static void rejects(Runnable r,String label){try{r.run();throw new AssertionError(label);}catch(IllegalArgumentException expected){check(true,label);}}
    static Product product(Category c,String name){Product p=new Product();p.setName(name);p.setPrice(new BigDecimal("100.50"));p.setCategory(c);return p;}
    static CategoryDao categories(Category c){return (CategoryDao)Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{CategoryDao.class},(p,m,a)->m.getName().equals("findById")&&((Integer)a[0])==c.getId()?c:null);}
    static class MemoryJpa implements Supplier<EntityManager> {
        Map<Integer,Product> rows=new LinkedHashMap<>();int sequence,offset,limit;String query;boolean active;
        EntityTransaction tx=(EntityTransaction)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{EntityTransaction.class},(p,m,a)->{if(m.getName().equals("begin"))active=true;if(m.getName().equals("commit")||m.getName().equals("rollback"))active=false;return m.getName().equals("isActive")?active:null;});
        public EntityManager get(){return (EntityManager)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{
            switch(m.getName()){
                case "getTransaction":return tx;
                case "persist":Product added=(Product)a[0];added.setId(++sequence);added.onCreate();rows.put(added.getId(),added);return null;
                case "find":return rows.get(a[1]);
                case "remove":rows.remove(((Product)a[0]).getId());return null;
                case "getReference":return new Category((Integer)a[1],"Category",null);
                case "createQuery":query=(String)a[0];offset=0;limit=Integer.MAX_VALUE;return newQuery();
                default:return null;
            }
        });}
        Object newQuery(){Map<String,Object> parameters=new HashMap<>();return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{TypedQuery.class},(p,m,a)->{
            switch(m.getName()){
                case "setParameter":parameters.put((String)a[0],a[1]);return p;
                case "setFirstResult":offset=(Integer)a[0];return p;
                case "setMaxResults":limit=(Integer)a[0];return p;
                case "getSingleResult":return (long)rows.size();
                case "getResultList":return values(parameters);
                case "getResultStream":return values(parameters).stream();
                default:return null;
            }
        });}
        List<Product> values(Map<String,Object> parameters){return rows.values().stream().filter(p->!parameters.containsKey("id")||p.getId()==(Integer)parameters.get("id")).sorted(Comparator.comparing(Product::getCreatedDate).thenComparing(Product::getId).reversed()).skip(offset).limit(limit).toList();}
    }
    static void auth(int role,String path,int expected)throws Exception {
        User user=new User();user.setRoleid(role);int[] status={200};boolean[] passed={false};
        HttpSession session=(HttpSession)Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->m.getName().equals("getAttribute")?user:null);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case "getSession"->role<0?null:session;case "getServletPath"->path;case "getContextPath"->"/ServletCRUDMVC";default->null;});
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("sendError"))status[0]=(Integer)a[0];if(m.getName().equals("sendRedirect"))status[0]=302;return null;});
        new AuthenticationFilter().doFilter(req,resp,(a,b)->passed[0]=true);
        check(status[0]==expected&&(expected!=200||passed[0]),"authorization role "+role+" "+path);
    }
    public static void main(String[] args)throws Exception {
        inputChecks();
        check(Product.class.getAnnotation(Table.class).name().equals("products"),"Product table mapping");
        check(Product.class.getDeclaredField("id").getAnnotation(GeneratedValue.class).strategy()==GenerationType.IDENTITY,"identity mapping");
        check(Product.class.getDeclaredField("category").getAnnotation(JoinColumn.class).referencedColumnName().equals("cate_id"),"Category FK mapping");
        MemoryJpa memory=new MemoryJpa();ProductDaoImpl dao=new ProductDaoImpl(memory);Category c=new Category(1,"Category",null);
        ProductServiceImpl service=new ProductServiceImpl(dao,categories(c));
        Product p=product(c,"  First  ");p.setImage("product/existing.png");service.insert(p);
        check(p.getId()>0&&p.getCreatedDate()!=null&&p.getName().equals("First"),"create and automatic date");
        check(service.findById(p.getId()).getCategory().getId()==1,"read category relation");
        var date=p.getCreatedDate();Product edit=product(c,"Updated");edit.setId(p.getId());check(service.update(edit),"update");
        check(service.findById(p.getId()).getImage().equals("product/existing.png")&&p.getCreatedDate().equals(date),"edit preserves image/date");
        for(int i=0;i<12;i++)service.insert(product(c,"Product "+i));
        check(service.count()==13,"count");check(service.findAll().size()==13,"findAll");
        var first=service.findByPage(1,6);var second=service.findByPage(2,6);
        check(first.size()==6&&second.size()==6&&Collections.disjoint(first,second),"pagination 6 per page");
        check(memory.offset==6&&memory.limit==6&&memory.query.contains("JOIN FETCH"),"DAO query pagination and eager relation");
        check(service.findByPage(3,6).size()==1,"last page");
        var newest=service.findNewest(10);check(newest.size()==10&&memory.limit==10&&memory.query.contains("p.createdDate DESC, p.id DESC"),"home newest 10 query");
        check(newest.get(0).getId()==13,"newest order");
        check(service.findById(999)==null&&!service.delete(999),"missing product safe");
        rejects(()->service.findById(-1),"invalid product ID");
        Product bad=product(new Category(999,"",null),"Bad");rejects(()->service.insert(bad),"invalid category ID");
        bad.setCategory(c);bad.setPrice(new BigDecimal("-1"));rejects(()->service.insert(bad),"negative price");
        bad.setPrice(BigDecimal.ZERO);rejects(()->service.insert(bad),"zero price");
        bad.setPrice(BigDecimal.ONE);bad.setName("  ");rejects(()->service.insert(bad),"empty name");
        check(service.delete(p.getId())&&service.findById(p.getId())==null,"delete product");
        check(ProductController.page(null,3)==1&&ProductController.page("abc",3)==1&&ProductController.page("-1",3)==1,"invalid page parameters");
        check(ProductController.page("99",3)==3&&ProductController.page("2",0)==1,"page overflow and empty data");
        auth(-1,"/admin/product/list",302);auth(5,"/admin/product/list",403);auth(1,"/admin/product/delete",200);
        auth(-1,"/admin/category/list",302);auth(5,"/admin/category/list",403);auth(1,"/admin/category/delete",200);
        auth(5,"/views/admin/list-category.jsp",403);
        if(args.length>0&&args[0].equals("--database"))database();
        else if(args.length>0&&args[0].equals("--mapping")){
            EntityManager em=JPAConfig.getEntityManager();
            try{
                check(em.getMetamodel().entity(Product.class).getAttribute("category").isAssociation(),"Hibernate association metamodel");
                em.createQuery("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id=:id",Product.class);
                em.createQuery("SELECT p FROM Product p JOIN FETCH p.category ORDER BY p.createdDate DESC, p.id DESC",Product.class);
                em.createQuery("SELECT COUNT(p) FROM Product p",Long.class);
                check(true,"Hibernate validates product JPQL (not executed)");
            }finally{em.close();JPAConfig.close();}
        }
        else System.out.println("SQL SERVER CRUD NOT RUN: apply migration manually, then use --database.");
        System.out.println("CHECKS PASSED: "+tests);
    }
    static void database()throws Exception {
        EntityManager em=JPAConfig.getEntityManager();EntityTransaction transaction=em.getTransaction();
        try {
            // Only reads until both required tables and an existing category are available.
            em.createQuery("SELECT COUNT(p) FROM Product p",Long.class).getSingleResult();
            List<Category> categories=em.createQuery("SELECT c FROM Category c",Category.class).setMaxResults(1).getResultList();
            if(categories.isEmpty())throw new IllegalStateException("Create a category manually before database checks.");
            transaction.begin();Category category=categories.get(0);
            EntityTransaction borrowedTx=(EntityTransaction)Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{EntityTransaction.class},(p,m,a)->m.getName().equals("isActive")?transaction.isActive():null);
            EntityManager borrowed=(EntityManager)Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{EntityManager.class},(p,m,a)->{
                if(m.getName().equals("close"))return null;if(m.getName().equals("getTransaction"))return borrowedTx;
                try{return m.invoke(em,a);}catch(InvocationTargetException e){throw e.getCause();}
            });
            ProductDaoImpl dao=new ProductDaoImpl(()->borrowed);ProductServiceImpl service=new ProductServiceImpl(dao,categories(category));
            long before=service.count();List<Integer> inserted=new ArrayList<>();
            for(int i=0;i<13;i++){Product p=product(category,"Product rollback test "+i);service.insert(p);inserted.add(p.getId());}
            em.flush();em.clear();check(service.count()==before+13,"SQL create/count");
            int id=inserted.get(0);Product p=service.findById(id);check(p.getCategory().getId()==category.getId(),"SQL relation/read");
            Product invalid=product(category," ");rejects(()->service.insert(invalid),"SQL invalid add rejected");
            Product invalidEdit=product(category,"Corrupted");invalidEdit.setId(id);invalidEdit.setPrice(BigDecimal.ZERO);
            rejects(()->service.update(invalidEdit),"SQL invalid edit rejected");
            em.clear();check(service.count()==before+13&&service.findById(id).getName().equals("Product rollback test 0"),"SQL invalid add/edit leave stored rows unchanged");
            rejects(()->service.delete(-1),"SQL invalid delete rejected");
            check(service.count()==before+13,"SQL invalid delete leaves all rows unchanged");
            p=service.findById(id);
            p.setName("Updated rollback test");service.update(p);em.flush();em.clear();check(service.findById(id).getName().equals("Updated rollback test"),"SQL update");
            check(service.findAll().size()==before+13,"SQL findAll");
            check(service.findByPage(1,6).size()==6&&service.findByPage(2,6).size()==6,"SQL pagination");
            check(service.findNewest(10).size()==10,"SQL newest 10");
            check(service.delete(id),"SQL delete test-created row only");em.flush();em.clear();check(service.findById(id)==null,"SQL deleted row absent");
        }finally{if(transaction.isActive())transaction.rollback();em.close();JPAConfig.close();System.out.println("Database test transaction rolled back; no migration executed.");}
    }
    static Object zero(Class<?> t){if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null;}
    static <T>T proxy(Class<T> t,InvocationHandler h){return t.cast(Proxy.newProxyInstance(CheckProduct.class.getClassLoader(),new Class[]{t},h));}
    static class Admin extends vn.iotstar.controller.ProductAdminController {
        Admin(vn.iotstar.service.ProductService s,vn.iotstar.service.CategoryService c){super(s,c);}
        void call(Http h,boolean post)throws Exception{if(post)doPost(h.req,h.resp);else doGet(h.req,h.resp);}
    }
    static class Http {
        Map<String,String> params=new HashMap<>(Map.of("name"," Product ","price","10000","categoryId","1","csrf","token","id","1"));
        Map<String,Object> attrs=new HashMap<>();String path="/admin/product/add",view;int status=200,parts;boolean csrf=true;Part image;
        HttpSession session=proxy(HttpSession.class,(p,m,a)->m.getName().equals("getAttribute")&&csrf?"token":zero(m.getReturnType()));
        HttpServletRequest req=proxy(HttpServletRequest.class,(p,m,a)->switch(m.getName()){
            case "getSession"->session;case "getServletPath"->path;case "getContextPath"->"/ServletCRUDMVC";
            case "getParameter"->params.get(a[0]);case "getAttribute"->attrs.get(a[0]);
            case "setAttribute"->{attrs.put((String)a[0],a[1]);yield null;}
            case "getPart"->{parts++;yield image;}
            case "getRequestDispatcher"->proxy(RequestDispatcher.class,(x,n,v)->{view=(String)a[0];return null;});
            default->zero(m.getReturnType());
        });
        HttpServletResponse resp=proxy(HttpServletResponse.class,(p,m,a)->{
            if(m.getName().equals("sendError"))status=(Integer)a[0];if(m.getName().equals("sendRedirect"))status=302;return zero(m.getReturnType());
        });
    }
    static Part image(String name,byte[] bytes){return proxy(Part.class,(p,m,a)->switch(m.getName()){
        case "getSubmittedFileName"->name;case "getSize"->(long)bytes.length;case "getInputStream"->new java.io.ByteArrayInputStream(bytes);default->zero(m.getReturnType());
    });}
    static long files(java.nio.file.Path root)throws Exception{try(var paths=java.nio.file.Files.walk(root)){return paths.filter(java.nio.file.Files::isRegularFile).count();}}
    static void inputChecks()throws Exception{
        var root=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("target"),"product-input-").toAbsolutePath();
        System.setProperty("app.upload.dir",root.toString());
        try{
            MemoryJpa memory=new MemoryJpa();Category category=new Category(1,"Category",null);
            ProductDaoImpl realDao=new ProductDaoImpl(memory);boolean[] fail={false};
            ProductDao dao=proxy(ProductDao.class,(p,m,a)->{
                if(fail[0]&&Set.of("insert","update","delete").contains(m.getName()))throw new IllegalStateException("Simulated database failure");
                try{return m.invoke(realDao,a);}catch(InvocationTargetException e){throw e.getCause();}
            });
            ProductServiceImpl service=new ProductServiceImpl(dao,categories(category));
            var catService=proxy(vn.iotstar.service.CategoryService.class,(p,m,a)->m.getName().equals("findAll")?List.of(category):null);
            Admin admin=new Admin(service,catService);
            Http valid=new Http();admin.call(valid,true);
            check(valid.status==302&&memory.rows.size()==1&&memory.rows.get(1).getName().equals("Product"),"controller adds normalized name, price 10000, existing category without optional image");
            for(String[] invalid:new String[][]{{"name",""},{"name","  "},{"name","x".repeat(256)},
                    {"price","0"},{"price","-1"},{"price","abc"},{"price",""},{"price","1.2.3"},{"price","NaN"},{"price","$10"},
                    {"price","10000000000000000"},{"price","1.001"},{"categoryId","999"},{"categoryId","abc"},{"categoryId",""},{"categoryId","0"}}){
                for(String route:List.of("add","edit")){
                    Http h=new Http();h.path="/admin/product/"+route;h.params.put(invalid[0],invalid[1]);admin.call(h,true);
                    check(h.status==200&&h.attrs.containsKey("alert")&&h.parts==0&&memory.rows.size()==1&&memory.rows.get(1).getName().equals("Product"),"invalid "+route+" "+invalid[0]+" rejected before upload without changing rows");
                    check(h.attrs.containsKey("product")&&h.attrs.get("productPriceInput").equals(h.params.get("price")),"invalid "+route+" preserves safe form values");
                }
            }
            for(String route:List.of("edit","delete"))for(String id:List.of("abc","0","-1","999")){
                Http h=new Http();h.path="/admin/product/"+route;h.params.put("id",id);admin.call(h,true);
                check(h.status==(id.equals("999")?404:400)&&memory.rows.size()==1,"invalid/nonexistent "+route+" ID cannot mutate");
            }
            for(String route:List.of("add","edit","delete")){
                Http h=new Http();h.path="/admin/product/"+route;h.csrf=false;admin.call(h,true);
                check(h.status==403&&h.parts==0&&memory.rows.size()==1,route+" requires CSRF");
            }
            Http get=new Http();get.path="/admin/product/delete";admin.call(get,false);check(get.status==405&&memory.rows.size()==1,"GET delete cannot mutate");
            var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
            byte[] png=bytes.toByteArray();
            for(Part invalid:List.of(image("fake.png",new byte[]{1,2,3}),image("fake.jpg",new byte[]{1,2,3}),image("image.svg",png),image("large.png",new byte[5*1024*1024+1]))){
                Http h=new Http();h.image=invalid;admin.call(h,true);check(h.attrs.containsKey("alert")&&memory.rows.size()==1&&files(root)==0,"invalid image rejected without saved file or row");
            }
            Http edit=new Http();edit.path="/admin/product/edit";edit.image=image("../../image.png",png);admin.call(edit,true);
            String old=memory.rows.get(1).getImage();check(edit.status==302&&files(root)==1&&old.startsWith("product/"),"valid image edit stores generated safe path");
            Http unchanged=new Http();unchanged.path=edit.path;admin.call(unchanged,true);check(memory.rows.get(1).getImage().equals(old),"edit with no image preserves old image");
            fail[0]=true;
            for(String route:List.of("add","edit","delete")){
                Http h=new Http();h.path="/admin/product/"+route;h.image=image("new.png",png);admin.call(h,true);
                check(memory.rows.size()==1&&memory.rows.get(1).getImage().equals(old)&&files(root)==1,"failed "+route+" preserves old row/image and cleans new upload");
                if(route.equals("delete"))check(h.status==503&&h.view==null,"failed delete returns 503 rather than Add form");
                else check(((Product)h.attrs.get("product")).getImage()==null,"failed save form does not preview discarded new file");
            }
            fail[0]=false;Http replacement=new Http();replacement.path=edit.path;replacement.image=image("replacement.png",png);admin.call(replacement,true);
            check(replacement.status==302&&!memory.rows.get(1).getImage().equals(old)&&files(root)==2,"successful replacement updates reference; existing design retains old file");
            Http delete=new Http();delete.path="/admin/product/delete";admin.call(delete,true);check(delete.status==302&&memory.rows.isEmpty()&&files(root)==2,"POST delete removes row; retained files are never deleted via user path");
        }finally{try(var paths=java.nio.file.Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(path);}}
    }
}
