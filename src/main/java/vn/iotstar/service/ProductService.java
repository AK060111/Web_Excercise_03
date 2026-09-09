package vn.iotstar.service;
import java.util.List;
import vn.iotstar.model.Product;
public interface ProductService {
    void validate(Product product);
    void insert(Product product);
    boolean update(Product product);
    boolean delete(int id);
    Product findById(int id);
    List<Product> findAll();
    long count();
    List<Product> findByPage(int page,int size);
    List<Product> findNewest(int limit);
}
