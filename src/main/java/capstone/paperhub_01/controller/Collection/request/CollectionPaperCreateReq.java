package capstone.paperhub_01.controller.Collection.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionPaperCreateReq {
    @NotNull
    private Long paperId;
}
