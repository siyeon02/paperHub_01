package capstone.paperhub_01.domain.paper.repository;

import capstone.paperhub_01.domain.paper.PaperInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.nio.channels.FileChannel;
import java.util.Optional;

public interface PaperInfoRepository extends JpaRepository<PaperInfo, Long> {
    Optional<PaperInfo> findByArxivId(String arxivId);

    Optional<PaperInfo> findByPaper_Id(Long paperId);
}
