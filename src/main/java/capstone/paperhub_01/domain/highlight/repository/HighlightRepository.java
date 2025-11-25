package capstone.paperhub_01.domain.highlight.repository;

import capstone.paperhub_01.domain.highlight.Highlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HighlightRepository extends JpaRepository<Highlight, Long> {
    List<Highlight> findByPaperSha256AndPage(String sha256, Integer page);
    List<Highlight> findByAnchorIdIn(Collection<Long> anchorIds);

    long countByAnchor_Id(Long anchorId);
}
