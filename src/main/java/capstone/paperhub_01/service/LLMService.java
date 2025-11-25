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
}
