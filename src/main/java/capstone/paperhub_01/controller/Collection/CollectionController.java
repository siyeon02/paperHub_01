package capstone.paperhub_01.controller.Collection;

import capstone.paperhub_01.controller.Collection.request.CollectionPaperCreateReq;
import capstone.paperhub_01.controller.Collection.response.CollectionPaperCreateResp;
import capstone.paperhub_01.domain.collection.ReadingStatus;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.CollectionService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping("/collections/{status}")
    public ResponseEntity<ApiResult<CollectionPaperCreateResp>> createCollectionPapers(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("status") String status, @Valid @RequestBody CollectionPaperCreateReq req) {
        Member member = userDetails.getUser();
        ReadingStatus rs = parseStatus(status);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(collectionService.createCollectionPapers(status, req, member.getId())));
    }
    private ReadingStatus parseStatus(String s) {
        String norm = s.trim().toLowerCase().replace('_','-');
        return switch (norm) {
            case "to-read", "toread" -> ReadingStatus.TO_READ;
            case "in-progress", "inprogress" -> ReadingStatus.IN_PROGRESS;
            case "done" -> ReadingStatus.DONE;
            default -> throw new BusinessException(ErrorCode.INVALID_STATUS);
        };
    }
}
