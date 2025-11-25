package capstone.paperhub_01.domain.paper;

import capstone.paperhub_01.domain.category.Category;
import capstone.paperhub_01.domain.category.PaperCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "papers", indexes = {
        @Index(name = "idx_papers_fingerprint", columnList = "fingerprint")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_papers_sha256", columnNames = "sha256")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String sha256;

    @Column(length = 64)
    private String fingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> infoJson;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> xmpJson;

    @Column(nullable = false)
    private Integer numPages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> pageSizes;

    @Column(nullable = false)
    private String storageUri;

    private String uploaderId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @OneToOne(mappedBy = "paper", fetch = LAZY, cascade = CascadeType.ALL, optional = true)
    private PaperInfo paperInfo;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "primary_category_code",
            foreignKey = @ForeignKey(name="fk_papers_primary_category"))
    private Category primaryCategory;

    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PaperCategory> secondaryCategories = new HashSet<>();

    public void setPrimaryCategory(Category cat) { this.primaryCategory = cat; }

    public void replaceSecondaryCategories(Collection<Category> cats) {
        secondaryCategories.clear();
        if (cats != null) for (Category c : cats) secondaryCategories.add(PaperCategory.link(this, c));
    }

    public void attachInfo(PaperInfo info) {
        this.paperInfo = info;
        info.setPaper(this);
    }
}
