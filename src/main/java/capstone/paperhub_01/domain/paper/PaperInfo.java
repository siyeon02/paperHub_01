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
import java.util.List;

@Entity
@Table(name = "paper_infos",
        uniqueConstraints = {
                @UniqueConstraint(name="uq_paper_infos_paper_id", columnNames = "paper_id")
})
@Getter
@NoArgsConstructor
@Setter
@AllArgsConstructor
public class PaperInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* package-private */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "paper_id", nullable = true, foreignKey = @ForeignKey(name="fk_paper_infos_paper_id"))
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

    private LocalDate updatedDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> categories;

    private String primaryCategory;

    private String pdfUrl;

    @Column(length = 500)
    private String venue;

    @Column(length = 50)
    private String venueType;



    // 값 객체(업서트 입력용)
    public record PaperInfoUpsert(
            String title,
            String arxivId,
            String abstractText,
            String authorsJson,
            LocalDate publishedDate,
            LocalDate updatedDate,
            String categoriesJson,
            String primaryCategory,
            String pdfUrl
    ) {}
}

