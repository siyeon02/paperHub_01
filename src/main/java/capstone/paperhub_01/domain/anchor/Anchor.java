package capstone.paperhub_01.domain.anchor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "anchors",
        uniqueConstraints = @UniqueConstraint(name = "uq_anchor_sig",
                columnNames = {"paperSha256", "page", "signature"}), // signature = quoteHash 또는 rects 해시
        indexes = {
                @Index(name = "idx_anchor_sha256", columnList = "paperSha256"),
                @Index(name = "idx_anchor_sha256_page", columnList = "paperSha256,page")
        })

public class Anchor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String paperSha256;

    @Column(nullable = false)
    private Integer page;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> rects;

    @Column(columnDefinition = "text")
    String exact;
    @Column(columnDefinition = "text")
    String prefix;
    @Column(columnDefinition = "text")
    String suffix;

    @Column(length = 96)
    String signature; // quoteHash or rectHash
    @Column(length = 64, nullable = false)
    String createdBy;
    @Column(nullable = false)
    OffsetDateTime createdAt;
}
