package capstone.paperhub_01.domain.collection.repository;

import capstone.paperhub_01.domain.collection.CollectionPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CollectionPaperRepository extends JpaRepository<CollectionPaper, Long> {
    Optional<CollectionPaper> findByPaperId(Long paperId);

}
