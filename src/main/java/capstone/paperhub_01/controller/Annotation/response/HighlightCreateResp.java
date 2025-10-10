package capstone.paperhub_01.controller.Annotation.response;

import capstone.paperhub_01.domain.highlight.Highlight;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class HighlightCreateResp {
    private Long highlightId;
    private Long anchorId;
    private String paperSha256;
    private Integer page;
    private String color;
    private OffsetDateTime createdAt;

    public static HighlightCreateResp from(Highlight h) {
        HighlightCreateResp r = new HighlightCreateResp();
        r.highlightId = h.getId();
        r.anchorId    = h.getAnchor().getId();
        r.paperSha256 = h.getPaperSha256();
        r.page        = h.getPage();
        r.color       = h.getColor();
        r.createdAt   = h.getCreatedAt();
        return r;
    }
}
