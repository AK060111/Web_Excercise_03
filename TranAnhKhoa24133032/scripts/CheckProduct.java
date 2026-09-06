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
        bad.setPrice(BigDecimal.ONE);bad.setName("  ");rejects(()->service.insert(bad),"empty name");
        check(service.delete(p.getId())&&service.findById(p.getId())==null,"delete product");
        check(ProductController.page(null,3)==1&&ProductController.page("abc",3)==1&&ProductController.page("-1",3)==1,"invalid page parameters");
        check(ProductController.page("99",3)==3&&ProductController.page("2",0)==1,"page overflow and empty data");
        auth(-1,"/admin/product/list",302);auth(5,"/admin/product/list",403);auth(1,"/admin/product/delete",200);
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
            p.setName("Updated rollback test");service.update(p);em.flush();em.clear();check(service.findById(id).getName().equals("Updated rollback test"),"SQL update");
            check(service.findAll().size()==before+13,"SQL findAll");
            check(service.findByPage(1,6).size()==6&&service.findByPage(2,6).size()==6,"SQL pagination");
            check(service.findNewest(10).size()==10,"SQL newest 10");
            check(service.delete(id),"SQL delete test-created row only");em.flush();em.clear();check(service.findById(id)==null,"SQL deleted row absent");
        }finally{if(transaction.isActive())transaction.rollback();em.close();JPAConfig.close();System.out.println("Database test transaction rolled back; no migration executed.");}
    }
}
