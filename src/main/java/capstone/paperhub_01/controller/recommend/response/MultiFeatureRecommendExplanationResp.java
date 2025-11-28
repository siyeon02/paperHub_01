package capstone.paperhub_01.controller.recommend.response;

public record MultiFeatureRecommendExplanationResp(
        String searchId,
        String recId,
        double totalScore,
        double cosineSimilarity,
        double venueMatch,
        double categoryMatch,
        double recency,
        double userPreference,
        String explanation
) {}
