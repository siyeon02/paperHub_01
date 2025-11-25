package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Annotation.response.PageAnnotationsResp;
import capstone.paperhub_01.domain.anchor.Anchor;
import capstone.paperhub_01.domain.anchor.repository.AnchorRepository;
import capstone.paperhub_01.domain.highlight.repository.HighlightRepository;
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
        // 1) 앵커 조회
        var anchors = anchorRepository.findByPaperSha256AndPage(sha256, page);

        var resp = new PageAnnotationsResp();
        resp.setItems(new ArrayList<>());

        if (anchors.isEmpty()) {
            resp.setCount(0);
            resp.setTotalHighlights(0);
            resp.setTotalNotes(0);
            resp.setTotalAnnotations(0);
            return resp;
        }

        // 2) 배치 조회 (하이라이트/메모)
        var anchorIds = anchors.stream().map(Anchor::getId).toList();

        var highsByAnchor = highlightRepository.findByAnchorIdIn(anchorIds).stream()
                .collect(Collectors.groupingBy(h -> h.getAnchor().getId()));

        var memosByAnchor = memoRepository.findByAnchorIdIn(anchorIds).stream()
                .collect(Collectors.groupingBy(m -> m.getAnchor().getId()));

        // 3) 앵커별 묶음 구성
        for (var a : anchors) {
            var item = new PageAnnotationsResp.Item();

            // ---- Anchor DTO ----
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

            // ---- Highlights DTO ----
            var hDtos = new ArrayList<PageAnnotationsResp.Highlight>();
            for (var h : highsByAnchor.getOrDefault(a.getId(), List.of())) {
                var hd = new PageAnnotationsResp.Highlight();
                hd.setId(h.getId());
                hd.setColor(h.getColor());
                hd.setCreatedBy(h.getCreatedBy());
                hDtos.add(hd);
            }
            item.setHighlights(hDtos);

            // ---- Notes DTO ----
            var mDtos = new ArrayList<PageAnnotationsResp.Memo>();
            for (var m : memosByAnchor.getOrDefault(a.getId(), List.of())) {
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

        // 4) 합계/카운트 계산 (여기가 핵심)
        resp.setCount(resp.getItems().size()); // ✅ 앵커 묶음 수
        int th = resp.getItems().stream().mapToInt(i -> i.getHighlights() == null ? 0 : i.getHighlights().size()).sum();
        int tn = resp.getItems().stream().mapToInt(i -> i.getNotes() == null ? 0 : i.getNotes().size()).sum();
        resp.setTotalHighlights(th);
        resp.setTotalNotes(tn);
        resp.setTotalAnnotations(th + tn);

        return resp;
    }

    private static double toD(Object o) {
        return (o == null) ? 0.0 : ((Number) o).doubleValue();
    }

}
