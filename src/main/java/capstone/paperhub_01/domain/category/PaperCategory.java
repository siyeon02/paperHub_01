package capstone.paperhub_01.domain.category;

import capstone.paperhub_01.domain.paper.Paper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name="paper_category")
@Getter
@NoArgsConstructor
public class PaperCategory {
    @EmbeddedId
    private PaperCategoryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("paperId")
    @JoinColumn(name = "paper_id",
            foreignKey = @ForeignKey(name = "fk_paper_categories_paper"))
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryCode")
    @JoinColumn(name = "category_code",
            foreignKey = @ForeignKey(name = "fk_paper_categories_category"))
    private Category category;

    @Column(name = "linked_at", nullable = false)
    private OffsetDateTime linkedAt;

    private PaperCategory(Paper paper, Category category) {
        this.paper = paper;
        this.category = category;
        this.id = new PaperCategoryId(paper.getId(), category.getCode());
        this.linkedAt = OffsetDateTime.now();
    }

    public static PaperCategory link(Paper paper, Category category) {
        return new PaperCategory(paper, category);
    }


}
