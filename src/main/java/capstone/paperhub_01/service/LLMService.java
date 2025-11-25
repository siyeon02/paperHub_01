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
                                                  double recencyScore) {

        String prompt = """
                당신은 연구 논문 추천 시스템의 설명을 생성하는 LLM입니다.

                [사용자가 읽은 기준 논문]
                제목: %s
                키워드: %s

                [추천된 논문]
                제목: %s
                키워드: %s

                [추천 점수 정보]
                - 종합 추천 점수: %.4f
                - 내용 유사도(cosine similarity): %.4f
                - 학회/저널 유사도(venue match): %.4f
                - 분야/카테고리 유사도(category match): %.4f
                - 최신성(recency): %.4f

                위 정보를 기반으로,
                "왜 이 논문이 추천되었는지"를 한국어로 자연스럽고 간결하게 설명해주세요.

                요구사항:
                - 논문 내용을 지어내지 말고, 제공된 정보(제목, 키워드, 점수들)만 이용하세요.
                - 사용자가 이해하기 쉽게, 점수들을 해석해서 이유를 3~5줄 정도로 설명하세요.
                - 예를 들어 "제목과 키워드가 유사해서", "같은 학회/저널에서 발표된 논문이라서",
                  "같은 연구 분야에 속해서", "최근에 발표된 최신 연구라서" 와 같이 설명할 수 있습니다.
                - 점수 수치를 그대로 나열하기보다는, 점수가 높고 낮은 피처를 중심으로 자연어로 풀어서 설명하세요.
                """.formatted(
                baseTitle,
                String.join(", ", baseKeywords),
                recTitle,
                String.join(", ", recKeywords),
                totalScore,
                cosineSimilarity,
                venueMatch,
                categoryMatch,
                recencyScore
        );

        return openAiClient.createChatCompletion(prompt);
    }
}
