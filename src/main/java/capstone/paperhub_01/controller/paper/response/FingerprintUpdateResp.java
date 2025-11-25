package capstone.paperhub_01.controller.paper.response;


import capstone.paperhub_01.domain.paper.Paper;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FingerprintUpdateResp {
    private Long paperId;
    private String sha256;
    private String fingerprint;
    private Integer numPages;
    private List<Map<String, Object>> pageSizes;

    public static FingerprintUpdateResp from(Paper paper){
        FingerprintUpdateResp resp = new FingerprintUpdateResp();
        resp.paperId = paper.getId();
        resp.sha256 = paper.getSha256();
        resp.fingerprint = paper.getFingerprint();
        resp.numPages = paper.getNumPages();
        resp.pageSizes = paper.getPageSizes();
        return resp;
    }
}


