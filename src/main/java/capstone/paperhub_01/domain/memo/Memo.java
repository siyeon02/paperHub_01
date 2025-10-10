package capstone.paperhub_01.domain.memo;

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
@Table(name = "notes",
        indexes = {
                @Index(name = "idx_note_anchor", columnList = "anchor_id"),
                @Index(name = "idx_note_sha256_page", columnList = "paper_Sha256,page")
        })
public class Memo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anchor_id")
    private Anchor anchor;

    // 문서/페이지 맥락 조회를 위해 보조 컬럼 유지
    @Column(nullable = false, length = 64)
    private String paperSha256;
    @Column(nullable = false)
    private Integer page;

    @Column(columnDefinition = "text", nullable = false)
    private String body;
    @Column(length = 64, nullable = false)
    private String createdBy;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // 스레드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Memo parent;
}
