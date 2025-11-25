package capstone.paperhub_01.controller.paper.request;

import lombok.Data;

@Data
public class PaperLookupReq {
    private String sha256;
    private String fingerprint;
}
