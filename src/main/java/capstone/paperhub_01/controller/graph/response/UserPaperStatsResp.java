package capstone.paperhub_01.controller.graph.response;

import capstone.paperhub_01.domain.userpaperstats.UserPaperStats;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserPaperStatsResp {
    // ==== Key ====
    private Long userId;
    private Long paperId;

    // ==== 시간/세션 ====
    private LocalDateTime firstOpenedAt;
    private LocalDateTime lastOpenedAt;
    private int totalOpenCount;
    private int totalReadTimeSec;
    private int totalSessions;
    private int maxSessionTimeSec;
    private int avgSessionTimeSec;

    // ==== 읽기 진행도 ====
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==== 최적화 필드 ====
    private String title;
    private String status;

    public static UserPaperStatsResp from(UserPaperStats s) {
        return UserPaperStatsResp.builder()
                .userId(s.getId().getUserId())
                .paperId(s.getId().getPaperId())

                .firstOpenedAt(s.getFirstOpenedAt())
                .lastOpenedAt(s.getLastOpenedAt())
                .totalOpenCount(s.getTotalOpenCount())
                .totalReadTimeSec(s.getTotalReadTimeSec())
                .totalSessions(s.getTotalSessions())
                .maxSessionTimeSec(s.getMaxSessionTimeSec())
                .avgSessionTimeSec(s.getAvgSessionTimeSec())

                .lastPageRead(s.getLastPageRead())
                .maxPageReached(s.getMaxPageReached())
                .pageCount(s.getPageCount())
                .completionRatio(s.getCompletionRatio())
                .isCompleted(s.getIsCompleted())

                .totalHighlightCount(s.getTotalHighlightCount())
                .totalMemoCount(s.getTotalMemoCount())
                .lastHighlightAt(s.getLastHighlightAt())
                .lastMemoAt(s.getLastMemoAt())

                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public static UserPaperStatsResp of(UserPaperStats s, String title, String status) {
        return UserPaperStatsResp.builder()
                .userId(s.getId().getUserId())
                .paperId(s.getId().getPaperId())

                .firstOpenedAt(s.getFirstOpenedAt())
                .lastOpenedAt(s.getLastOpenedAt())
                .totalOpenCount(s.getTotalOpenCount())
                .totalReadTimeSec(s.getTotalReadTimeSec())
                .totalSessions(s.getTotalSessions())
                .maxSessionTimeSec(s.getMaxSessionTimeSec())
                .avgSessionTimeSec(s.getAvgSessionTimeSec())

                .lastPageRead(s.getLastPageRead())
                .maxPageReached(s.getMaxPageReached())
                .pageCount(s.getPageCount())
                .completionRatio(s.getCompletionRatio())
                .isCompleted(s.getIsCompleted())

                .totalHighlightCount(s.getTotalHighlightCount())
                .totalMemoCount(s.getTotalMemoCount())
                .lastHighlightAt(s.getLastHighlightAt())
                .lastMemoAt(s.getLastMemoAt())

                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())

                .title(title)
                .status(status)
                .build();
    }

}
