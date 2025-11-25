package capstone.paperhub_01.domain.userpaperstats;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_paper_stats")
public class UserPaperStats {

    @EmbeddedId
    private UserPaperStatsId id;

    // ==== 시간/세션 관련 ====
    private LocalDateTime firstOpenedAt;
    private LocalDateTime lastOpenedAt;

    private int totalOpenCount;
    private int totalReadTimeSec;
    private int totalSessions;
    private int maxSessionTimeSec;
    private int avgSessionTimeSec;

    // ==== 진행도/완독 ====
    private Integer lastPageRead;
    private Integer maxPageReached;
    private Integer pageCount;
    private Double completionRatio;
    private Boolean isCompleted;

    // ==== 상호작용 ====
    private int totalHighlightCount;
    private int totalMemoCount;

    private LocalDateTime lastHighlightAt;
    private LocalDateTime lastMemoAt;

    // ==== 메타 ====
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    //한 세션 끝났을 때
    public void recordSession(int sessionSeconds, Integer lastPage, Integer maxPage, Integer totalPageCount) {
        totalSessions++;
        totalReadTimeSec += sessionSeconds;

        if (sessionSeconds > maxSessionTimeSec) {
            maxSessionTimeSec = sessionSeconds;
        }
        avgSessionTimeSec = (totalSessions == 0)
                ? 0
                : totalReadTimeSec / totalSessions;

        if (lastPage != null) {
            lastPageRead = lastPage;
        }
        if (maxPage != null) {
            if (maxPageReached == null || maxPage > maxPageReached) {
                maxPageReached = maxPage;
            }
        }
        if (totalPageCount != null) {
            pageCount = totalPageCount;
        }

        // completionRatio & isCompleted 갱신
        if (pageCount != null && pageCount > 0 && maxPageReached != null) {
            completionRatio = Math.min(1.0, maxPageReached / (double) pageCount);
            isCompleted = completionRatio >= 0.9;  // 예: 90% 이상이면 완독 처리
        }
    }

    /** 하이라이트 추가됐을 때 */
    public void addHighlight(LocalDateTime now) {
        totalHighlightCount++;
        lastHighlightAt = now;
    }

    /** 메모 추가됐을 때 */
    public void addMemo(LocalDateTime now) {
        totalMemoCount++;
        lastMemoAt = now;
    }


    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isCompleted == null) {
            isCompleted = false;
        }
    }

    public void recordOpen(LocalDateTime now) {
        if (firstOpenedAt == null) {
            firstOpenedAt = now;
        }
        lastOpenedAt = now;
        totalOpenCount++;
    }



}
