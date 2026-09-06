package vn.iotstar.service.impl;
import java.util.List;
import vn.iotstar.dao.CategoryDao;
import vn.iotstar.dao.impl.CategoryDaoImpl;
import vn.iotstar.model.Category;
import vn.iotstar.service.CategoryService;
public class CategoryServiceImpl implements CategoryService {
    private final CategoryDao dao=new CategoryDaoImpl();
    public void insert(Category c){if(findByCategoryname(c.getName())==null)dao.insert(c);}
    public void update(Category c){Category old=findById(c.getId());if(old==null)throw new IllegalArgumentException("Danh mục không tồn tại");old.setName(c.getName());if(c.getIcon()!=null)old.setIcon(c.getIcon());dao.update(old);}
    public void delete(int id){try{dao.delete(id);}catch(Exception e){throw new IllegalStateException("Không thể xóa danh mục",e);}}
    public Category findById(int id){return dao.findById(id);}
    public Category findByCategoryname(String n){try{return dao.findByCategoryname(n);}catch(Exception e){throw new IllegalStateException("Không thể tìm danh mục",e);}}
    public List<Category> findAll(){return dao.findAll();}
    public List<Category> searchByName(String k){return dao.searchByName(k);}
    public List<Category> findAll(int page,int pagesize){return dao.findAll(page,pagesize);}
    public int count(){return dao.count();}
}

