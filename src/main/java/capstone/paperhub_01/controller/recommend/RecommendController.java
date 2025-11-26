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

//    @GetMapping("/similar/{searchId}")
//    public ResponseEntity<ApiResult<List<RecommendResp>>> recommend(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable String searchId, @RequestParam(defaultValue = "10") int k) {
//        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(recommendationService.getSimilarPapers(searchId, k)));
//    }
//
//    @GetMapping("/similar/{searchId}/{recId}/explanation")
//    public ResponseEntity<ApiResult<RecommendExplanationResp>> getExplanation(
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @PathVariable String searchId,
//            @PathVariable String recId
//    ) {
//        return ResponseEntity.ok(ApiResult.success(
//                recommendationService.getExplanation(searchId, recId)
//        ));
//    }


    //------멀티피쳐 추천
    /**
     * 1) 유사 논문 추천
     *    - 내부에서는 MultiFeatureRecommenderService.recommendPapers() 사용
     */
    @GetMapping("/similar/{searchId}")
    public ResponseEntity<ApiResult<List<PaperRecommendResp>>> recommendMultiFeature(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String searchId,
            @RequestParam(defaultValue = "10") int k
    ) {
        Long userId = userDetails.getUser().getId(); // 필요하면 서비스로 넘겨서 개인화도 가능

        // MultiFeatureRecommenderService가 반환하는 DTO 타입에 맞춰서 가져오기
        List<PaperScoreDto> scored = multiFeatureRecommenderService
                .recommendPapers(searchId, k, null);

        // ⬇️ 여기서 PaperScoreDto → RecommendResp 로 매핑
        //    (RecommendResp 필드 구조에 맞게 수정해서 사용)
        List<PaperRecommendResp> respList = scored.stream()
                .map(dto -> new PaperRecommendResp(
                        dto.arxivId(),
                        dto.title(),
                        dto.venue(),
                        dto.venueType(),
                        dto.primaryCategory(),
                        dto.publishedDate(),
                        dto.totalScore()
                        // 필요하면 feature별 점수도 DTO에 추가해서 내려줄 수 있음
                        // dto.scores().cosineSimilarity(),
                        // dto.scores().venueMatch(),
                        // ...
                ))
                .toList();

        return ResponseEntity.ok(ApiResult.success(respList));
    }

    /**
     * 2) 추천 이유 설명 API
     *    - 기존 getExplanation을, 새 점수/리랭킹 로직을 쓰도록 서비스 쪽에서만 수정
     *    - 컨트롤러는 userId만 추가로 넘겨주는 형태가 깔끔
     */
    @GetMapping("/similar/{searchId}/{recId}/explanation")
    public ResponseEntity<ApiResult<MultiFeatureRecommendExplanationResp>> getExplanationMultiFeature(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String searchId,
            @PathVariable String recId
    ) {
        Long userId = userDetails.getUser().getId();

        // 서비스 시그니처를 이렇게 바꿨다고 가정:
        // RecommendExplanationResp getExplanation(Long userId, String searchId, String recId)
        MultiFeatureRecommendExplanationResp explanation =
                multiFeatureRecommenderService.getExplanationMultiFeature(userId, searchId, recId);

        return ResponseEntity.ok(ApiResult.success(explanation));
    }

    /**
     * 3) venue 기반 추천 (예: CVPR)
     *    - MultiFeatureRecommenderService.recommendByVenue() 사용
     *    - 필요하면 엔드포인트 추가
     */
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
