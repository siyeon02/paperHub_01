package capstone.paperhub_01.domain.userpaperstats.repository;

import capstone.paperhub_01.domain.userpaperstats.UserPaperStats;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStatsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserPaperStatsRepository extends JpaRepository<UserPaperStats, UserPaperStatsId> {
    List<UserPaperStats> findByIdUserId(Long userId);

    List<UserPaperStats> findAllByIdUserId(Long userId);

}
