package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.recommend.response.MultiFeatureRecommendExplanationResp;
import capstone.paperhub_01.controller.recommend.response.PaperRecommendResp;
import capstone.paperhub_01.controller.recommend.response.PaperScoreDto;
import capstone.paperhub_01.controller.recommend.response.RecommendExplanationResp;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.pinecone.PineconeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MultiFeatureRecommenderService {

    private final PineconeClient pineconeClient;
    private final PaperInfoRepository paperInfoRepository;
    private final LLMService llmService;

    // 가중치
    private final Map<String, Double> weights = Map.of(
            "cosine_similarity", 0.4,
            "venue_match", 0.3,
            "category_match", 0.2,
            "recency", 0.1
    );

    // --------- 1. 메타데이터 조회 ---------
    private PaperInfo getPaperMetadata(String arxivId) {
        return paperInfoRepository.findByArxivId(arxivId)
                .orElse(null);
    }

    // --------- 2. 코사인 유사도 (Pinecone) ---------
    private List<Map.Entry<String, Double>> computeCosineSimilarity(String arxivId, int topK) {
        try {
            // 너가 원래 쓰던 pineconeClient.queryById 그대로 활용 (JSON 반환 가정)
            String json = pineconeClient.queryById(arxivId, topK + 1);

            JsonNode root = new ObjectMapper().readTree(json);
            List<Map.Entry<String, Double>> result = new ArrayList<>();

            for (JsonNode m : root.path("matches")) {
                String id = m.path("id").asText("");
                if (id.isEmpty() || arxivId.equals(id)) continue;

                double score = m.path("score").asDouble(0.0);

                // metadata에 arxiv_id가 따로 있으면 그걸 쓰고, 없으면 id 사용
                String arxiv = m.path("metadata").path("arxiv_id").asText(id);

                result.add(Map.entry(arxiv, score));
            }

            return result;
        } catch (Exception e) {
            // 로그만 찍고 빈 리스트
            return List.of();
        }
    }

    // --------- 3. 점수 계산 함수들 ---------
    private double computeVenueScore(String targetVenue, String candidateVenue) {
        if (targetVenue == null || candidateVenue == null
                || targetVenue.isBlank() || candidateVenue.isBlank()) {
            return 0.0;
        }

        String t = targetVenue.toLowerCase();
        String c = candidateVenue.toLowerCase();

        if (t.equals(c)) {
            return 1.0;
        }
        if (t.contains(c) || c.contains(t)) {
            return 0.9;
        }

        Set<String> tWords = new HashSet<>(List.of(t.split("\\s+")));
        Set<String> cWords = new HashSet<>(List.of(c.split("\\s+")));
        tWords.retainAll(cWords);

        if (!tWords.isEmpty()) {
            double overlapRatio = (double) tWords.size()
                    / Math.min(t.split("\\s+").length, c.split("\\s+").length);
            return overlapRatio * 0.7;
        }
        return 0.0;
    }

    private double computeCategoryScore(String targetCat, String candidateCat) {
        if (targetCat == null || candidateCat == null
                || targetCat.isBlank() || candidateCat.isBlank()) {
            return 0.0;
        }

        if (targetCat.equals(candidateCat)) {
            return 1.0;
        }

        String tMain = targetCat.split("\\.")[0];
        String cMain = candidateCat.split("\\.")[0];

        if (tMain.equals(cMain)) {
            return 0.5;
        }
        return 0.0;
    }

    private double computeRecencyScore(LocalDate publishedDate) {
        if (publishedDate == null) {
            return 0.5;
        }

        long days = ChronoUnit.DAYS.between(publishedDate, LocalDate.now());
        double years = days / 365.25;

        if (years <= 2.0) return 1.0;
        if (years >= 5.0) return 0.0;
        return 1.0 - (years - 2.0) / 3.0;
    }

    // --------- 4. 논문 기반 추천 ---------
    public List<PaperScoreDto> recommendPapers(String sourceArxivId,
                                               int topK,
                                               @Nullable List<String> excludeArxivIds) {

        PaperInfo source = getPaperMetadata(sourceArxivId);
        if (source == null) {
            // 예외 던질지, 빈 리스트 반환할지 선택
            return List.of();
        }

        List<Map.Entry<String, Double>> candidates =
                computeCosineSimilarity(sourceArxivId, 100);

        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> exclude = new HashSet<>(Optional.ofNullable(excludeArxivIds).orElse(List.of()));
        exclude.add(sourceArxivId);

        List<PaperScoreDto> scored = new ArrayList<>();

        for (Map.Entry<String, Double> cand : candidates) {
            String candArxivId = cand.getKey();
            double cosine = cand.getValue();

            if (exclude.contains(candArxivId)) continue;

            PaperInfo candPaper = getPaperMetadata(candArxivId);
            if (candPaper == null) continue;

            double venueScore = computeVenueScore(
                    source.getVenue(), candPaper.getVenue());
            double categoryScore = computeCategoryScore(
                    source.getPrimaryCategory(), candPaper.getPrimaryCategory());
            double recencyScore = computeRecencyScore(candPaper.getPublishedDate());

            // 가중 합산
            double total = 0.0;
            total += cosine * weights.get("cosine_similarity");
            total += venueScore * weights.get("venue_match");
            total += categoryScore * weights.get("category_match");
            total += recencyScore * weights.get("recency");

            PaperScoreDto.ScoreDetail detail = new PaperScoreDto.ScoreDetail(
                    cosine, venueScore, categoryScore, recencyScore
            );

            scored.add(new PaperScoreDto(
                    candArxivId,
                    candPaper.getTitle(),
                    candPaper.getVenue(),
                    candPaper.getVenueType(),
                    candPaper.getPrimaryCategory(),
                    candPaper.getPublishedDate(),
                    total,
                    detail
            ));
        }

        // total_score 기준 정렬
        return scored.stream()
                .sorted(Comparator.comparingDouble(PaperScoreDto::totalScore).reversed())
                .limit(topK)
                .toList();
    }

    // --------- 5. venue 기반 추천 ---------
    public List<PaperScoreDto> recommendByVenue(String venue, int topK) {
        Pageable pageable = PageRequest.of(0, topK);
        List<PaperInfo> papers = paperInfoRepository.findByVenueLike(venue, pageable);

        // score 없이 그냥 리스트만 주고 싶으면 totalScore=1.0, detail null로 줘도 됨
        return papers.stream()
                .map(p -> new PaperScoreDto(
                        p.getArxivId(),
                        p.getTitle(),
                        p.getVenue(),
                        p.getVenueType(),
                        p.getPrimaryCategory(),
                        p.getPublishedDate(),
                        1.0,
                        new PaperScoreDto.ScoreDetail(0, 0, 0, 0)
                ))
                .toList();
    }

    public MultiFeatureRecommendExplanationResp getExplanationMultiFeature(Long userId, String searchId, String recId) {

        // 1. 기준 논문 메타데이터
        PaperInfo base = getPaperMetadata(searchId);
        if (base == null) {
            throw new IllegalArgumentException("기준 논문을 찾을 수 없습니다: " + searchId);
        }

        // categories 문자열에서 키워드 리스트 뽑아오기 (콤마 기준 가정)
        List<String> baseKeywords = parseKeywords(base.getPrimaryCategory());

        // 2. 멀티 피처 추천 목록 가져오기
        List<PaperScoreDto> candidates = recommendPapers(searchId, 50, null);
        PaperScoreDto target = candidates.stream()
                .filter(c -> c.arxivId().equals(recId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("추천 결과에서 recId를 찾을 수 없습니다: " + recId));

        // 3. 추천 논문 메타데이터
        PaperInfo rec = getPaperMetadata(recId);
        if (rec == null) {
            throw new IllegalArgumentException("추천 논문을 찾을 수 없습니다: " + recId);
        }
        List<String> recKeywords = parseKeywords(rec.getPrimaryCategory());

        // 4. 멀티 피처 점수들
        PaperScoreDto.ScoreDetail s = target.scores();

        // 5. LLM 호출 (멀티 피처 버전)
        String explanation = llmService.generateExplanationMultiFeature(
                base.getTitle(),
                baseKeywords,
                rec.getTitle(),
                recKeywords,
                target.totalScore(),      // 종합 점수
                s.cosineSimilarity(),
                s.venueMatch(),
                s.categoryMatch(),
                s.recency()
        );

        // 6. 최종 응답 DTO 조립
        return new MultiFeatureRecommendExplanationResp(
                searchId,                // 기준 논문 ID
                recId,                   // 추천 논문 ID
                target.totalScore(),     // 최종 스코어
                s.cosineSimilarity(),
                s.venueMatch(),
                s.categoryMatch(),
                s.recency(),
                explanation              // LLM이 생성한 텍스트
        );
    }

    private List<String> parseKeywords(String categories) {
        if (categories == null || categories.isBlank()) {
            return List.of();
        }
        return Arrays.stream(categories.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

}

