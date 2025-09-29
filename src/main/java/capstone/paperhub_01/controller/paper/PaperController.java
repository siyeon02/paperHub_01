package capstone.paperhub_01.controller.paper;

import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.controller.paper.response.PaperViewResp;
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

}
