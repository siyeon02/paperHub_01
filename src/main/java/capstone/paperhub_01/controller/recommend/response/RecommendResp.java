package capstone.paperhub_01.controller.recommend.response;

import lombok.Data;

import java.util.List;

@Data
public class RecommendResp {
    private String arxivId;
    private String title;
    private List<String> authors;
    private List<String> keywords;
    private String published;   // yyyy-MM-dd
    private Double score;        // 최종스코어(재랭킹 반영 가능)
}
