package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.graph.response.EdgeResp;
import capstone.paperhub_01.controller.graph.response.GraphResp;
import capstone.paperhub_01.controller.graph.response.NodeResp;
import capstone.paperhub_01.controller.graph.response.UserPaperStatsResp;
import capstone.paperhub_01.controller.recommend.response.PaperScoreDto;
import capstone.paperhub_01.controller.recommend.response.RecommendResp;
import capstone.paperhub_01.domain.collection.CollectionPaper;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStats;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStatsId;
import capstone.paperhub_01.domain.userpaperstats.repository.UserPaperStatsRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import capstone.paperhub_01.pinecone.PineconeClient;
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
    private final UserPaperStatsRepository userPaperStatsRepository;
    private final PineconeClient pineconeClient;

//    public GraphResp buildPaperGraph(String arxivId, List<RecommendResp> recs) {
//
//        List<NodeResp> nodes = new ArrayList<>();
//        List<EdgeResp> edges = new ArrayList<>();
//
//        // 1. 중심 논문 정보를 DB에서 조회
//        PaperInfo center = paperInfoRepository.findByArxivId(arxivId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
//
//        // 2. 중심 노드 추가
//        NodeResp centerNode = new NodeResp(
//                center.getId(),
//                center.getArxivId(),
//                center.getTitle(),
//                center.getAbstractText(),
//                center.getAuthors(),
//                center.getPrimaryCategory(),
//                center.getPublishedDate().toString());
//        nodes.add(centerNode);
//
//        // 3. 추천 노드 + similar 엣지 추가
//        int rank = 1;
//        for (RecommendResp r : recs) {
//
//            // 혹시 center가 똑같이 들어왔으면 스킵
//            if (arxivId.equals(r.getArxivId())) {
//                continue;
//            }
//
//            // (선택) 이미 nodes에 있는 arXivId인지 체크해서 중복 방지
//            if (containsNode(nodes, r.getArxivId())) {
//                continue;
//            }
//
//            // 추천 노드: 우리 DB에 없는 논문일 수도 있으니, 안전하게 null 허용
//            PaperInfo maybePaper = paperInfoRepository.findByArxivId(r.getArxivId())
//                    .orElse(null);
//
//            Long nodeId = maybePaper != null ? maybePaper.getId() : null;
//            String primaryCategory = maybePaper != null ? maybePaper.getPrimaryCategory() : null;
//            String abstractText = maybePaper != null ? maybePaper.getAbstractText() : null;
//
//            NodeResp node = new NodeResp(
//                    nodeId,
//                    r.getArxivId(),
//                    r.getTitle(),
//                    abstractText,
//                    String.join(", ", r.getAuthors()),
//                    primaryCategory,
//                    r.getPublished());
//            nodes.add(node);
//
//            // similar edge 생성 (center -> recommended)
//            EdgeResp edge = new EdgeResp(
//                    null, // edge id 굳이 없으면 null
//                    "similar",
//                    arxivId, // source
//                    r.getArxivId(), // target
//                    r.getScore(), // weight
//                    rank++ // rank
//            );
//            edges.add(edge);
//        }
//
//        return new GraphResp(nodes, edges);
//    }

    private boolean containsNode(List<NodeResp> nodes, String arxivId) {
        return nodes.stream().anyMatch(n -> arxivId.equals(n.getArXivId()));
    }

    private String joinAuthors(List<String> authors) {
        if (authors == null || authors.isEmpty())
            return "";
        return String.join(", ", authors);
    }

    public GraphResp buildPaperGraphMultiFeature(String arxivId, List<PaperScoreDto> recs) {
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
                center.getPublishedDate().toString());
        nodes.add(centerNode);

        // 3. 추천 노드 + similar 엣지 추가 (멀티 피처 버전)
        int rank = 1;
        for (PaperScoreDto r : recs) {

            // center와 동일하면 스킵
            if (arxivId.equals(r.arxivId())) {
                continue;
            }

            // 이미 추가된 노드인지 체크 (arxivId 기준)
            if (containsNode(nodes, r.arxivId())) {
                continue;
            }

            // 추천 노드: 우리 DB에 없는 논문일 수도 있으니 null 허용
            PaperInfo maybePaper = paperInfoRepository.findByArxivId(r.arxivId())
                    .orElse(null);

            Long nodeId = maybePaper != null ? maybePaper.getId() : null;
            String primaryCategory = maybePaper != null
                    ? maybePaper.getPrimaryCategory()
                    : r.primaryCategory();
            String abstractText = maybePaper != null ? maybePaper.getAbstractText() : null;
            String authors = maybePaper != null
                    ? maybePaper.getAuthors()
                    : ""; // PaperScoreDto에 authors 없으니 DB 없으면 빈 문자열

            String published = null;
            if (maybePaper != null && maybePaper.getPublishedDate() != null) {
                published = maybePaper.getPublishedDate().toString();
            } else if (r.publishedDate() != null) {
                published = r.publishedDate().toString();
            }

            NodeResp node = new NodeResp(
                    nodeId,
                    r.arxivId(),
                    r.title(),
                    abstractText,
                    authors,
                    primaryCategory,
                    published);
            nodes.add(node);

            List<String> edgeKeywords = r.keywords();
            if (edgeKeywords != null && edgeKeywords.size() > 10) {
                edgeKeywords = edgeKeywords.subList(0, 10); // ★ 너무 길면 상위 10개만
            }

            // similar edge 생성 (center -> recommended)
            EdgeResp edge = new EdgeResp(
                    null, // edge id 필요 없으면 null
                    "similar", // edge type
                    arxivId, // source
                    r.arxivId(), // target
                    r.totalScore(), // 멀티 피처 종합 점수를 weight로 사용
                    rank++, // rank,
                    edgeKeywords

            );
            edges.add(edge);
        }

        return new GraphResp(nodes, edges);
    }

    public UserPaperStatsResp getStats(Long userId, Long paperId) {
        UserPaperStatsId id = new UserPaperStatsId(userId, paperId);
        UserPaperStats stats = userPaperStatsRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATS_NOT_FOUND));

        return UserPaperStatsResp.from(stats);
    }

    public GraphResp buildLibraryGraph(List<CollectionPaper> papers) {
        List<NodeResp> nodes = new ArrayList<>();
        List<EdgeResp> edges = new ArrayList<>();

        for (CollectionPaper cp : papers) {
            PaperInfo info = cp.getPaper().getPaperInfo();
            // PaperInfo가 없으면 paperInfoRepository에서 찾기 (fetch join으로 가져왔으면 있을 것)
            if (info == null) {
                info = paperInfoRepository.findByPaper_Id(cp.getPaper().getId()).orElse(null);
            }

            if (info != null) {
                NodeResp node = new NodeResp(
                        info.getId(),
                        info.getArxivId(),
                        info.getTitle(),
                        info.getAbstractText(),
                        info.getAuthors(),
                        info.getPrimaryCategory(),
                        info.getPublishedDate() != null ? info.getPublishedDate().toString() : null);
                nodes.add(node);
            }
        }
        // 엣지는 일단 없음 (노드만 뿌려줌)
        return new GraphResp(nodes, edges);
    }
}
