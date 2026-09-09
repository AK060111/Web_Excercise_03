package vn.iotstar.dao;
import java.util.List;
import vn.iotstar.model.Product;
public interface ProductDao {
    void insert(Product product);
    boolean update(Product product);
    boolean delete(int id);
    Product findById(int id);
    List<Product> findAll();
    long count();
    List<Product> findByPage(int page,int pageSize);
    List<Product> findNewest(int limit);
}
