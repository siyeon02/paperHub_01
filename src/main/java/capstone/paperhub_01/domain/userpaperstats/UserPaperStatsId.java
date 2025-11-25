package capstone.paperhub_01.domain.userpaperstats;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserPaperStatsId implements Serializable {
    private Long userId;
    private Long paperId;

    public UserPaperStatsId(Long userId, Long paperId) {
        this.userId = userId;
        this.paperId = paperId;
    }
}
