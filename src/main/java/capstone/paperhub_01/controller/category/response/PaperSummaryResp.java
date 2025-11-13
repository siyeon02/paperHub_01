package capstone.paperhub_01.controller.category.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaperSummaryResp {

    private Long id;
    private String title;
    private String arxivId;
    private String abstractText;
    private String authors;
    private LocalDate publishedDate;
    private String primaryCategory;
}
