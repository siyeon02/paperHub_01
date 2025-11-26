package capstone.paperhub_01.controller.recommend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaperRecommendResp {
    private String arxivId;
    private String title;
    private String venue;
    private String venueType;
    private String primaryCategory;
    private LocalDate publishedDate;
    private double totalScore;
}

