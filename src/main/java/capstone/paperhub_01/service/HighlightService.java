package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Annotation.request.HighlightCreateReq;
import capstone.paperhub_01.domain.highlight.Highlight;
import capstone.paperhub_01.domain.highlight.repository.HighlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class HighlightService {
    private final AnchorService anchorService;
    private final HighlightRepository highlightRepository;

    @Transactional
    public Highlight create(HighlightCreateReq req, String createdBy) {
        // 1) Anchor upsert
        var anchor = anchorService.upsert(
                req.getPaperSha256(), req.getPage(),
                req.getRects(), req.getExact(), req.getPrefix(), req.getSuffix(),
                req.getSignature(), createdBy
        );

        // 2) Highlight 생성 (앵커 참조)
        var h = new Highlight();
        h.setAnchor(anchor);
        h.setPaperSha256(req.getPaperSha256());
        h.setPage(req.getPage());
        h.setColor(req.getColor());
        h.setStatus("active");
        h.setCreatedBy(createdBy);
        var now = OffsetDateTime.now();
        h.setCreatedAt(now); h.setUpdatedAt(now);
        return highlightRepository.save(h);
    }
}
