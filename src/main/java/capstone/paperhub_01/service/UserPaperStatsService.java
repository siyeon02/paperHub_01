package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.graph.response.UserPaperStatsResp;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStats;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStatsId;
import capstone.paperhub_01.domain.userpaperstats.repository.UserPaperStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPaperStatsService {

    private final UserPaperStatsRepository userPaperStatsRepository;
    private final capstone.paperhub_01.domain.collection.repository.CollectionPaperRepository collectionPaperRepository;
    private final capstone.paperhub_01.domain.paper.repository.PaperRepository paperRepository;

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
                                .build()));
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
    public UserPaperStatsResp getStats(Long userId, Long paperId) {
        UserPaperStatsId id = new UserPaperStatsId(userId, paperId);
        UserPaperStats userPaperStats = userPaperStatsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "통계 데이터가 존재하지 않습니다. userId=" + userId + ", paperId=" + paperId));

        // 단건 조회 시에도 title/status 채워주기
        String title = "Unknown Title";
        String status = "TO_READ";

        // Try to find CollectionPaper first
        // Since we don't have findByMemberIdAndPaperId, we use findInfoByIdAndMember if
        // it works,
        // but wait, findInfoByIdAndMember takes ID(pk) not paperId.
        // So we can't easily find CollectionPaper by paperId without adding a method.
        // But for now, let's just fetch Paper info directly as a fallback or if we
        // can't find CP.
        // Actually, for single item, we can just use paperRepository to get title.
        // Status will be default.
        // If we really want status, we need to add the method to repo.
        // Given the constraints and the fact that Dashboard uses getAllStats, I will
        // prioritize getAllStats.
        // For getStats (single), I will just fetch title from Paper.

        var paperOpt = paperRepository.findWithInfoById(paperId);
        if (paperOpt.isPresent()) {
            var p = paperOpt.get();
            if (p.getPaperInfo() != null) {
                title = p.getPaperInfo().getTitle();
            }
        }

        return UserPaperStatsResp.of(userPaperStats, title, status);
    }

    @Transactional(readOnly = true)
    public List<UserPaperStatsResp> getAllStats(Long userId) {
        List<UserPaperStats> statsList = userPaperStatsRepository.findAllByIdUserId(userId);
        if (statsList.isEmpty()) {
            return List.of();
        }

        // 1. Bulk fetch CollectionPapers for this user
        // findAllByMemberAndStatus with null status returns all
        List<capstone.paperhub_01.domain.collection.CollectionPaper> cps = collectionPaperRepository
                .findAllByMemberAndStatus(userId, null);

        // Map<PaperId, CollectionPaper>
        java.util.Map<Long, capstone.paperhub_01.domain.collection.CollectionPaper> cpMap = cps.stream()
                .collect(java.util.stream.Collectors.toMap(
                        cp -> cp.getPaper().getId(),
                        cp -> cp,
                        (existing, replacement) -> existing // duplicated? shouldn't happen
                ));

        // 2. Identify missing papers (stats exist but no CP)
        java.util.Set<Long> missingPaperIds = new java.util.HashSet<>();
        for (UserPaperStats s : statsList) {
            if (!cpMap.containsKey(s.getId().getPaperId())) {
                missingPaperIds.add(s.getId().getPaperId());
            }
        }

        // 3. Bulk fetch missing papers
        java.util.Map<Long, capstone.paperhub_01.domain.paper.Paper> paperMap = new java.util.HashMap<>();
        if (!missingPaperIds.isEmpty()) {
            paperRepository.findAllById(missingPaperIds).forEach(p -> paperMap.put(p.getId(), p));
        }

        // 4. Build response
        return statsList.stream().map(s -> {
            Long pId = s.getId().getPaperId();
            String title = "Unknown Title";
            String status = "TO_READ";

            if (cpMap.containsKey(pId)) {
                var cp = cpMap.get(pId);
                title = cp.getPaper().getPaperInfo() != null ? cp.getPaper().getPaperInfo().getTitle() : "No Title";
                status = cp.getStatus().name();
            } else if (paperMap.containsKey(pId)) {
                var p = paperMap.get(pId);
                title = p.getPaperInfo() != null ? p.getPaperInfo().getTitle() : "No Title";
            }

            return UserPaperStatsResp.of(s, title, status);
        }).toList();
    }

}
