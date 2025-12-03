package capstone.paperhub_01.controller.paper;

import capstone.paperhub_01.controller.paper.request.FingerprintUpdateReq;
import capstone.paperhub_01.controller.paper.request.PaperLookupReq;
import capstone.paperhub_01.controller.paper.request.ReadingSessionReq;
import capstone.paperhub_01.controller.paper.response.FingerprintUpdateResp;
import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.controller.paper.response.PaperLookupResp;
import capstone.paperhub_01.controller.paper.response.PaperViewResp;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.PaperService;
import capstone.paperhub_01.service.UserPaperStatsService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Validated
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService paperService;
    private final UserPaperStatsService userPaperStatsService;

    // @PostMapping(value = "/register-from-url", consumes =
    // MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<ApiResult<PaperCreateResp>> upload(
    // @NotNull @RequestPart("file") MultipartFile file,
    // @AuthenticationPrincipal UserDetailsImpl userDetails) {
    //
    // Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "paperhub");
    // try {
    // Files.createDirectories(tmpDir);
    // } catch (IOException e) {
    // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed
    // to create tmp dir", e);
    // }
    //
    // Path localTmp;
    // try {
    // localTmp = Files.createTempFile(tmpDir, "upload-", ".pdf");
    // file.transferTo(localTmp.toFile());
    // } catch (IOException e) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to receive
    // upload", e);
    // }
    //
    // PaperCreateResp resp;
    // try {
    // Long memberId = userDetails.getUser().getId();
    // resp = paperService.uploadAndExtractFromPath(localTmp, memberId);
    // } finally {
    //
    // try { Files.deleteIfExists(localTmp); } catch (Exception ignore) {}
    // }
    //
    // return
    // ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(resp));
    // }

    @PostMapping(value = "/register-from-url", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<PaperCreateResp>> upload(
            @NotNull @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "paperhub");
        try {
            Files.createDirectories(tmpDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tmp dir", e);
        }

        Path localTmp;
        try {
            localTmp = Files.createTempFile(tmpDir, "upload-", ".pdf");
            file.transferTo(localTmp.toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to receive upload", e);
        }

        PaperCreateResp resp;
        try {
            Long memberId = userDetails.getUser().getId();

            // 원본 파일명 전달 (없으면 빈 문자열)
            String originalFilename = file.getOriginalFilename();
            resp = paperService.uploadAndExtractFromPath(localTmp, originalFilename, memberId);

        } finally {
            try {
                Files.deleteIfExists(localTmp);
            } catch (Exception ignore) {
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(resp));
    }

    @GetMapping("/{paperId}")
    public ResponseEntity<ApiResult<PaperViewResp>> getById(@AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long paperId) {
        Long userId = userDetails.getUser().getId();
        userPaperStatsService.recordOpen(userId, paperId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(paperService.getById(paperId)));
    }

    @GetMapping("/{paperId}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable Long paperId) {
        byte[] bytes = paperService.loadPaperFile(paperId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PatchMapping(value = "/{paperId}/fingerprint", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResult<FingerprintUpdateResp>> setFingerprint(
            @PathVariable Long paperId,
            @RequestBody FingerprintUpdateReq req) {

        var updated = paperService.setFingerprint(paperId, req.getFingerprint());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(FingerprintUpdateResp.from(updated)));
    }

    @PostMapping(value = "/lookup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResult<PaperLookupResp>> lookup(@RequestBody PaperLookupReq req) {
        var p = paperService.findBySha256OrFingerPrint(req.getSha256(), req.getFingerprint());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(PaperLookupResp.from(p)));
    }

    @PostMapping("/{paperId}/sessions")
    public ResponseEntity<ApiResult<Void>> endReadingSession(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long paperId,
            @RequestBody ReadingSessionReq req) {
        userPaperStatsService.recordSession(
                userDetails.getUser().getId(),
                paperId,
                req.sessionSeconds(), // 이번 세션 읽은 시간 (초)
                req.lastPage(), // 마지막으로 본 페이지
                req.maxPage(), // 가장 깊게 본 페이지
                req.pageCount() // 전체 페이지 수 (선택)
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResult.success(null));
    }

}
