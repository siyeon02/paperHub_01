package capstone.paperhub_01.controller.Annotation.response;

import capstone.paperhub_01.domain.memo.Memo;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MemoCreateResp {
    private Long id;
    private Long anchorId;
    private String paperSha256;
    private Integer page;
    private String body;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long parentId;

    public static MemoCreateResp from(Memo m){
        var r = new MemoCreateResp();
        r.id = m.getId();
        r.anchorId = m.getAnchor().getId();
        r.paperSha256 = m.getPaperSha256();
        r.page = m.getPage();
        r.body = m.getBody();
        r.createdBy = m.getCreatedBy();
        r.createdAt = m.getCreatedAt();
        r.updatedAt = m.getUpdatedAt();
        r.parentId = (m.getParent()==null? null : m.getParent().getId());
        return r;
    }
}
