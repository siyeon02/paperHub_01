package capstone.paperhub_01.service;


import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
import capstone.paperhub_01.controller.category.response.PaperSummaryResp;
import capstone.paperhub_01.controller.category.response.SubCategorySummaryResp;
import capstone.paperhub_01.domain.category.Category;
import capstone.paperhub_01.domain.category.PaperCategory;
import capstone.paperhub_01.domain.category.PaperCategoryId;
import capstone.paperhub_01.domain.category.repository.CategoryRepository;
import capstone.paperhub_01.domain.category.repository.PaperCategoryRepository;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;

import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final PaperRepository paperRepository;
    private final PaperCategoryRepository paperCategoryRepository;
    private final PaperInfoRepository paperInfoRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;


    @Transactional
    public void syncFromPaperInfo(Long paperId, @Nullable String arxivId) {
        Paper paper = paperRepository.getReferenceById(paperId);

        var infoOpt = paperInfoRepository.findByPaper_Id(paperId);
        if (infoOpt.isEmpty() && arxivId != null && !arxivId.isBlank()) {
            infoOpt = findByArxivVariants(arxivId);
        }
        if (infoOpt.isEmpty()) {
            log.warn("[CatSync] PaperInfo not found for paperId={}, arxivId={}", paperId, arxivId);
            return;
        }

        PaperInfo info = infoOpt.get();

        // ✅ FK 백필 후 반드시 저장
        if (info.getPaper() == null) {
            info.setPaper(paper);
            paperInfoRepository.save(info);      // ← 추가
        }

        List<String> codes = extractCodesFromInfo(info);
        if (codes == null || codes.isEmpty()) {
            log.warn("[CatSync] categories empty for paperId={}, arxivId={}", paperId, arxivId);
            return;
        }
        syncPaperCategories(paper, codes);
    }


    @Transactional
    public void syncPaperCategories(Paper paper, List<String> codes) {
        if (codes == null || codes.isEmpty()) return;

        for (String raw : codes) {
            String code = normalize(raw);
            if (code.isEmpty()) continue;

            Category category = getOrCreateHierarchy(code);

            // ✅ 멱등 체크는 (paper_id, category_code)로!
            if (!paperCategoryRepository.existsByPaper_IdAndCategory_Code(paper.getId(), category.getCode())) {
                paperCategoryRepository.save(PaperCategory.link(paper, category));
            }
        }
    }

    private Category getOrCreateHierarchy(String code) {
        String[] parts = code.split("\\.");
        StringBuilder acc = new StringBuilder();
        Category parent = null;

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) acc.append(".");
            acc.append(parts[i]);
            String cur = acc.toString();

            Category current = categoryRepository.findById(cur).orElse(null);
            if (current == null) {
                // name은 우선 code로. 필요시 별도 사전으로 rename 가능
                current = Category.of(cur, cur, parent);
                categoryRepository.save(current);
            }
            parent = current;
        }
        return parent;
    }

    private List<String> extractCodesFromInfo(PaperInfo info) {
        List<String> arr = info.getCategories();
        if (arr == null) return List.of();
        return arr.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(c -> !c.isEmpty())
                .distinct()
                .toList();
    }

    private java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> findByArxivVariants(String arxivId) {
        String trimmed = arxivId == null ? null : arxivId.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return java.util.Optional.empty();
        }

        java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> direct = paperInfoRepository.findByArxivId(trimmed);
        if (direct.isPresent()) return direct;

        java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> directIgnore = paperInfoRepository.findByArxivIdIgnoreCase(trimmed);
        if (directIgnore.isPresent()) return directIgnore;

        String withoutPrefix = trimmed.replaceFirst("^arXiv\\s*", "");
        if (!withoutPrefix.equals(trimmed)) {
            java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> prefixed = paperInfoRepository.findByArxivId(withoutPrefix);
            if (prefixed.isPresent()) return prefixed;
            java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> prefixedIgnore = paperInfoRepository.findByArxivIdIgnoreCase(withoutPrefix);
            if (prefixedIgnore.isPresent()) return prefixedIgnore;
        }

        String baseId = stripVersion(withoutPrefixOrSelf(trimmed));
        if (!baseId.equals(trimmed)) {
            java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> base = paperInfoRepository.findByArxivId(baseId);
            if (base.isPresent()) return base;
            java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> baseIgnore = paperInfoRepository.findByArxivIdIgnoreCase(baseId);
            if (baseIgnore.isPresent()) return baseIgnore;
        }

        if (!withoutPrefix.equals(trimmed)) {
            String baseFromNoPrefix = stripVersion(withoutPrefix);
            if (!baseFromNoPrefix.equals(withoutPrefix)) {
                java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> base2 = paperInfoRepository.findByArxivId(baseFromNoPrefix);
                if (base2.isPresent()) return base2;
                java.util.Optional<capstone.paperhub_01.domain.paper.PaperInfo> baseIgnore2 = paperInfoRepository.findByArxivIdIgnoreCase(baseFromNoPrefix);
                if (baseIgnore2.isPresent()) return baseIgnore2;
            }
        }

        return java.util.Optional.empty();
    }

    private String withoutPrefixOrSelf(String arxivId) {
        if (arxivId == null) return "";
        return arxivId.replaceFirst("^arXiv\\s*", "");
    }

    private String stripVersion(String arxivId) {
        if (arxivId == null) return "";
        return arxivId.trim().replaceFirst("v\\d+$", "");
    }


    private String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "");
    }



    /**
     *
     * @param memberId   향후 개인화 집계용 파라미터(현재는 미사용)
     * @param withCounts true이면 논문 수 집계 포함
     * @param pageable   페이징 정보
     * @return 루트 카테고리 요약의 페이지 응답
     */
    @Transactional(readOnly = true)
    public Page<CategorySummaryResp> getRootSummaries(Long memberId, boolean withCounts, Pageable pageable) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return categoryRepository.findRootSummaries(pageable);
    }

    @Transactional(readOnly = true)
    public Page<SubCategorySummaryResp> getChildrenSummaries(String code, Pageable pageable) {
        // 부모가 없으면 404
        if (!categoryRepository.existsById(code)) {
            throw new IllegalArgumentException("Category not found: " + code);
        }

        return categoryRepository.findChildrenNoRollup(code, pageable);



    }

    public Page<PaperSummaryResp> getCategoryPapers(String code, boolean rollup, Pageable pageable) {
        if (!categoryRepository.existsById(code)) {
            throw new IllegalArgumentException("Category not found: " + code);
        }
        return rollup
                ? categoryRepository.findRollupPapersByCategory(code, pageable)
                : categoryRepository.findDirectPapersByCategory(code, pageable);
    }
}
