package capstone.paperhub_01.domain.category;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.LinkedHashSet;
import java.util.Set;


@Entity
@Getter
@NoArgsConstructor
@Table(name = "category")
public class Category {
    @Id
    @Column(length = 50)
    private String code;

    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code",
            referencedColumnName = "code",
            foreignKey = @ForeignKey(name = "fk_categories_parent"))
    private Category parent;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<PaperCategory> paperCategories = new LinkedHashSet<>();

    public static Category of(String code, String name, Category parent) {
        Category c = new Category();
        c.code = code;
        c.name = (name != null && !name.isBlank()) ? name : code;
        c.parent = parent;
        return c;
    }

    public void rename(String name) {
        this.name = (name != null && !name.isBlank()) ? name : this.code;
    }

}
