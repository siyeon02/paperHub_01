package capstone.paperhub_01.domain.paper;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

    private String sourceId;

    private String title;
    private String author;

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
}
