package capstone.paperhub_01.LLM;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    public String createChatCompletion(String prompt) {
        try {
            // 1. HTTP 헤더
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 2. Request Body (OpenAI Chat Completions 형식)
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "max_tokens", 512,
                    "temperature", 0.3
            );

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

            // 3. POST 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("OpenAI API 호출 실패: " + response.getStatusCode());
            }

            // 4. JSON 파싱해서 content만 꺼내기
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("OpenAI 응답에 choices가 없습니다.");
            }

            String content = choices.get(0).path("message").path("content").asText("");
            return content.trim();

        } catch (Exception e) {
            throw new RuntimeException("OpenAI Chat Completion 호출 중 오류", e);
        }
    }
}
