package vn.iotstar.dao.impl;

import java.util.List;
import java.util.function.Supplier;
import jakarta.persistence.*;
import vn.iotstar.config.JPAConfig;
import vn.iotstar.dao.ProductDao;
import vn.iotstar.model.Product;
import vn.iotstar.model.Category;

public class ProductDaoImpl implements ProductDao {
    private final Supplier<EntityManager> managers;
    private static final String SELECT="SELECT p FROM Product p JOIN FETCH p.category ";
    private static final String ORDER="ORDER BY p.createdDate DESC, p.id DESC";
    public ProductDaoImpl(){this(JPAConfig::getEntityManager);}
    public ProductDaoImpl(Supplier<EntityManager> managers){this.managers=managers;}
    public void insert(Product p){
        EntityManager em=managers.get();EntityTransaction tx=em.getTransaction();
        try{tx.begin();p.setCategory(em.getReference(Category.class,p.getCategory().getId()));em.persist(p);tx.commit();}
        catch(RuntimeException e){if(tx.isActive())tx.rollback();throw new IllegalStateException("Không thể thêm sản phẩm. Kiểm tra danh mục và dữ liệu.");}
        finally{em.close();}
    }
    public boolean update(Product p){
        EntityManager em=managers.get();EntityTransaction tx=em.getTransaction();
        try{
            tx.begin();Product old=em.find(Product.class,p.getId());
            if(old==null){tx.commit();return false;}
            old.setName(p.getName());old.setDescription(p.getDescription());old.setPrice(p.getPrice());
            old.setCategory(em.getReference(Category.class,p.getCategory().getId()));
            if(p.getImage()!=null)old.setImage(p.getImage());
            tx.commit();return true;
        }catch(RuntimeException e){if(tx.isActive())tx.rollback();throw new IllegalStateException("Không thể sửa sản phẩm. Kiểm tra danh mục và dữ liệu.");}
        finally{em.close();}
    }
    public boolean delete(int id){
        EntityManager em=managers.get();EntityTransaction tx=em.getTransaction();
        try{tx.begin();Product p=em.find(Product.class,id);if(p!=null)em.remove(p);tx.commit();return p!=null;}
        catch(RuntimeException e){if(tx.isActive())tx.rollback();throw new IllegalStateException("Không thể xóa sản phẩm.");}
        finally{em.close();}
    }
    public Product findById(int id){
        EntityManager em=managers.get();
        try{return em.createQuery(SELECT+"WHERE p.id=:id",Product.class).setParameter("id",id).getResultList().stream().findFirst().orElse(null);}
        finally{em.close();}
    }
    public List<Product> findAll(){return list(0,null);}
    public List<Product> findByPage(int page,int size){
        if(page<1||size<1)throw new IllegalArgumentException("Trang không hợp lệ");
        return list(Math.multiplyExact(page-1,size),size);
    }
    public List<Product> findNewest(int limit){if(limit<1)throw new IllegalArgumentException("Giới hạn không hợp lệ");return list(0,limit);}
    private List<Product> list(int offset,Integer limit){
        EntityManager em=managers.get();
        try{TypedQuery<Product> query=em.createQuery(SELECT+ORDER,Product.class);
            if(limit!=null)query.setFirstResult(offset).setMaxResults(limit);
            return query.getResultList();
        }finally{em.close();}
    }
    public long count(){EntityManager em=managers.get();try{return em.createQuery("SELECT COUNT(p) FROM Product p",Long.class).getSingleResult();}finally{em.close();}}
}
