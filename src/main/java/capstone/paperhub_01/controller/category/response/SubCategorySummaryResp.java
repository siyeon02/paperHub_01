package capstone.paperhub_01.controller.category.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubCategorySummaryResp {
    private String code;
    private String name;
    private long paperCount;
    private long childrenCount;


    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> SubCategorySummaryResp.PageResponse<T> from(Page<T> p) {
            return new SubCategorySummaryResp.PageResponse<>(
                    p.getContent(), p.getNumber(), p.getSize(),
                    p.getTotalElements(), p.getTotalPages()
            );
        }
    }
}
