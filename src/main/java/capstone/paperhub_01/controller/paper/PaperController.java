package capstone.paperhub_01.controller.paper;

import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.PaperService;
import capstone.paperhub_01.util.ApiResult;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;

@Validated
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService paperService;

    @PostMapping(value = "/register-from-url", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<PaperCreateResp>> upload(
            @NotNull @RequestPart("file")MultipartFile file,
            @RequestParam(value = "sourceId", required = false) String sourceId,
            @RequestParam(value = "uploaderId", required = false) String uploaderId
            ){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(paperService.uploadAndExtract(file, sourceId, uploaderId)));
    }
}
