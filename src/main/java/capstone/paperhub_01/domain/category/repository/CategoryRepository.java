package capstone.paperhub_01.domain.category.repository;

import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
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


    /* 서브 목록: 카운트 없이 */
    @Query("""
  select new capstone.paperhub_01.controller.category.response.SubCategorySummaryResp(
     ch.code, ch.name
  )
  from Category ch
  where ch.parent.code = :code
  order by ch.code
""")
    Page<SubCategorySummaryResp> findChildrenWithDirectCounts(@Param("code") String code, Pageable pageable);




}
