package capstone.paperhub_01.service;

import capstone.paperhub_01.domain.userpaperstats.UserPaperStats;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStatsId;
import capstone.paperhub_01.domain.userpaperstats.repository.UserPaperStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserPaperStatsService {

    private final UserPaperStatsRepository userPaperStatsRepository;


    @Transactional
    public UserPaperStats getOrCreate(Long userId, Long paperId) {
        UserPaperStatsId id = new UserPaperStatsId(userId, paperId);

        return userPaperStatsRepository.findById(id)
                .orElseGet(() -> userPaperStatsRepository.save(
                        UserPaperStats.builder()
                                .id(id)
                                // int 필드들은 기본값 0이지만, 명시해두면 더 읽기 좋음
                                .totalOpenCount(0)
                                .totalReadTimeSec(0)
                                .totalSessions(0)
                                .maxSessionTimeSec(0)
                                .avgSessionTimeSec(0)
                                .totalHighlightCount(0)
                                .totalMemoCount(0)
                                .isCompleted(false)
                                .build()
                ));
    }

    /**
     * 논문 뷰어를 열었을 때 호출.
     * - firstOpenedAt이 없으면 최초 열람 시간으로 설정
     * - lastOpenedAt 갱신
     * - totalOpenCount 증가
     */
    @Transactional
    public void recordOpen(Long userId, Long paperId) {
        LocalDateTime now = LocalDateTime.now();
        UserPaperStats stats = getOrCreate(userId, paperId);

        stats.recordOpen(now);
        // 별도 save() 필요 없음 – @Transactional + 변경 감지
    }

    /**
     * 한 읽기 세션이 끝났을 때 호출 (예: 탭 닫기, "읽기 종료" 버튼)
     *
     * @param userId         사용자 ID
     * @param paperId        논문 ID
     * @param sessionSeconds 이번 세션 동안 읽은 시간(초)
     * @param lastPage       마지막으로 본 페이지 (null 가능)
     * @param maxPage        이번 세션 중 가장 깊게 본 페이지 (null 가능)
     * @param pageCount      전체 페이지 수 (null 가능, paper 테이블과 중복이면 생략 가능)
     */
    @Transactional
    public void recordSession(Long userId,
                              Long paperId,
                              int sessionSeconds,
                              Integer lastPage,
                              Integer maxPage,
                              Integer pageCount) {

        UserPaperStats stats = getOrCreate(userId, paperId);
        stats.recordSession(sessionSeconds, lastPage, maxPage, pageCount);
    }

    /**
     * 하이라이트가 추가됐을 때 호출.
     */
    @Transactional
    public void addHighlight(Long userId, Long paperId) {
        LocalDateTime now = LocalDateTime.now();
        UserPaperStats stats = getOrCreate(userId, paperId);
        stats.addHighlight(now);
    }

    /**
     * 메모가 추가됐을 때 호출.
     */
    @Transactional
    public void addMemo(Long userId, Long paperId) {
        LocalDateTime now = LocalDateTime.now();
        UserPaperStats stats = getOrCreate(userId, paperId);
        stats.addMemo(now);
    }

    /**
     * 조회용 예시 – 필요하면 응답 DTO로 변환해서 반환하면 됨.
     */
    @Transactional(readOnly = true)
    public UserPaperStats getStats(Long userId, Long paperId) {
        UserPaperStatsId id = new UserPaperStatsId(userId, paperId);
        return userPaperStatsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("통계 데이터가 존재하지 않습니다. userId=" + userId + ", paperId=" + paperId));
    }
}
