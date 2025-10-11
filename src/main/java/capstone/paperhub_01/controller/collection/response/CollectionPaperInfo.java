package capstone.paperhub_01.controller.collection.response;

import capstone.paperhub_01.domain.collection.ReadingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class CollectionPaperInfo {
    private Long id;                 // collectionPaperId
    private Long paperId;
    private ReadingStatus status;
    private OffsetDateTime lastOpenedAt;
    private OffsetDateTime addedAt;
    private OffsetDateTime updatedAt;

    //paperInfo 정보
    private String title;
    private String arxivId;
    private String abstractText;
    private String primaryCategory;
    private String pdfUrl;

    // 필요 시 화면에서 쓰면 채우고, 아니면 null 유지
    private String authorsJson;      // 예: ["A B","C D"]
    private String categoriesJson;   // 예: ["cs.CL","cs.AI"]
    private LocalDate publishedDate; // arXiv 등 공개일
}
