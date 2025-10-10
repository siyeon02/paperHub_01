package capstone.paperhub_01.controller.Annotation.response;

import lombok.Data;

@Data
public class MemoDeleteResp {
    private Long memoId;

    public MemoDeleteResp(Long memoId) {
        this.memoId = memoId;
    }
}
