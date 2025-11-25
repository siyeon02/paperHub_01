package capstone.paperhub_01.domain.category;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PaperCategoryId implements Serializable {
    @Column(name = "paper_id")
    private Long paperId;

    @Column(name = "category_code", length = 64)
    private String categoryCode;
}
