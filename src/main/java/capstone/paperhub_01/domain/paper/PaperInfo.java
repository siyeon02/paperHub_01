package capstone.paperhub_01.domain.paper;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "paper_infos")
@Getter
@NoArgsConstructor
@Setter
@AllArgsConstructor
public class PaperInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", foreignKey = @ForeignKey(name="fk_paper_infos_paper_id"))
    private Paper paper;

    private String title;

    @Column(unique = true)
    private String arxivId;

    @Column(columnDefinition = "text")
    private String abstractText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String authors; // JSON 문자열 ["A B","C D"]

    private LocalDate publishedDate;

    private OffsetDateTime updatedDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String categories;

    private String primaryCategory;

    private String pdfUrl;


    /* package-private */ void setPaper(Paper p) { this.paper = p; }

    // 업데이트 전용 도메인 메서드
    public void upsertFrom(PaperInfoUpsert up) {
        if (up.title() != null) this.title = up.title();
        if (up.arxivId() != null) this.arxivId = up.arxivId();
        if (up.abstractText() != null) this.abstractText = up.abstractText();
        if (up.authorsJson() != null) this.authors = up.authorsJson();
        if (up.publishedDate() != null) this.publishedDate = up.publishedDate();
        if (up.updatedDate() != null) this.updatedDate = up.updatedDate();
        if (up.categoriesJson() != null) this.categories = up.categoriesJson();
        if (up.primaryCategory() != null) this.primaryCategory = up.primaryCategory();
        if (up.pdfUrl() != null) this.pdfUrl = up.pdfUrl();
    }

    // 값 객체(업서트 입력용)
    public record PaperInfoUpsert(
            String title,
            String arxivId,
            String abstractText,
            String authorsJson,
            LocalDate publishedDate,
            OffsetDateTime updatedDate,
            String categoriesJson,
            String primaryCategory,
            String pdfUrl
    ) {}
}

