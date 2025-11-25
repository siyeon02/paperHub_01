package capstone.paperhub_01.controller.graph;

import capstone.paperhub_01.controller.graph.response.GraphResp;
import capstone.paperhub_01.controller.recommend.response.RecommendResp;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.GraphService;
import capstone.paperhub_01.service.RecommendationService;
import capstone.paperhub_01.util.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;
    private final RecommendationService recommendationService;

    @GetMapping("/graph/{arxivId}")
    public ResponseEntity<ApiResult<GraphResp>> getGraph(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("arxivId") String paperInfoId) {
        // 1. Pinecone에서 추천 받아오기
        List<RecommendResp> recs = recommendationService.getSimilarPapers(paperInfoId, 10);
        System.out.println("Pinecone recs: " + recs.size());
        recs.forEach(r -> System.out.println(r.getArxivId() + " / " + r.getScore()));

        // 2. 그래프 변환
        GraphResp graph = graphService.buildPaperGraph(paperInfoId, recs);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(graph));
    }
}
