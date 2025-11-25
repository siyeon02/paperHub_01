package capstone.paperhub_01.domain.memo.repository;

import capstone.paperhub_01.domain.memo.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    List<Memo> findByPaperSha256AndPage(String sha256, Integer page);
    List<Memo> findByAnchor_Id(Long anchorId);

    List<Memo> findByAnchorIdIn(List<Long> anchorIds);

}
