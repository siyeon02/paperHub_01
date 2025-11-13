package capstone.paperhub_01.domain.category.repository;

import capstone.paperhub_01.domain.category.PaperCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperCategoryRepository extends JpaRepository<PaperCategory, Long> {
    boolean existsByPaper_IdAndCategory_Code(Long paperId, String categoryCode);
}
