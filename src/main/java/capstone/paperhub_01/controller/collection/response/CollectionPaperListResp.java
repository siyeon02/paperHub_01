package capstone.paperhub_01.controller.collection.response;

import capstone.paperhub_01.domain.collection.ReadingStatus;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class CollectionPaperListResp {
    Long id;
    private Long paperId;// 간단 표기 (필요시 List<String>)
    private ReadingStatus status;
    private OffsetDateTime lastOpenedAt;
    private OffsetDateTime addedAt;
    private OffsetDateTime updatedAt;

    @Data
    public static class PageResp<T>{
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
