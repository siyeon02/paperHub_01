package capstone.paperhub_01.domain.collection;

import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.paper.Paper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "collection_papers",
        indexes = {
                @Index(name="idx_cp_member_status", columnList="member_id,status"),
                @Index(name="idx_cp_paper", columnList="paper_id")
        })
public class CollectionPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReadingStatus status; // TO_READ, IN_PROGRESS, DONE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tags;

    private OffsetDateTime lastOpenedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
