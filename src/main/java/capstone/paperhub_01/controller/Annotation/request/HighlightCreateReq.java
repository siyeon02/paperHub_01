package capstone.paperhub_01.controller.Annotation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HighlightCreateReq {
    @NotBlank
    private String paperSha256;
    @Min(1)  private Integer page;
    @NotNull
    private List<Map<String,Object>> rects;
    private String exact, prefix, suffix, signature;
    @Pattern(regexp = "^#?[0-9A-Fa-f]{3,8}$") private String color;
}
