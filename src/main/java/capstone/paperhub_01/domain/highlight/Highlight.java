package capstone.paperhub_01.domain.highlight;

import capstone.paperhub_01.domain.anchor.Anchor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "highlights",
        indexes = {
                @Index(name = "idx_hl_anchor", columnList = "anchor_id"),
                @Index(name = "idx_hl_sha256_page", columnList = "paperSha256,page")
        })
public class Highlight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "anchor_id")
    private Anchor anchor;

    @Column(nullable = false, length = 64)
    private String paperSha256; // 중복 검색용
    @Column(nullable = false)
    private Integer page;

    @Column(length = 16)
    private String color; // "#FFE066"
    @Column(length = 64, nullable = false)
    private String createdBy;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
