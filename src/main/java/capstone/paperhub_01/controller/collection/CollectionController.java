package capstone.paperhub_01.controller.collection;

import capstone.paperhub_01.controller.collection.request.CollectionPaperCreateReq;
import capstone.paperhub_01.controller.collection.response.*;
import capstone.paperhub_01.domain.collection.ReadingStatus;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.CollectionService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<ApiResult<CollectionPaperCreateResp>> createCollectionPapers(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("status") String status,
            @Valid @RequestBody CollectionPaperCreateReq req) {
        Member member = userDetails.getUser();
        ReadingStatus rs = parseStatus(status);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(collectionService.createCollectionPapers(status, req, member.getId())));
    }

    @PatchMapping("/collection-items/{status}/{id}")
    public ResponseEntity<ApiResult<StatusChangeResp>> changeCollectionStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable("status") String status,
            @PathVariable("id") Long id) {
        Member member = userDetails.getUser();
        ReadingStatus rs = parseStatus(status);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResult.success(collectionService.changeCollectionStatus(status, id, member.getId())));
    }

    @GetMapping("/collections/{status}")
    public ResponseEntity<ApiResult<CollectionPaperListResp.PageResp<CollectionPaperListResp>>> retrieveCollectionPapers(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("status") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {
        Member member = userDetails.getUser();
        ReadingStatus rs = parseStatus(status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        var resp = collectionService.retrieveCollectionPapers(member.getId(), rs, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    @GetMapping("/collection-items/{id}")
    public ResponseEntity<ApiResult<CollectionPaperInfo>> retrieveCollectionPaperInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("id") Long id) {
        Member member = userDetails.getUser();
        var resp = collectionService.retrieveCollectionPaperInfo(id, member.getId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    @DeleteMapping("/collection-items/{id}")
    public ResponseEntity<ApiResult<DeleteCollectionPaperResp>> deleteCollectionPaper(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable("id") Long id) {
        Member member = userDetails.getUser();
        var resp = collectionService.deleteCollectionPaper(member.getId(), id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    @GetMapping("/collections/count")
    public ResponseEntity<ApiResult<CollectionStatusCountResp>> countCollections(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Member member = userDetails.getUser();
        var resp = collectionService.countCollections(member.getId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    private ReadingStatus parseStatus(String s) {
        String norm = s.trim().toLowerCase().replace('_', '-');
        return switch (norm) {
            case "to-read", "toread" -> ReadingStatus.TO_READ;
            case "in-progress", "inprogress" -> ReadingStatus.IN_PROGRESS;
            case "done" -> ReadingStatus.DONE;
            default -> throw new BusinessException(ErrorCode.INVALID_STATUS);
        };
    }

    private Sort.Order parseSort(String s) {
        // "updatedAt,desc" → Sort.Order.desc("updatedAt")
        String[] arr = s.split(",");
        String prop = arr[0];
        boolean desc = arr.length > 1 && "desc".equalsIgnoreCase(arr[1]);
        return desc ? Sort.Order.desc(prop) : Sort.Order.asc(prop);
    }
}
