package capstone.paperhub_01.controller.Collection.response;

import capstone.paperhub_01.domain.collection.ReadingStatus;
import jakarta.persistence.Column;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CollectionPaperCreateResp {
    private Long collectionPaperId;
    private Long paperId;
    private ReadingStatus status;
    private OffsetDateTime lastOpenedAt;
    private OffsetDateTime addedAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
