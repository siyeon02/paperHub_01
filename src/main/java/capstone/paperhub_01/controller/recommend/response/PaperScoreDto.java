package capstone.paperhub_01.controller.recommend.response;

import java.time.LocalDate;
import java.util.List;

public record PaperScoreDto(
        String arxivId,
        String title,
        String venue,
        String venueType,
        String primaryCategory,
        LocalDate publishedDate,
        double totalScore,
        ScoreDetail scores,
        List<String> keywords
) {
    public record ScoreDetail(
            double cosineSimilarity,
            double venueMatch,
            double categoryMatch,
            double recency,
            double userPreference
    ){}
}

