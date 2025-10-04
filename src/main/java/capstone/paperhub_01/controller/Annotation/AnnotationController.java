package capstone.paperhub_01.controller.Annotation;

import capstone.paperhub_01.controller.Annotation.request.HighlightCreateReq;
import capstone.paperhub_01.controller.Annotation.request.MemoCreateReq;
import capstone.paperhub_01.controller.Annotation.response.HighlightCreateResp;
import capstone.paperhub_01.controller.Annotation.response.HighlightDeleteResp;
import capstone.paperhub_01.controller.Annotation.response.MemoCreateResp;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.AnchorService;
import capstone.paperhub_01.service.HighlightService;
import capstone.paperhub_01.service.MemoService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnotationController {
    private final MemoService memoService;
    private final HighlightService highlightService;
    private final AnchorService anchorService;

    @PostMapping("/highlights")
    public ResponseEntity<ApiResult<HighlightCreateResp>> createHighlight(@AuthenticationPrincipal UserDetailsImpl userDetails,  @Valid @RequestBody HighlightCreateReq req) {
        Member member = userDetails.getUser();
        var hl = highlightService.create(req, String.valueOf(member.getId())); // TODO: SecurityContext에서 user
        var dto = HighlightCreateResp.from(hl);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(dto));
    }

    @DeleteMapping("/highlights/{id}")
    public ResponseEntity<ApiResult<HighlightDeleteResp>> deleteHighlight(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long id) {
        Member member = userDetails.getUser();
        var resp = highlightService.delete(id, member.getId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    @PostMapping("/memos")
    public ResponseEntity<ApiResult<MemoCreateResp>> createMemo(@AuthenticationPrincipal UserDetailsImpl userDetails, @Valid @RequestBody MemoCreateReq req){
        Member member = userDetails.getUser();
        var m = memoService.create(req, member.getId());
        var dto = MemoCreateResp.from(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(dto));
    }

}
