package vn.iotstar.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import jakarta.persistence.*;

@Entity
@Table(name="products",schema="dbo")
public class Product implements Serializable {
    private static final long serialVersionUID=1L;
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="product_id")
    private int id;
    @Column(name="product_name",nullable=false,columnDefinition="nvarchar(255)")
    private String name;
    @Column(name="description",columnDefinition="nvarchar(max)")
    private String description;
    @Column(name="price",nullable=false,precision=18,scale=2)
    private BigDecimal price;
    @Column(name="image",columnDefinition="nvarchar(255)")
    private String image;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="category_id",nullable=false,referencedColumnName="cate_id")
    private Category category;
    @Column(name="created_date",nullable=false,updatable=false)
    private LocalDateTime createdDate;
    @PrePersist
    public void onCreate(){if(createdDate==null)createdDate=LocalDateTime.now(ZoneOffset.UTC);}
    public int getId(){return id;} public void setId(int v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
    public String getImage(){return image;} public void setImage(String v){image=v;}
    public Category getCategory(){return category;} public void setCategory(Category v){category=v;}
    public LocalDateTime getCreatedDate(){return createdDate;}
    public String getCreatedDateDisplay(){return createdDate==null?"":createdDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))+" UTC";}
}
