package capstone.paperhub_01.service;

import capstone.paperhub_01.LLM.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LLMService {

    private final OpenAiClient openAiClient;

    public String generateExplanation(String baseTitle,
                                      List<String> baseKeywords,
                                      String recTitle,
                                      List<String> recKeywords,
                                      double similarity) {

        String prompt = """
                당신은 연구 논문 추천 시스템의 설명 생성 LLM입니다.

                [사용자가 읽은 기준 논문]
                제목: %s
                키워드: %s

                [추천된 논문]
                제목: %s
                키워드: %s
                유사도 점수: %.4f

                위 정보를 기반으로,
                "왜 이 논문이 추천되었는지"를 한국어로 자연스럽고 간결하게 설명해주세요.
                - 논문 내용을 지어내지 말고, 제공된 정보(제목, 키워드, 유사도)만 이용하세요.
                - 3~5줄 이내로 요약해서 작성하세요.
                """.formatted(
                baseTitle,
                String.join(", ", baseKeywords),
                recTitle,
                String.join(", ", recKeywords),
                similarity
        );

        return openAiClient.createChatCompletion(prompt);
    }

    public String generateExplanationMultiFeature(String baseTitle,
                                                  List<String> baseKeywords,
                                                  String recTitle,
                                                  List<String> recKeywords,
                                                  double totalScore,
                                                  double cosineSimilarity,
                                                  double venueMatch,
                                                  double categoryMatch,
                                                  double recencyScore,
                                                  double userPreference) {

        String prompt = """
당신은 연구 논문 추천 시스템의 설명을 생성하는 LLM입니다.
당신의 목적은 "추천 점수들을 해석하여, 왜 이 논문이 추천되었는지 근거 기반으로 설명하는 것"입니다.
절대로 논문 내용을 추측하거나, 제공되지 않은 정보를 생성하지 마세요.

────────────────────────────────
[기준 논문 정보]
제목: %s
키워드: %s

[추천된 논문 정보]
제목: %s
키워드: %s

[추천 점수 요약]
- 종합 점수(total): %.4f
- 내용 유사도(cosine similarity): %.4f
- 학회·저널 유사도(venue match): %.4f
- 분야·카테고리 유사도(category match): %.4f
- 최신성(recency): %.4f
- 사용자 선호도(user preference): %.4f
────────────────────────────────

[설명 생성 규칙]

1) 제공된 데이터(제목, 키워드, 점수)를 기반으로 **원인-결과 구조**로 설명하세요.
   예: "A 점수가 높기 때문에 B한 논문으로 추천되었습니다."

2) 점수 해석 기준:
   - 0.7 이상: "높다", "강하게 기여했다"
   - 0.4 ~ 0.7: "보통 수준으로 기여했다"
   - 0.4 미만: "기여도가 낮다" → 문장에서 언급하지 않아도 됨

3) 아래 요소를 중심으로 설명을 구성하세요:
   - **내용 측면**: 키워드 겹침, 연구 문제의 유사성 등
   - **연구 맥락 측면**: venue/학회 계열이 비슷한지
   - **분야 계열 측면**: 카테고리/분야가 얼마나 가까운지
   - **시간적 측면**: 최신성 점수가 높다면 반영
   - **사용자 측면**: 사용자 선호도가 높다면  
     → "사용자가 평소 선호하는 분야 또는 학회 경향과 일치"라고 자연스럽게 설명

4) 금지:
   - 논문 내용을 임의로 추정하거나 생성하지 마세요.
   - 점수만 나열하지 말고 반드시 “의미적 이유”로 연결하세요.
   - 3~5문장 이내로 작성하세요.

5) 문장 스타일:
   - 간결하지만 **근거가 분명하게 보이는 문장**
   - “높은 편”, “강하게 기여” 같은 비교/강조 표현을 적절히 사용

이제 위 규칙을 바탕으로 “이 논문이 추천된 이유”를 한국어로 설명하세요.
""".formatted(

        baseTitle,
                String.join(", ", baseKeywords),
                recTitle,
                String.join(", ", recKeywords),
                totalScore,
                cosineSimilarity,
                venueMatch,
                categoryMatch,
                recencyScore,
                userPreference
        );

        return openAiClient.createChatCompletion(prompt);
    }
}
