package capstone.paperhub_01.domain.anchor.repository;

import capstone.paperhub_01.domain.anchor.Anchor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnchorRepository extends JpaRepository<Anchor, Long> {
    Optional<Anchor> findByPaperSha256AndPageAndSignature(String sha256, Integer page, String signature);

    List<Anchor> findByPaperSha256AndPage(String sha256, Integer page);
}
