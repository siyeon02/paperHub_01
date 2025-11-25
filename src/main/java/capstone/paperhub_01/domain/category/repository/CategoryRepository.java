package capstone.paperhub_01.domain.category.repository;

import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
import capstone.paperhub_01.controller.category.response.PaperSummaryResp;
import capstone.paperhub_01.controller.category.response.SubCategorySummaryResp;
import capstone.paperhub_01.domain.category.Category;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    <T> Optional<T> findByCode(String code);


    /** withCounts=false: 집계 없이 코드/이름만 페이징 */
    @Query("""
        select new capstone.paperhub_01.controller.category.response.CategorySummaryResp(
            c.code, c.name, 0L, 0L
        )
        from Category c
        where c.parent is null
        order by c.code
    """)
    Page<CategorySummaryResp> findRootSummaries(Pageable pageable);

    /** withCounts=true: 논문 수/자식 수 집계 포함 */
    @Query(value = """
              SELECT
                c.code                  AS code,
                c.name                  AS name,
                COALESCE(crc.paper_count, 0) AS paperCount,
                (SELECT COUNT(*) FROM category ch WHERE ch.parent_code = c.code) AS childrenCount
              FROM category c
              LEFT JOIN category_rollup_counts crc ON crc.category_code = c.code
              WHERE c.parent_code IS NULL
              ORDER BY c.code
            """,
            countQuery = """
  SELECT COUNT(*) FROM category c WHERE c.parent_code IS NULL
""",
            nativeQuery = true)
    Page<CategorySummaryResp> findRootSummariesWithCounts(Pageable pageable);


    /** 직계만: pc.category.code = :code */
    @Query("""
        select distinct new capstone.paperhub_01.controller.category.response.PaperSummaryResp(
            p.id, coalesce(pi.title, ''), pi.arxivId, pi.abstractText, pi.authors, pi.publishedDate, pi.primaryCategory
        )
        from PaperCategory pc
        join pc.paper p
        left join PaperInfo pi on pi.paper = p
        where pc.category.code = :code
            or pc.category.code like concat(:code, '.%')
    """)
    Page<PaperSummaryResp> findDirectPapersByCategory(@Param("code") String code, Pageable pageable);

    /** 롤업(자기 + 하위): code = :code or code like :code.% */
    @Query("""
                select distinct new capstone.paperhub_01.controller.category.response.PaperSummaryResp(
                    p.id, coalesce(pi.title, ''), pi.arxivId, pi.abstractText, pi.authors, pi.publishedDate, pi.primaryCategory
                )
                from PaperCategory pc
                join pc.paper p
                left join PaperInfo pi on pi.paper = p
                where pc.category.code = :code
            """)
    Page<PaperSummaryResp> findRollupPapersByCategory(@Param("code") String code, Pageable pageable);


    // 루트 카테고리 요약 (롤업 X, 직접 연결만)
    @Query("""
        SELECT new capstone.paperhub_01.controller.category.response.CategorySummaryResp(
            c.code,
            c.name,
            (SELECT COUNT(DISTINCT pc.paper.id) FROM capstone.paperhub_01.domain.category.PaperCategory pc
             WHERE pc.category.code = c.code),
            (SELECT COUNT(ch) FROM capstone.paperhub_01.domain.category.Category ch
             WHERE ch.parent.code = c.code)
        )
        FROM capstone.paperhub_01.domain.category.Category c
        WHERE c.parent IS NULL
        ORDER BY c.code
        """)
    Page<CategorySummaryResp> findRootSummariesNoRollup(Pageable pageable);

    // 자식 카테고리 요약 (롤업 X, 직접 연결만)
    @Query("""
        SELECT new capstone.paperhub_01.controller.category.response.SubCategorySummaryResp(
            c.code,
            c.name,
            (SELECT COUNT(DISTINCT pc.paper.id) FROM capstone.paperhub_01.domain.category.PaperCategory pc
             WHERE pc.category.code = c.code),
            (SELECT COUNT(ch) FROM capstone.paperhub_01.domain.category.Category ch
             WHERE ch.parent.code = c.code)
        )
        FROM capstone.paperhub_01.domain.category.Category c
        WHERE c.parent.code = :parentCode
        ORDER BY c.code
        """)
    Page<SubCategorySummaryResp> findChildrenNoRollup(String parentCode, Pageable pageable);

    // (참고) 기존 findRootSummariesWithCounts / findRootSummaries / findChildrenWithDirectCounts
    // 를 더 이상 쓰지 않으면 제거해도 무방.
}
