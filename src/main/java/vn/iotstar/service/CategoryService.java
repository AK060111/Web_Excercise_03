package vn.iotstar.service;
import java.util.List;
import vn.iotstar.model.Category;
public interface CategoryService {
    void validate(Category category);
    void insert(Category category);
    void update(Category category);
    void delete(int id);
    Category findById(int cateid);
    Category findByCategoryname(String name);
    List<Category> findAll();
    List<Category> searchByName(String catname);
    List<Category> findAll(int page, int pagesize);
    int count();

    default void edit(Category category) { update(category); }
    default Category get(int id) { return findById(id); }
    default Category get(String name) { return findByCategoryname(name); }
    default List<Category> getAll() { return findAll(); }
    default List<Category> search(String keyword) { return searchByName(keyword); }
}
