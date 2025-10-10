package capstone.paperhub_01.controller.paper.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FingerprintUpdateReq {
    @NotBlank
    private String fingerprint;
}
