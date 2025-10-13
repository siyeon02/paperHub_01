package capstone.paperhub_01.service;


import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
import capstone.paperhub_01.controller.category.response.SubCategorySummaryResp;
import capstone.paperhub_01.domain.category.Category;
import capstone.paperhub_01.domain.category.PaperCategory;
import capstone.paperhub_01.domain.category.PaperCategoryId;
import capstone.paperhub_01.domain.category.repository.CategoryRepository;
import capstone.paperhub_01.domain.category.repository.PaperCategoryRepository;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;

import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public void attachCategoriesFromInfo(Long paperId, List<String> codes) {
        Paper paper = paperRepository.getReferenceById(paperId);

        for (String code : codes) {
            Category category = getOrCreateHierarchy(code);
            PaperCategoryId id = new PaperCategoryId(paper.getId(), category.getCode());
            if (!paperCategoryRepository.existsById(id.getPaperId())) {
                paperCategoryRepository.save(PaperCategory.link(paper, category));
            }
        }
    }


    private Category getOrCreateHierarchy(String code) {
        // 1) 루트부터 순차 생성: "cs" -> "cs.AI" -> "cs.AI.Deep" 처럼 다단계도 안전
        String[] parts = code.split("\\.");
        StringBuilder acc = new StringBuilder();
        Category parent = null;

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) acc.append(".");
            acc.append(parts[i]);
            String cur = acc.toString();

            Category current = categoryRepository.findById(cur).orElse(null);
            if (current == null) {
                current = Category.of(cur, cur, parent);
                categoryRepository.save(current);
            } else {
                // 필요시 이름 동기화 로직(외부에서 사람이 친화적 이름을 주입할 때)
                // current.rename(humanNameFromDictOrNull(cur));
            }
            parent = current;
        }
        return parent; // 마지막 노드
    }

    @Transactional
    public void syncPaperCategories(Paper paper, List<String> codes) {
        if (codes == null || codes.isEmpty()) return;

        for (String raw : codes) {
            String code = normalize(raw);
            if (code.isEmpty()) continue;
            Category category = getOrCreateHierarchy(code);
            PaperCategoryId id = new PaperCategoryId(paper.getId(), category.getCode());
            if (!paperCategoryRepository.existsById(id.getPaperId())) {
                paperCategoryRepository.save(PaperCategory.link(paper, category));
            }
        }
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

        return withCounts
                ? categoryRepository.findRootSummariesWithCounts(pageable)
                : categoryRepository.findRootSummaries(pageable);
    }

    @Transactional(readOnly = true)
    public Page<SubCategorySummaryResp> getChildrenSummaries(String code, Pageable pageable) {
        // 부모가 없으면 404
        if (!categoryRepository.existsById(code)) {
            throw new IllegalArgumentException("Category not found: " + code);
        }

        return categoryRepository.findChildrenWithDirectCounts(code, pageable);



    }
}
