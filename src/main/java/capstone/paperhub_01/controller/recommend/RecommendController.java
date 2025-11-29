package capstone.paperhub_01.controller.recommend;

import capstone.paperhub_01.controller.recommend.response.*;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.MultiFeatureRecommenderService;
import capstone.paperhub_01.service.RecommendationService;
import capstone.paperhub_01.util.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendController {
    private final RecommendationService recommendationService;
    private final MultiFeatureRecommenderService multiFeatureRecommenderService;



    @GetMapping("/similar/{searchId}")
    public ResponseEntity<ApiResult<List<PaperRecommendResp>>> recommendMultiFeature(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String searchId,
            @RequestParam(defaultValue = "10") int k
    ) {
        Long userId = userDetails.getUser().getId();

        // Stage1에서 넉넉하게 뽑을 후보 개수 (예: topK의 5배 or 최소 50개)
        int candidateSize = Math.max(k * 5, 50);

        // 2-stage + user_paper_stats 기반 개인화 추천
        List<PaperScoreDto> scored = multiFeatureRecommenderService
                .recommendPersonalized(
                        userId,
                        searchId,
                        candidateSize,   // Stage1: Pinecone + venue 필터로 뽑을 개수
                        k,               // Stage2: 최종 반환 개수
                        null             // excludeArxivIds (필요시 나중에 추가)
                );

        // PaperScoreDto → 응답 DTO 매핑
        List<PaperRecommendResp> respList = scored.stream()
                .map(dto -> new PaperRecommendResp(
                        dto.arxivId(),
                        dto.title(),
                        dto.venue(),
                        dto.venueType(),
                        dto.primaryCategory(),
                        dto.publishedDate(),
                        dto.totalScore()
                        // 혹시 프론트에서 feature별 점수도 보고 싶으면 여기에 추가 필드 넣어서 내려줘도 됨
                        // dto.scores().cosineSimilarity(),
                        // dto.scores().venueMatch(),
                        // dto.scores().categoryMatch(),
                        // dto.scores().recency(),
                        // dto.scores().userPreference()
                ))
                .toList();

        return ResponseEntity.ok(ApiResult.success(respList));
    }

    @GetMapping("/similar/{searchId}/{recId}/explanation")
    public ResponseEntity<ApiResult<MultiFeatureRecommendExplanationResp>> getExplanationMultiFeature(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String searchId,
            @PathVariable String recId
    ) {
        Long userId = userDetails.getUser().getId();

        MultiFeatureRecommendExplanationResp explanation =
                multiFeatureRecommenderService.getExplanationMultiFeature(userId, searchId, recId);

        return ResponseEntity.ok(ApiResult.success(explanation));
    }

    @GetMapping("/venue")
    public ResponseEntity<ApiResult<List<PaperRecommendResp>>> recommendByVenue(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String venue,
            @RequestParam(defaultValue = "10") int k
    ) {
        List<PaperScoreDto> papers = multiFeatureRecommenderService
                .recommendByVenue(venue, k);

        List<PaperRecommendResp> respList = papers.stream()
                .map(dto -> new PaperRecommendResp(
                        dto.arxivId(),
                        dto.title(),
                        dto.venue(),
                        dto.venueType(),
                        dto.primaryCategory(),
                        dto.publishedDate(),
                        dto.totalScore()
                ))
                .toList();

        return ResponseEntity.ok(ApiResult.success(respList));
    }

}
