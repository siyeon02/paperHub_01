package capstone.paperhub_01.domain.paper.repository;

import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.PaperInfo;
import org.apache.el.stream.Stream;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    Optional<Paper> findBySha256(String sha256);

    Optional<Paper> findByFingerprint(String fingerprint);

    @EntityGraph(attributePaths = "paperInfo")
    @Query("select p from Paper p where p.id = :id")
    Optional<Paper> findWithInfoById(Long id);

    boolean existsByFingerprint(String fingerprint);
}
