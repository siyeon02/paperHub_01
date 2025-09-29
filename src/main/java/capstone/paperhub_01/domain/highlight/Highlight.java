package capstone.paperhub_01.domain.highlight;

import capstone.paperhub_01.domain.anchor.Anchor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "highlights",
        indexes = {
        @Index(name="idx_hl_anchor", columnList="anchor_id"),
        @Index(name="idx_hl_sha256_page", columnList="paperSha256,page")
        })
public class Highlight {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;

    @ManyToOne(optional=false, fetch= FetchType.LAZY)
    @JoinColumn(name="anchor_id")
    Anchor anchor;

    @Column(nullable=false, length=64) String paperSha256; // 중복 검색용
    @Column(nullable=false) Integer page;

    @Column(length=16) String color; // "#FFE066"
    @Column(length=16) String status; // "active|resolved|todo"
    @Column(length=64, nullable=false) String createdBy;
    @Column(nullable=false) OffsetDateTime createdAt;
    @Column(nullable=false)
    OffsetDateTime updatedAt;
}
