package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Annotation.request.HighlightCreateReq;
import capstone.paperhub_01.controller.Annotation.response.HighlightDeleteResp;
import capstone.paperhub_01.domain.anchor.repository.AnchorRepository;
import capstone.paperhub_01.domain.highlight.Highlight;
import capstone.paperhub_01.domain.highlight.repository.HighlightRepository;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class HighlightService {
    private final AnchorService anchorService;
    private final HighlightRepository highlightRepository;
    private final AnchorRepository anchorRepository;
    private final UserPaperStatsService userPaperStatsService;
    private final MemberRepository memberRepository;
    private final PaperRepository paperRepository;

    @Transactional
    public Highlight create(HighlightCreateReq req, String createdBy) {
        // 1) Anchor upsert
        var anchor = anchorService.upsert(
                req.getPaperSha256(),
                req.getPage(),
                req.getRects(),
                req.getExact(),
                req.getPrefix(),
                req.getSuffix(),
                req.getSignature(), createdBy
        );

        // 2) Highlight 생성 (앵커 참조)
        var h = new Highlight();
        h.setAnchor(anchor);
        h.setPaperSha256(req.getPaperSha256());
        h.setPage(req.getPage());
        h.setColor(req.getColor());
        h.setCreatedBy(createdBy);
        var now = OffsetDateTime.now();
        h.setCreatedAt(now); h.setUpdatedAt(now);

        Highlight saved = highlightRepository.save(h);

        Long memberId = memberRepository.findById(Long.valueOf(createdBy))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                .getId();

        Long paperId = paperRepository.findBySha256(req.getPaperSha256())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND))
                .getId();

        //하이라이트 1개 추가)
        userPaperStatsService.addHighlight(memberId, paperId);

        return saved;


        //return highlightRepository.save(h);
    }

    @Transactional
    public HighlightDeleteResp delete(Long highlightId, Long memberId) {

        var h = highlightRepository.findById(highlightId)
                .orElseThrow(() -> new IllegalArgumentException("highlight not found: " + highlightId));

        Long anchorId = h.getAnchor().getId();

        highlightRepository.delete(h);

        boolean noHighlights = highlightRepository.countByAnchor_Id(anchorId) > 0;
        if (noHighlights) {
            anchorRepository.deleteById(anchorId);
        }

        return new HighlightDeleteResp(highlightId, anchorId);

    }
}
