package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Annotation.response.PageAnnotationsResp;
import capstone.paperhub_01.domain.anchor.Anchor;
import capstone.paperhub_01.domain.anchor.repository.AnchorRepository;
import capstone.paperhub_01.domain.highlight.Highlight;
import capstone.paperhub_01.domain.highlight.repository.HighlightRepository;
import capstone.paperhub_01.domain.memo.repository.Memo;
import capstone.paperhub_01.domain.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PageAnnotationQueryService {
    private final AnchorRepository anchorRepository;
    private final HighlightRepository highlightRepository;
    private final MemoRepository memoRepository;

    @Transactional(readOnly = true)
    public PageAnnotationsResp getPageBundle(String sha256, int page) {
        var anchors = anchorRepository.findByPaperSha256AndPage(sha256, page);
        var resp = new PageAnnotationsResp();
        resp.setItems(new ArrayList<>());

        if (anchors.isEmpty()) {
            resp.setCount(0);
            return resp;
        }

        var anchorIds = anchors.stream().map(Anchor::getId).toList();

        var highsByAnchor = highlightRepository.findByAnchorIdIn(anchorIds).stream()
                .collect(Collectors.groupingBy(h -> h.getAnchor().getId()));

        var memosByAnchor = memoRepository.findByAnchorIdIn(anchorIds).stream()
                .collect(Collectors.groupingBy(m -> m.getAnchor().getId()));

        for (var a : anchors) {
            // ---- Anchor ----
            var item = new PageAnnotationsResp.Item();

            var adto = new PageAnnotationsResp.Anchor();
            adto.setId(a.getId());
            adto.setSignature(a.getSignature());
            adto.setExact(a.getExact());
            adto.setPrefix(a.getPrefix());
            adto.setSuffix(a.getSuffix());

            var rectDtos = new ArrayList<PageAnnotationsResp.Rect>();
            if (a.getRects() != null) {
                for (var r : a.getRects()) {
                    var rr = new PageAnnotationsResp.Rect();
                    rr.setX(toD(r.get("x")));
                    rr.setY(toD(r.get("y")));
                    rr.setW(toD(r.get("w")));
                    rr.setH(toD(r.get("h")));
                    rectDtos.add(rr);
                }
            }
            adto.setRects(rectDtos);
            item.setAnchor(adto);

            // ---- Highlights ----
            var hDtos = new ArrayList<PageAnnotationsResp.Highlight>();
            for (Highlight h : highsByAnchor.getOrDefault(a.getId(), List.of())) {
                var hd = new PageAnnotationsResp.Highlight();
                hd.setId(h.getId());
                hd.setColor(h.getColor());
                hd.setCreatedBy(h.getCreatedBy());
                hDtos.add(hd);
            }
            item.setHighlights(hDtos);

            // ---- Notes ----
            var mDtos = new ArrayList<PageAnnotationsResp.Memo>();
            for (Memo m : memosByAnchor.getOrDefault(a.getId(), List.of())) {
                var md = new PageAnnotationsResp.Memo();
                md.setId(m.getId());
                md.setBody(m.getBody());
                md.setCreatedBy(m.getCreatedBy());
                md.setCreatedAt(m.getCreatedAt());
                md.setParentId(m.getParent() != null ? m.getParent().getId() : null);
                mDtos.add(md);
            }
            item.setNotes(mDtos);

            resp.getItems().add(item);
        }

        resp.setCount(resp.getItems().size());
        return resp;
    }

    private static double toD(Object o) {
        return (o == null) ? 0.0 : ((Number) o).doubleValue();
    }

}
