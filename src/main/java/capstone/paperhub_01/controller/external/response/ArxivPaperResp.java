package capstone.paperhub_01.controller.external.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArxivPaperResp {
    private String id;
    private String title;
    private String summary;
    private List<String> authors;
    private String pdfLink;
    private String published;
    private List<String> categories;

    public List<String> getAuthors() {
        return authors == null ? Collections.emptyList() : authors;
    }

    public List<String> getCategories() {
        return categories == null ? Collections.emptyList() : categories;
    }
}
