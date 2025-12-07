package capstone.paperhub_01.controller.graph;

import capstone.paperhub_01.controller.graph.response.GraphResp;
import capstone.paperhub_01.controller.graph.response.UserPaperStatsResp;
import capstone.paperhub_01.controller.recommend.response.PaperScoreDto;
import capstone.paperhub_01.domain.collection.ReadingStatus;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.CollectionService;
import capstone.paperhub_01.service.GraphService;
import capstone.paperhub_01.service.MultiFeatureRecommenderService;
import capstone.paperhub_01.service.UserPaperStatsService;
import capstone.paperhub_01.util.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GraphController {

    private final GraphService graphService;
    private final MultiFeatureRecommenderService multiFeatureRecommenderService;
    private final CollectionService collectionService;
    private final UserPaperStatsService userPaperStatsService;

    @GetMapping("/graph/{arxivId}")
    public ResponseEntity<ApiResult<GraphResp>> getGraph(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("arxivId") String arxivId) {
        Long userId = userDetails.getUser().getId();

        int k = 10; // 그래프에 보여줄 추천 노드 개수
        int candidateSize = Math.max(k * 5, 50); // Stage1에서 넉넉히 뽑을 후보 개수

        List<PaperScoreDto> recs = multiFeatureRecommenderService.recommendPersonalized(
                userId,
                arxivId,
                candidateSize, // Stage1: Pinecone + venue 필터 후보 개수
                k, // Stage2: 최종 top-k
                null // excludeArxivIds
        );

        log.info("[GRAPH] recs size = {}", recs.size());

        GraphResp graph = graphService.buildPaperGraphMultiFeature(arxivId, recs);

        return ResponseEntity.ok(ApiResult.success(graph));
    }

    @GetMapping("/papers/{paperId}/userStats")
    public ResponseEntity<ApiResult<UserPaperStatsResp>> getStats(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long paperId) {
        Long userId = user.getUser().getId();
        UserPaperStatsResp resp = userPaperStatsService.getStats(userId, paperId);
        return ResponseEntity.ok(ApiResult.success(resp));
    }

    @GetMapping("/userStats")
    public ResponseEntity<ApiResult<List<UserPaperStatsResp>>> getAllStats(
            @AuthenticationPrincipal UserDetailsImpl user) {
        Long userId = user.getUser().getId();
        return ResponseEntity.ok(
                ApiResult.success(userPaperStatsService.getAllStats(userId))
        );
    }

    @GetMapping("/graph/library")
    public ResponseEntity<ApiResult<GraphResp>> getLibraryGraph(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(value = "status", required = false) String status) {
        Long userId = userDetails.getUser().getId();
        ReadingStatus readingStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                readingStatus = ReadingStatus.valueOf(status.toUpperCase().replace("-", "_"));
            } catch (Exception e) {
                // ignore invalid status values and fall back to all
            }
        }

        var papers = collectionService.retrieveAllCollectionPapers(userId, readingStatus);
        GraphResp graph = graphService.buildLibraryGraph(papers);

        return ResponseEntity.ok(ApiResult.success(graph));
    }
}
