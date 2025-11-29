package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.recommend.response.MultiFeatureRecommendExplanationResp;
import capstone.paperhub_01.controller.recommend.response.PaperRecommendResp;
import capstone.paperhub_01.controller.recommend.response.PaperScoreDto;
import capstone.paperhub_01.controller.recommend.response.RecommendExplanationResp;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.domain.userpaperstats.UserPaperStats;
import capstone.paperhub_01.domain.userpaperstats.repository.UserPaperStatsRepository;
import capstone.paperhub_01.pinecone.PineconeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiFeatureRecommenderService {

    private final PineconeClient pineconeClient;
    private final PaperInfoRepository paperInfoRepository;
    private final LLMService llmService;
    private final UserPaperStatsRepository userPaperStatsRepository;

    // 가중치
    private final Map<String, Double> weights = Map.of(
            "cosine_similarity", 0.35,
            "venue_match", 0.25,
            "category_match", 0.2,
            "recency", 0.1,
            "user_preference",   0.1
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


    // --------- venue 기반 추천 ---------
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
                        new PaperScoreDto.ScoreDetail(0, 0, 0, 0,0)
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
        //List<PaperScoreDto> candidates = recommendPapers(searchId, 50, null);
        List<PaperScoreDto> candidates = recommendPersonalized(
                userId,
                searchId,
                100,  // Stage1 candidateSize
                50,   // 이 안에서 recId 찾을 거니까 넉넉하게
                null
        );

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
                s.recency(),
                s.userPreference()
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
                s.userPreference(),
                explanation

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

    public List<PaperScoreDto> recommendPersonalized(
            Long userId,
            String sourceArxivId,
            int candidateSize,  // Stage1에서 뽑을 개수 (예: 100)
            int topK,           // 최종 반환할 개수   (예: 10)
            @Nullable List<String> excludeArxivIds
    ) {
        PaperInfo source = getPaperMetadata(sourceArxivId);
        if (source == null) {
            return List.of();
        }

        // 제외 목록 구성 (자기 자신 + optional)
        Set<String> exclude = new HashSet<>(
                Optional.ofNullable(excludeArxivIds).orElse(List.of())
        );
        exclude.add(sourceArxivId);

        // === Stage 1: Pinecone + venue 필터로 후보 생성 ===
        List<Candidate> stage1 = pickCandidatesByPineconeAndVenue(sourceArxivId, candidateSize);
        if (stage1.isEmpty()) {
            return List.of();
        }

        // === Stage 2: user_paper_stats 기반 개인화 랭킹 ===
        UserProfile profile = (userId != null) ? buildUserProfile(userId) : new UserProfile();

        List<PaperScoreDto> scored = new ArrayList<>();

        for (Candidate cand : stage1) {
            PaperInfo p = cand.paper();
            if (exclude.contains(p.getArxivId())) continue;

            double cosine = cand.cosineScore();
            double venueScore = computeVenueScore(source.getVenue(), p.getVenue());
            double categoryScore = computeCategoryScore(
                    source.getPrimaryCategory(), p.getPrimaryCategory());
            double recencyScore = computeRecencyScore(p.getPublishedDate());
            double userPrefScore = computeUserPreferenceScore(profile, p);

            double total = 0.0;
            total += cosine        * weights.get("cosine_similarity");
            total += venueScore    * weights.get("venue_match");
            total += categoryScore * weights.get("category_match");
            total += recencyScore  * weights.get("recency");
            total += userPrefScore * weights.get("user_preference");

            PaperScoreDto.ScoreDetail detail = new PaperScoreDto.ScoreDetail(
                    cosine,
                    venueScore,
                    categoryScore,
                    recencyScore,
                    userPrefScore
            );

            scored.add(new PaperScoreDto(
                    p.getArxivId(),
                    p.getTitle(),
                    p.getVenue(),
                    p.getVenueType(),
                    p.getPrimaryCategory(),
                    p.getPublishedDate(),
                    total,
                    detail
            ));

        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(PaperScoreDto::totalScore).reversed())
                .limit(topK)
                .toList();
    }


    //stage 1
    private List<Candidate> pickCandidatesByPineconeAndVenue(String sourceArxivId,
                                                             int candidateSize) {
        PaperInfo source = getPaperMetadata(sourceArxivId);
        if (source == null) {
            // 기준 논문 없으면 그냥 Pinecone topN 통째로 넘기는 fallback
            return pickCandidatesByPineconeOnly(sourceArxivId, candidateSize);
        }

        String sourceVenue = source.getVenue();
        boolean useVenueFilter = (sourceVenue != null && !sourceVenue.isBlank());

        List<Map.Entry<String, Double>> sims = computeCosineSimilarity(sourceArxivId, 200);
        List<Candidate> candidates = new ArrayList<>();

        for (Map.Entry<String, Double> entry : sims) {
            String candArxivId = entry.getKey();
            double cosine = entry.getValue();

            PaperInfo candPaper = getPaperMetadata(candArxivId);
            if (candPaper == null) continue;

            if (useVenueFilter) {
                double venueScore = computeVenueScore(sourceVenue, candPaper.getVenue());
                // threshold 너무 세면 다 잘려나가니 일단 0.3 정도로 완화
                if (venueScore < 0.3) {
                    continue;
                }
            }

            candidates.add(new Candidate(candPaper, cosine));
            if (candidates.size() >= candidateSize) {
                break;
            }
        }

        // venue 필터를 썼는데 후보가 하나도 없으면 → fallback: venue 필터 없이 한 번 더
        if (candidates.isEmpty() && useVenueFilter) {
            return pickCandidatesByPineconeOnly(sourceArxivId, candidateSize);
        }

        return candidates;
    }

    // venue 필터 없이 Pinecone 상위만 쓰는 fallback
    private List<Candidate> pickCandidatesByPineconeOnly(String sourceArxivId, int candidateSize) {
        List<Map.Entry<String, Double>> sims = computeCosineSimilarity(sourceArxivId, candidateSize);
        List<Candidate> candidates = new ArrayList<>();

        for (Map.Entry<String, Double> entry : sims) {
            String candArxivId = entry.getKey();
            double cosine = entry.getValue();

            PaperInfo candPaper = getPaperMetadata(candArxivId);
            if (candPaper == null) continue;

            candidates.add(new Candidate(candPaper, cosine));
            if (candidates.size() >= candidateSize) {
                break;
            }
        }

        return candidates;
    }

    //stage 2
    private UserProfile buildUserProfile(Long userId) {
        List<UserPaperStats> statsList = userPaperStatsRepository.findByIdUserId(userId);
        if (statsList.isEmpty()) {
            return new UserProfile();
        }

        UserProfile profile = new UserProfile();

        for (UserPaperStats s : statsList) {
            Long paperId = s.getId().getPaperId();
            PaperInfo p = paperInfoRepository.findById(paperId).orElse(null);
            if (p == null) continue;

            String cat = p.getPrimaryCategory();
            String venue = p.getVenue();

            Double completion = s.getCompletionRatio();  // 0~1 범위 기대
            int readTime = s.getTotalReadTimeSec();

            if (cat != null && !cat.isBlank()) {
                CategoryPref cp = profile.categoryMap
                        .computeIfAbsent(cat, k -> new CategoryPref());
                if (completion != null) {
                    cp.sumCompletion += completion;
                    cp.count++;
                }
                cp.totalReadTimeSec += readTime;
            }

            if (venue != null && !venue.isBlank()) {
                VenuePref vp = profile.venueMap
                        .computeIfAbsent(venue, k -> new VenuePref());
                if (completion != null) {
                    vp.sumCompletion += completion;
                    vp.count++;
                }
                vp.totalReadTimeSec += readTime;
            }
        }
        return profile;
    }


    private double computeUserPreferenceScore(UserProfile profile, PaperInfo candidate) {
        String cat = candidate.getPrimaryCategory();
        String venue = candidate.getVenue();

        // 데이터 없을 때 중립값
        double catScore = 0.5;
        double venueScore = 0.5;

        if (cat != null && !cat.isBlank()) {
            CategoryPref cp = profile.categoryMap.get(cat);
            if (cp != null && cp.count > 0) {
                double completionAvg = cp.completionAvg(); // 0~1
                double timeBoost = Math.tanh(cp.totalReadTimeSec / 3600.0); // 읽은 시간(시 단위) 보정
                catScore = 0.7 * completionAvg + 0.3 * timeBoost;
            }
        }

        if (venue != null && !venue.isBlank()) {
            VenuePref vp = profile.venueMap.get(venue);
            if (vp != null && vp.count > 0) {
                double completionAvg = vp.completionAvg();
                double timeBoost = Math.tanh(vp.totalReadTimeSec / 3600.0);
                venueScore = 0.7 * completionAvg + 0.3 * timeBoost;
            }
        }

        // 카테고리, venue 둘 다 고려해서 최종 0~1 스코어
        return 0.7 * catScore + 0.3 * venueScore;
    }

    // Stage1 후보용 내부 DTO
    private record Candidate(PaperInfo paper, double cosineScore) {}



    // === 내부 헬퍼 타입들 ===
    private static class CategoryPref {
        double sumCompletion;
        int count;
        int totalReadTimeSec;

        double completionAvg() {
            return (count == 0) ? 0.0 : sumCompletion / count;
        }
    }

    private static class VenuePref {
        double sumCompletion;
        int count;
        int totalReadTimeSec;

        double completionAvg() {
            return (count == 0) ? 0.0 : sumCompletion / count;
        }
    }

    private static class UserProfile {
        Map<String, CategoryPref> categoryMap = new HashMap<>();
        Map<String, VenuePref> venueMap = new HashMap<>();
    }



}

