package capstone.paperhub_01.domain.paper.repository;

import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.PaperInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaperInfoRepository extends JpaRepository<PaperInfo, Long> {
  Optional<PaperInfo> findByArxivId(String arxivId);

  Optional<PaperInfo> findByArxivIdIgnoreCase(String arxivId);

  Optional<PaperInfo> findByPaper_Id(Long paperId);

  @Query(value = """
      SELECT *
      FROM paper_infos pi
      WHERE (:q IS NULL OR :q = '' OR
             lower(pi.title) LIKE lower(concat('%', :q, '%')) OR
             lower(pi.arxiv_id) LIKE lower(concat('%', :q, '%')))
        AND (
             :category IS NULL OR :category = '' OR
             pi.primary_category = :category OR
             (:rollup = true AND pi.primary_category LIKE concat(:category, '.%'))
             )
      ORDER BY pi.published_date DESC NULLS LAST, pi.id DESC
      """, countQuery = """
      SELECT count(*)
      FROM paper_infos pi
      WHERE (:q IS NULL OR :q = '' OR
             lower(pi.title) LIKE lower(concat('%', :q, '%')) OR
             lower(pi.arxiv_id) LIKE lower(concat('%', :q, '%')))
        AND (
             :category IS NULL OR :category = '' OR
             pi.primary_category = :category OR
             (:rollup = true AND pi.primary_category LIKE concat(:category, '.%'))
          )
      """, nativeQuery = true)
  Page<PaperInfo> searchByKeywordAndCategory(@Param("q") String q,
      @Param("category") String category,
      @Param("rollup") boolean rollup,
      Pageable pageable);

  Long paper(Paper paper);

  interface CategoryAgg {
    String getCode();

    String getName();

    Long getPaperCount();

    Long getChildrenCount();
  }

  @Query(value = """
      SELECT
        split_part(pi.primary_category, '.', 1) AS code,
        split_part(pi.primary_category, '.', 1) AS name,
        COUNT(*) AS paperCount,
        0::bigint AS childrenCount
      FROM paper_infos pi
      WHERE pi.primary_category IS NOT NULL AND pi.primary_category <> ''
      GROUP BY 1, 2
      ORDER BY 1
      """, countQuery = """
      SELECT COUNT(*) FROM (
        SELECT split_part(pi.primary_category, '.', 1) AS code
        FROM paper_infos pi
        WHERE pi.primary_category IS NOT NULL AND pi.primary_category <> ''
        GROUP BY 1
      ) t
      """, nativeQuery = true)
  Page<CategoryAgg> findRootCategoriesFromInfos(Pageable pageable);

  @Query(value = """
      SELECT
        concat(:code, '.', split_part(pi.primary_category, '.', 2)) AS code,
        concat(:code, '.', split_part(pi.primary_category, '.', 2)) AS name,
        COUNT(*) AS paperCount,
        0::bigint AS childrenCount
      FROM paper_infos pi
      WHERE split_part(pi.primary_category, '.', 1) = :code
            AND coalesce(split_part(pi.primary_category, '.', 2), '') <> ''
      GROUP BY 1, 2
      ORDER BY 1
      """, countQuery = """
      SELECT COUNT(*) FROM (
        SELECT concat(:code, '.', split_part(pi.primary_category, '.', 2)) AS code
        FROM paper_infos pi
        WHERE split_part(pi.primary_category, '.', 1) = :code
              AND coalesce(split_part(pi.primary_category, '.', 2), '') <> ''
        GROUP BY 1
      ) t
      """, nativeQuery = true)
  Page<CategoryAgg> findChildrenCategoriesFromInfos(@Param("code") String code, Pageable pageable);

  @Query("""
          SELECT p FROM PaperInfo p
          WHERE p.venue IS NOT NULL AND p.venue <> ''
            AND (LOWER(p.venue) = LOWER(:venue)
                 OR LOWER(p.venue) LIKE CONCAT('%', LOWER(:venue), '%'))
          ORDER BY p.publishedDate DESC
      """)
  List<PaperInfo> findByVenueLike(@Param("venue") String venue, Pageable pageable);

    @Query("SELECT p FROM PaperInfo p WHERE p.id IN :ids")
    List<PaperInfo> findByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT p FROM PaperInfo p WHERE p.arxivId IN :arxivIds")
    List<PaperInfo> findByArxivIdIn(@Param("arxivIds") Collection<String> arxivIds);

}
