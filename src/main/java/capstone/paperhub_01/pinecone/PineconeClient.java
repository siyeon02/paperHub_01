package capstone.paperhub_01.pinecone;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PineconeClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${pinecone.apiKey}")
    private String apiKey;

    @Value("${pinecone.queryUrl}")
    private String queryUrl;

    @Value("${pinecone.namespace:}")
    private String namespace;

    /**
     * Pinecone에 ID 기반 쿼리 전송
     */
    public String queryById(String id, int topK) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("topK", topK);
        body.put("includeMetadata", true);
        if (namespace != null && !namespace.isEmpty()) {
            body.put("namespace", namespace);
        }

        return webClient.post()
                .uri(queryUrl)
                .header("Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
    }


    /**
     * 앱 실행 시 자동 연결 확인
     */
    @PostConstruct
    public void testConnection() {
        System.out.println("🔍 [Pinecone Test] Connecting to " + queryUrl + " ...");
        try {
            Map<String, Object> body = Map.of(
                    "id", "2510.05057v1",  // 존재하는 논문 ID로 테스트
                    "topK", 2,
                    "includeMetadata", true
            );

            String response = webClient.post()
                    .uri(queryUrl)
                    .header("Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));

            System.out.println("✅ [Pinecone Test] Connection OK!");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("❌ [Pinecone Test] Connection failed: " + e.getMessage());
        }
    }
}
