package capstone.paperhub_01.controller.graph.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphResp {
    List<NodeResp> nodes;
    List<EdgeResp> edges;
}
