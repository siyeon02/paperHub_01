package capstone.paperhub_01.domain.paper.repository;

import capstone.paperhub_01.domain.paper.Paper;
import org.apache.el.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    Optional<Paper> findBySha256(String sha256);

    Optional<Paper> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);
}
