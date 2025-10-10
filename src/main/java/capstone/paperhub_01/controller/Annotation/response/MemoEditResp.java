package capstone.paperhub_01.controller.Annotation.response;

import capstone.paperhub_01.domain.memo.Memo;
import lombok.Data;

import java.time.OffsetDateTime;
@Data
public class MemoEditResp {
    private Long id;
    private Long anchorId;
    private String paperSha256;
    private Integer page;
    private String body;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long parentId;

    public MemoEditResp(Memo memo) {
        this.id = memo.getId();
        this.anchorId = memo.getAnchor().getId();
        this.paperSha256 = memo.getPaperSha256();
        this.page = memo.getPage();
        this.body = memo.getBody();
        this.createdBy = memo.getCreatedBy();
        this.createdAt = memo.getCreatedAt();
        this.updatedAt = memo.getUpdatedAt();
        this.parentId = (memo.getParent()==null? null : memo.getParent().getId());
    }
}
