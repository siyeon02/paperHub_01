package capstone.paperhub_01.controller.paperinfo.response;

import capstone.paperhub_01.domain.paper.PaperInfo;
import lombok.Data;

@Data
public class PaperInfoSummaryResp {
    private Long id;
    private String arxivId;
    private String title;
    private String abstractText;
    private String authors;
    private String primaryCategory;
    private String publishedDate;
    private String pdfUrl;

    public static PaperInfoSummaryResp from(PaperInfo info) {
        PaperInfoSummaryResp resp = new PaperInfoSummaryResp();
        resp.id = info.getId();
        resp.arxivId = info.getArxivId();
        resp.title = info.getTitle();
        resp.abstractText = info.getAbstractText();
        resp.authors = info.getAuthors();
        resp.primaryCategory = info.getPrimaryCategory();
        resp.publishedDate = info.getPublishedDate() == null ? null : info.getPublishedDate().toString();
        resp.pdfUrl = info.getPdfUrl();
        return resp;
    }
}

