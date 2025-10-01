package capstone.paperhub_01.controller.Annotation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemoCreateReq {
    private Long anchorId;

    @NotBlank
    private String body;

    private Long parentId;
}
