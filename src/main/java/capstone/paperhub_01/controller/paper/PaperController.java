package capstone.paperhub_01.controller.paper;

import capstone.paperhub_01.controller.paper.request.FingerprintUpdateReq;
import capstone.paperhub_01.controller.paper.request.PaperLookupReq;
import capstone.paperhub_01.controller.paper.response.FingerprintUpdateResp;
import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.controller.paper.response.PaperLookupResp;
import capstone.paperhub_01.controller.paper.response.PaperViewResp;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.service.PaperService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService paperService;

    @PostMapping(value = "/register-from-url", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<PaperCreateResp>> upload(
            @NotNull @RequestPart("file") MultipartFile file,
            @RequestParam(value = "sourceId", required = false) String sourceId,
            @RequestParam(value = "uploaderId", required = false) String uploaderId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(paperService.uploadAndExtract(file, sourceId, uploaderId)));
    }

    @GetMapping("/{paperId}")
    public ResponseEntity<ApiResult<PaperViewResp>> getById(@PathVariable Long paperId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(paperService.getById(paperId)));
    }

    @PatchMapping(value = "/{paperId}/fingerprint", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResult<FingerprintUpdateResp>> setFingerprint(
            @PathVariable Long paperId,
            @RequestBody FingerprintUpdateReq req
            ){

        var updated = paperService.setFingerprint(paperId, req.getFingerprint());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(FingerprintUpdateResp.from(updated)));
    }

    @PostMapping(value = "/lookup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResult<PaperLookupResp>> lookup(@RequestBody PaperLookupReq req){
        var p = paperService.findBySha256OrFingerPrint(req.getSha256(), req.getFingerprint());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(PaperLookupResp.from(p)));
    }

}
