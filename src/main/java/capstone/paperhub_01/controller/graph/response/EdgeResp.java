package capstone.paperhub_01.controller.graph.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeResp {
    private Long id;
    private String type;      // "similar" or "citation"
    private String source;    // paper:...
    private String target;
    private Double weight;    // cosine score or citation weight
    private Integer rank;
    private List<String> keywords;
}
