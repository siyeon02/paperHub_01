package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.recommend.response.RecommendResp;
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


    public List<RecommendResp> getSimilarPapers(String arxivId, int topK) {
        try {
            String json = pineconeClient.queryById(arxivId, topK + 5); // 여유분 요청
            JsonNode root = objectMapper.readTree(json);

            List<RecommendResp> results = new ArrayList<>();
            for (JsonNode m : root.path("matches")) {
                String id = m.path("id").asText("");
                if (id.isEmpty() || arxivId.equals(id)) continue; // 자기 자신 제외

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
            throw new RuntimeException("Pinecone 응답 파싱 실패", e);
        }
    }

    private List<String> nodeToList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        return objectMapper.convertValue(node, new TypeReference<List<String>>() {});
    }
}
