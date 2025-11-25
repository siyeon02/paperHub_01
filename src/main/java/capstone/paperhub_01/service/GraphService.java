package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.graph.response.EdgeResp;
import capstone.paperhub_01.controller.graph.response.GraphResp;
import capstone.paperhub_01.controller.graph.response.NodeResp;
import capstone.paperhub_01.controller.recommend.response.RecommendResp;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GraphService {

    private final PaperRepository paperRepository;
    private final RecommendationService recommendationService;
    private final MemberRepository memberRepository;
    private final PaperInfoRepository paperInfoRepository;

    public GraphResp buildPaperGraph(String arxivId, List<RecommendResp> recs) {

        List<NodeResp> nodes = new ArrayList<>();
        List<EdgeResp> edges = new ArrayList<>();

        // 1. 중심 논문 정보를 DB에서 조회
        PaperInfo center = paperInfoRepository.findByArxivId(arxivId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        // 2. 중심 노드 추가
        NodeResp centerNode = new NodeResp(
                center.getId(),
                center.getArxivId(),
                center.getTitle(),
                center.getAbstractText(),
                center.getAuthors(),
                center.getPrimaryCategory(),
                center.getPublishedDate().toString()
        );
        nodes.add(centerNode);

        // 3. 추천 노드 + similar 엣지 추가
        int rank = 1;
        for (RecommendResp r : recs) {

            // 혹시 center가 똑같이 들어왔으면 스킵
            if (arxivId.equals(r.getArxivId())) {
                continue;
            }

            // (선택) 이미 nodes에 있는 arXivId인지 체크해서 중복 방지
            if (containsNode(nodes, r.getArxivId())) {
                continue;
            }

            // 추천 노드: 우리 DB에 없는 논문일 수도 있으니, 안전하게 null 허용
            PaperInfo maybePaper = paperInfoRepository.findByArxivId(r.getArxivId())
                    .orElse(null);

            Long nodeId = maybePaper != null ? maybePaper.getId() : null;
            String primaryCategory = maybePaper != null ? maybePaper.getPrimaryCategory() : null;
            String abstractText = maybePaper != null ? maybePaper.getAbstractText() : null;

            NodeResp node = new NodeResp(
                    nodeId,
                    r.getArxivId(),
                    r.getTitle(),
                    abstractText,
                    String.join(", ", r.getAuthors()),
                    primaryCategory,
                    r.getPublished()
            );
            nodes.add(node);

            // similar edge 생성 (center -> recommended)
            EdgeResp edge = new EdgeResp(
                    null,                       // edge id 굳이 없으면 null
                    "similar",
                    arxivId,              // source
                    r.getArxivId(),             // target
                    r.getScore(),               // weight
                    rank++                      // rank
            );
            edges.add(edge);
        }

        return new GraphResp(nodes, edges);
    }

    private boolean containsNode(List<NodeResp> nodes, String arxivId) {
        return nodes.stream().anyMatch(n -> arxivId.equals(n.getArXivId()));
    }

    private String joinAuthors(List<String> authors) {
        if (authors == null || authors.isEmpty()) return "";
        return String.join(", ", authors);
    }

}
