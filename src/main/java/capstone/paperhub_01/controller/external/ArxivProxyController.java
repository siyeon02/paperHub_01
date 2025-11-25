package capstone.paperhub_01.controller.external;

import capstone.paperhub_01.controller.external.response.ArxivPaperResp;
import capstone.paperhub_01.service.ArxivService;
import capstone.paperhub_01.util.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/arxiv")
@RequiredArgsConstructor
public class ArxivProxyController {

    private final ArxivService arxivService;

    @GetMapping("/search")
    public ResponseEntity<ApiResult<List<ArxivPaperResp>>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "start", defaultValue = "0") int start,
            @RequestParam(value = "maxResults", defaultValue = "10") int maxResults
    ) {
        try {
            return ResponseEntity.ok(ApiResult.success(arxivService.search(query, start, maxResults)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            log.error("Failed to fetch arXiv search results", e);
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResult.error("arXiv 데이터를 가져오지 못했습니다.", HttpStatus.BAD_GATEWAY.value()));
        }
    }
}
