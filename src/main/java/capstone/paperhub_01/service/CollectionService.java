package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.collection.request.CollectionPaperCreateReq;
import capstone.paperhub_01.controller.collection.response.*;
import capstone.paperhub_01.domain.collection.CollectionPaper;
import capstone.paperhub_01.domain.collection.ReadingStatus;
import capstone.paperhub_01.domain.collection.repository.CollectionPaperRepository;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final PaperRepository paperRepository;
    private final CollectionPaperRepository collectionPaperRepository;
    private final MemberRepository memberRepository;
    private final PaperInfoRepository paperInfoRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CollectionPaperCreateResp createCollectionPapers(String status, CollectionPaperCreateReq req,
            Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Paper paper = paperRepository.findById(req.getPaperId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        ReadingStatus rs = parseStatus(status);

        CollectionPaper cp = new CollectionPaper();
        cp.setMember(member);
        cp.setPaper(paper);
        cp.setStatus(rs);
        cp.setLastOpenedAt(OffsetDateTime.now());
        cp.setAddedAt(OffsetDateTime.now());
        cp.setUpdatedAt(OffsetDateTime.now());

        collectionPaperRepository.save(cp);

        CollectionPaperCreateResp resp = new CollectionPaperCreateResp();
        resp.setCollectionPaperId(cp.getId());
        resp.setPaperId(cp.getPaper().getId());
        resp.setStatus(cp.getStatus());
        resp.setLastOpenedAt(cp.getLastOpenedAt());
        resp.setAddedAt(cp.getAddedAt());
        resp.setUpdatedAt(cp.getUpdatedAt());

        return resp;

    }

    private ReadingStatus parseStatus(String s) {
        return switch (s.toLowerCase()) {
            case "to-read", "to_read", "toread" -> ReadingStatus.TO_READ;
            case "in-progress", "in_progress", "inprogress" -> ReadingStatus.IN_PROGRESS;
            case "done" -> ReadingStatus.DONE;
            default -> throw new BusinessException(ErrorCode.INVALID_STATUS);
        };
    }

    @Transactional
    public StatusChangeResp changeCollectionStatus(String status, Long id, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ReadingStatus target = parseStatus(status);

        CollectionPaper cp = collectionPaperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        if (cp.getStatus() != target) {
            cp.setStatus(target);
            cp.setUpdatedAt(OffsetDateTime.now());
        }

        StatusChangeResp resp = new StatusChangeResp();
        resp.setCollectionPaperId(cp.getId());
        resp.setPaperId(cp.getPaper().getId());
        resp.setStatus(cp.getStatus());
        resp.setLastOpenedAt(cp.getLastOpenedAt());
        resp.setAddedAt(cp.getAddedAt());
        resp.setUpdatedAt(cp.getUpdatedAt());
        return resp;

    }

    @Transactional(readOnly = true)
    public CollectionPaperListResp.PageResp<CollectionPaperListResp> retrieveCollectionPapers(
            Long memberId, ReadingStatus rs, Pageable pageable) {

        Page<CollectionPaper> page = collectionPaperRepository.searchByMemberAndStatus(memberId, rs, pageable);

        List<CollectionPaperListResp> lists = page.getContent().stream()
                .map(this::toResp)
                .toList();

        CollectionPaperListResp.PageResp<CollectionPaperListResp> resp = new CollectionPaperListResp.PageResp<>();
        resp.setContent(lists);
        resp.setPage(pageable.getPageNumber());
        resp.setSize(pageable.getPageSize());
        resp.setTotalElements(page.getTotalElements());
        resp.setTotalPages(page.getTotalPages());
        resp.setLast(page.isLast());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<CollectionPaper> retrieveAllCollectionPapers(Long memberId, ReadingStatus rs) {
        return collectionPaperRepository.findAllByMemberAndStatus(memberId, rs);
    }

    private CollectionPaperListResp toResp(CollectionPaper cp) {
        CollectionPaperListResp r = new CollectionPaperListResp();
        r.setId(cp.getId());
        r.setPaperId(cp.getPaper().getId());
        r.setStatus(cp.getStatus());
        r.setLastOpenedAt(cp.getLastOpenedAt());
        r.setAddedAt(cp.getAddedAt());
        r.setUpdatedAt(cp.getUpdatedAt());

        // PaperInfo
        PaperInfo info = resolvePaperInfo(cp.getPaper());
        populateListResp(r, cp.getPaper(), info);
        return r;
    }

    @Transactional(readOnly = true)
    public CollectionPaperInfo retrieveCollectionPaperInfo(Long id, Long memberId) {

        CollectionPaper cp = collectionPaperRepository.findInfoByIdAndMember(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        CollectionPaperInfo r = new CollectionPaperInfo();

        r.setId(cp.getId());
        r.setPaperId(cp.getPaper().getId());
        r.setStatus(cp.getStatus());
        r.setLastOpenedAt(cp.getLastOpenedAt());
        r.setAddedAt(cp.getAddedAt());
        r.setUpdatedAt(cp.getUpdatedAt());

        PaperInfo info = resolvePaperInfo(cp.getPaper());
        populateDetailResp(r, cp.getPaper(), info);

        return r;
    }

    @Transactional
    public DeleteCollectionPaperResp deleteCollectionPaper(Long memberId, Long id) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CollectionPaper cp = collectionPaperRepository.findInfoByIdAndMember(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        DeleteCollectionPaperResp resp = new DeleteCollectionPaperResp();
        resp.setId(cp.getId());

        collectionPaperRepository.delete(cp);

        return resp;
    }

    private PaperInfo resolvePaperInfo(Paper paper) {
        if (paper == null)
            return null;
        PaperInfo info = paper.getPaperInfo();
        if (info != null)
            return info;
        return paperInfoRepository.findByPaper_Id(paper.getId()).orElse(null);
    }

    private void populateListResp(CollectionPaperListResp target, Paper paper, PaperInfo info) {
        if (target == null) {
            return;
        }
        if (info != null) {
            target.setTitle(info.getTitle());
            target.setAuthors(info.getAuthors());
            target.setPrimaryCategory(info.getPrimaryCategory());
            target.setCategories(encodeCategories(info.getCategories()));
            target.setArxivId(info.getArxivId());
            target.setPdfUrl(resolvePdfUrl(info, paper));
        } else {
            applyFallback(target, paper);
        }
    }

    private void populateDetailResp(CollectionPaperInfo target, Paper paper, PaperInfo info) {
        if (target == null) {
            return;
        }
        if (info != null) {
            target.setTitle(info.getTitle());
            target.setArxivId(info.getArxivId());
            target.setAbstractText(info.getAbstractText());
            target.setPrimaryCategory(info.getPrimaryCategory());
            target.setPdfUrl(resolvePdfUrl(info, paper));
            target.setAuthorsJson(info.getAuthors());
            target.setCategoriesJson(encodeCategories(info.getCategories()));
            target.setPublishedDate(info.getPublishedDate());
        } else {
            applyFallback(target, paper);
        }
    }

    private void applyFallback(CollectionPaperListResp target, Paper paper) {
        if (target == null || paper == null) {
            return;
        }
        java.util.Map<String, Object> meta = paper.getInfoJson();
        if (meta != null) {
            Object title = meta.getOrDefault("Title", meta.get("title"));
            if (title instanceof String titleStr && !titleStr.isBlank()) {
                target.setTitle(titleStr);
            }
            Object author = meta.getOrDefault("Author", meta.get("author"));
            if (author != null) {
                target.setAuthors(author.toString());
            }
        }
        target.setPdfUrl(resolvePdfUrl(null, paper));
    }

    private void applyFallback(CollectionPaperInfo target, Paper paper) {
        if (target == null || paper == null) {
            return;
        }
        java.util.Map<String, Object> meta = paper.getInfoJson();
        if (meta != null) {
            Object title = meta.getOrDefault("Title", meta.get("title"));
            if (title instanceof String titleStr && !titleStr.isBlank()) {
                target.setTitle(titleStr);
            }
        }
        target.setPdfUrl(resolvePdfUrl(null, paper));
    }

    private String encodeCategories(java.util.List<String> categories) {
        if (categories == null)
            return null;
        try {
            return objectMapper.writeValueAsString(categories);
        } catch (JsonProcessingException e) {
            return categories.toString();
        }
    }

    private String resolvePdfUrl(PaperInfo info, Paper paper) {
        if (info != null) {
            String url = info.getPdfUrl();
            if (url != null && !url.isBlank()) {
                return url;
            }
            String arxivId = info.getArxivId();
            if (arxivId != null && !arxivId.isBlank()) {
                String cleaned = arxivId.trim().replaceFirst("^arXiv:\s*", "");
                if (!cleaned.isEmpty()) {
                    String candidate = cleaned.endsWith(".pdf") ? cleaned : cleaned + ".pdf";
                    return "https://arxiv.org/pdf/" + candidate.replaceAll("\\s", "");
                }
            }
        }
        if (paper != null && paper.getId() != null) {
            return "/api/papers/" + paper.getId() + "/file";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public CollectionStatusCountResp countCollections(Long memberId) {
        List<CollectionPaperRepository.StatusCountProjection> counts = collectionPaperRepository
                .countByMemberGrouped(memberId);

        // 기본값(0) 포함시켜서 응답
        Map<ReadingStatus, Long> map = new EnumMap<>(ReadingStatus.class);
        for (ReadingStatus rs : ReadingStatus.values()) {
            map.put(rs, 0L);
        }
        counts.forEach(c -> map.put(c.getStatus(), c.getCount()));

        return new CollectionStatusCountResp(map);
    }
}
