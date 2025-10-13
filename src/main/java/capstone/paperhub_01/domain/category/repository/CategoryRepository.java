package capstone.paperhub_01.domain.category.repository;

import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
import capstone.paperhub_01.domain.category.Category;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    <T> Optional<T> findByCode(String code);


    /** withCounts=false: 집계 없이 코드/이름만 페이징 */
    @Query("""
        select new capstone.paperhub_01.controller.category.response.CategorySummaryResp(
            c.code, c.name, 0L
        )
        from Category c
        where c.parent is null
        order by c.code
    """)
    Page<CategorySummaryResp> findRootSummaries(Pageable pageable);

    /** withCounts=true: 논문 수/자식 수 집계 포함 */
    @Query("""
        select new capstone.paperhub_01.controller.category.response.CategorySummaryResp(
            c.code,
            c.name,
            (select count(ch) from Category ch where ch.parent = c)
        )
        from Category c
        where c.parent is null
        order by c.code
    """)
    Page<CategorySummaryResp> findRootSummariesWithCounts(Pageable pageable);
}
