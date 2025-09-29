package capstone.paperhub_01.controller.Annotation;

import capstone.paperhub_01.controller.Annotation.request.HighlightCreateReq;
import capstone.paperhub_01.service.AnchorService;
import capstone.paperhub_01.service.HighlightService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnotationController {
    private final HighlightService highlightService;
    private final AnchorService anchorService;

    @PostMapping("/highlights")
    public ResponseEntity<ApiResult<Map<String,Object>>> createHighlight(@Valid @RequestBody HighlightCreateReq req) {
        var hl = highlightService.create(req, "user-123"); // TODO: SecurityContext에서 user
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(Map.of(
                "highlightId", hl.getId(), "anchorId", hl.getAnchor().getId()
        )));
    }
}
