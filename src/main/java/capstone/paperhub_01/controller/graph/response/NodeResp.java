package capstone.paperhub_01.controller.graph.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeResp {
    private Long id;
    private String arXivId;
    private String title;
    private String abstractText;
    private String authors;
    private String primaryCategory;
    private String publishedDate;

    public NodeResp(Long id, String arXivId, String title, String abstractText, String authors, String publishedDate) {
        this.id = id;
        this.arXivId = arXivId;
        this.title = title;
        this.abstractText = abstractText;
        this.authors = authors;
        this.publishedDate = publishedDate;
    }
}
