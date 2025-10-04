package capstone.paperhub_01.controller.Annotation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemoEditReq {
    @NotBlank
    private String body;
}
