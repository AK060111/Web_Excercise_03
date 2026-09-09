package vn.iotstar.dao;
import java.util.List;
import vn.iotstar.model.Category;
public interface CategoryDao {
    void insert(Category category);
    void update(Category category);
    void delete(int cateid) throws Exception;
    Category findById(int cateid);
    Category findByCategoryname(String name) throws Exception;
    List<Category> findAll();
    List<Category> searchByName(String catname);
    List<Category> findAll(int page, int pagesize);
    int count();

    default void edit(Category category) { update(category); }
    default Category get(int id) { return findById(id); }
    default Category get(String name) {
        try {
            return findByCategoryname(name);
        } catch (Exception e) {
            return null;
        }
    }
    default List<Category> getAll() { return findAll(); }
    default List<Category> search(String keyword) { return searchByName(keyword); }
}
