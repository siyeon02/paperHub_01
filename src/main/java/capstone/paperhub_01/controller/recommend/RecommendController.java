package capstone.paperhub_01.controller.recommend;

import capstone.paperhub_01.controller.recommend.response.RecommendResp;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
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

    @GetMapping("/similar/{searchId}")
    public ResponseEntity<ApiResult<List<RecommendResp>>> recommend(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable String searchId, @RequestParam(defaultValue = "10") int k) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(recommendationService.getSimilarPapers(searchId, k)));
    }

}
