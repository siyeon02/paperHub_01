package capstone.paperhub_01.domain.collection;

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
                @Index(name="idx_cp_collection_status", columnList="collection_id,status"),
                @Index(name="idx_cp_paper", columnList="paper_id")
        })
public class CollectionPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 '컬렉션(폴더)'에 속하는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collectionfolder_id")
    private CollectionFolder collection;  // 별도 폴더 엔티티(간단히 id+name만 있어도 됨)


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReadingStatus status; // TO_READ, IN_PROGRESS, DONE

    @JdbcTypeCode(SqlTypes.JSON) // hibernate-types 사용 시 jsonb 매핑
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tags;

    @Column(nullable = false)
    private int highlightCount = 0;

    private OffsetDateTime lastOpenedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
