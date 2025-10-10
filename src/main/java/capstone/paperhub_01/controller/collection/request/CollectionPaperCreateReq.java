package capstone.paperhub_01.controller.collection.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionPaperCreateReq {
    @NotNull
    private Long paperId;
}
