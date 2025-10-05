package capstone.paperhub_01.controller.Annotation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageAnnotationsReq {
    @NotBlank
    private String sha256;

    @NotNull
    @Min(1)
    private Integer page;
}
