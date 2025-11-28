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
            
            [사용자가 읽은 기준 논문]
            제목: %s
            키워드: %s
            
            [추천된 논문]
            제목: %s
            키워드: %s
            
            [추천 점수 요약]
            - 종합 추천 점수(total): %.4f
            - 내용 유사도(cosine similarity): %.4f
            - 학회·저널 유사도(venue match): %.4f
            - 분야·카테고리 유사도(category match): %.4f
            - 최신성(recency): %.4f
            - 사용자 선호도(user preference): %.4f
            
            위 정보를 바탕으로,
            "왜 이 논문이 추천되었는지"를 한국어로 자연스럽고 간결하게 설명해주세요.
            
            요구사항:
            - 제공된 정보(제목, 키워드, 점수)만 사용하고, 논문 내용을 임의로 생성하지 마세요.
            - 점수들을 단순히 나열하는 대신, 어떤 점수가 높아서 추천에 기여했는지 해석해서 3~5줄 내로 설명하세요.
            - 필요하면 다음과 같이 표현할 수 있습니다:
              • "내용 상 유사성이 높아서"
              • "같은 학회/저널 계열에 속해서"
              • "같은 연구 분야에 속해서"
              • "최근 발표된 논문이라서"
              • "사용자가 평소 많이 읽은 분야/학회와 일치해서"
            - 특히 사용자 선호도(user preference)가 높다면,
              '해당 사용자가 자주 읽는 주제나 학회와 일치하는 경향'을 자연스럽게 설명에 반영하세요.
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
