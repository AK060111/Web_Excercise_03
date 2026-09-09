package vn.iotstar.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "Category")
@NamedQuery(name = "Category.findAll", query = "SELECT c FROM Category c")
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cate_id")
    private int id;

    @Column(name = "cate_name", columnDefinition = "nvarchar(255) not null")
    private String name;

    @Column(name = "icons", columnDefinition = "nvarchar(255) null")
    private String icon;

    public Category() { }

    public Category(int id, String name, String icon) { this.id = id; this.name = name; this.icon = icon; }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }

    public void setIcon(String icon) { this.icon = icon; }
}
