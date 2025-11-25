package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.recommend.response.RecommendExplanationResp;
import capstone.paperhub_01.controller.recommend.response.RecommendResp;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import capstone.paperhub_01.pinecone.PineconeClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final PineconeClient pineconeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LLMService llmService;


    public List<RecommendResp> getSimilarPapers(String searchId, int topK) {
        try {
            String json = pineconeClient.queryById(searchId, topK + 5); // 여유분 요청
            JsonNode root = objectMapper.readTree(json);

            List<RecommendResp> results = new ArrayList<>();
            for (JsonNode m : root.path("matches")) {
                String id = m.path("id").asText("");
                if (id.isEmpty() || searchId.equals(id)) continue; // 자기 자신 제외

                JsonNode meta = m.path("metadata");
                String title = meta.path("title").asText("");
                // authors/keywords는 배열 → List<String>으로 변환
                List<String> authors = nodeToList(meta.path("authors"));
                List<String> keywords = nodeToList(meta.path("keywords"));
                String published = meta.path("published").asText("");
                double score = m.path("score").asDouble(0.0);

                RecommendResp dto = new RecommendResp();
                dto.setArxivId(id);
                dto.setTitle(title);
                dto.setAuthors(authors);
                dto.setKeywords(keywords);
                dto.setPublished(published);
                dto.setScore(score);

                results.add(dto);
            }

            // 점수 내림차순 정렬 후 Top-K
            results.sort(Comparator.comparing(RecommendResp::getScore).reversed());
            return results.stream().limit(topK).toList();

        } catch (Exception e) {
            // 필요 시 로깅/폴백
            throw new BusinessException(ErrorCode.PINECONE_CONNECTION_FAIL);
        }
    }

    private List<String> nodeToList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        return objectMapper.convertValue(node, new TypeReference<List<String>>() {});
    }


    public RecommendExplanationResp getExplanation(String searchArxivId, String recArxivId) {
        try {
            // 1) 기준 논문 메타데이터
            PaperMeta baseMeta = fetchMeta(searchArxivId);

            // 2) 추천 논문 메타데이터
            PaperMeta recMeta = fetchMeta(recArxivId);

            // 3) 기준 논문에서 recId로 추천된 score 찾기 (없으면 0)
            double similarity = fetchSimilarity(searchArxivId, recArxivId);

            // 4) LLM 호출
            String explanation = llmService.generateExplanation(
                    baseMeta.title,
                    baseMeta.keywords,
                    recMeta.title,
                    recMeta.keywords,
                    similarity
            );

            // 5) DTO 변환
            RecommendExplanationResp resp = new RecommendExplanationResp();
            resp.setArXiVId(recArxivId);
            resp.setExplanation(explanation);
            return resp;

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_FAIL);
        }
    }

    private PaperMeta fetchMeta(String arxivId) {
        try {
            String json = pineconeClient.queryById(arxivId, 1);
            JsonNode root = objectMapper.readTree(json);

            JsonNode match = root.path("matches").get(0);
            JsonNode meta = match.path("metadata");

            return new PaperMeta(
                    arxivId,
                    meta.path("title").asText(""),
                    nodeToList(meta.path("authors")),
                    nodeToList(meta.path("keywords"))
            );

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PINECONE_CONNECTION_FAIL);
        }
    }

    private double fetchSimilarity(String baseId, String recId) {
        try {
            String json = pineconeClient.queryById(baseId, 20);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode m : root.path("matches")) {
                if (recId.equals(m.path("id").asText(""))) {
                    return m.path("score").asDouble(0.0);
                }
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static class PaperMeta {
        String arxivId;
        String title;
        List<String> authors;
        List<String> keywords;


        PaperMeta(String arxivId, String title, List<String> authors, List<String> keywords) {
            this.arxivId = arxivId;
            this.title = title;
            this.authors = authors;
            this.keywords = keywords;
        }
    }

}
