package capstone.paperhub_01.controller.Annotation.response;

import capstone.paperhub_01.domain.highlight.Highlight;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
public class HighlightDeleteResp {
    private Long highlightId;
    private Long anchorId;

    public HighlightDeleteResp(Long highlightId, Long anchorId) {
        this.highlightId = highlightId;
        this.anchorId = anchorId;
    }

}
