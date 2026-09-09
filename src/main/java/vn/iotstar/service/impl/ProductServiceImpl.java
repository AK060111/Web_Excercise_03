package vn.iotstar.service.impl;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import vn.iotstar.dao.ProductDao;
import vn.iotstar.dao.CategoryDao;
import vn.iotstar.dao.impl.ProductDaoImpl;
import vn.iotstar.dao.impl.CategoryDaoImpl;
import vn.iotstar.model.Product;
import vn.iotstar.model.Category;
import vn.iotstar.service.ProductService;
public class ProductServiceImpl implements ProductService {
    private final ProductDao dao;private final CategoryDao categories;
    public ProductServiceImpl(){this(new ProductDaoImpl(),new CategoryDaoImpl());}
    public ProductServiceImpl(ProductDao dao,CategoryDao categories){this.dao=dao;this.categories=categories;}
    public void validate(Product p){
        if(p==null)throw new IllegalArgumentException("Sản phẩm không hợp lệ.");
        p.setName(vn.iotstar.util.ValidationUtil.required(p.getName(),"Tên sản phẩm",255));
        if(p.getPrice()==null||p.getPrice().signum()<=0||p.getPrice().compareTo(new BigDecimal("9999999999999999.99"))>0)
            throw new IllegalArgumentException("Giá phải lớn hơn 0 và tối đa 9999999999999999.99.");
        try{p.setPrice(p.getPrice().setScale(2,RoundingMode.UNNECESSARY));}
        catch(ArithmeticException e){throw new IllegalArgumentException("Giá chỉ có tối đa 2 chữ số thập phân.");}
        if(p.getCategory()==null||p.getCategory().getId()<1)throw new IllegalArgumentException("Vui lòng chọn danh mục.");
        Category c=categories.findById(p.getCategory().getId());
        if(c==null)throw new IllegalArgumentException("Danh mục không tồn tại.");
        p.setCategory(c);
    }
    public void insert(Product p){validate(p);p.setId(0);dao.insert(p);}
    public boolean update(Product p){id(p.getId());validate(p);return dao.update(p);}
    public boolean delete(int value){id(value);return dao.delete(value);}
    public Product findById(int value){id(value);return dao.findById(value);}
    public List<Product> findAll(){return dao.findAll();}
    public long count(){return dao.count();}
    public List<Product> findByPage(int page,int size){return dao.findByPage(page,size);}
    public List<Product> findNewest(int limit){return dao.findNewest(limit);}
    private void id(int value){if(value<1)throw new IllegalArgumentException("ID sản phẩm không hợp lệ.");}
}
