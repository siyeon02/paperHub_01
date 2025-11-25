package capstone.paperhub_01.controller.Annotation.response;

import capstone.paperhub_01.service.PageAnnotationQueryService;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PageAnnotationsResp {
    private int count;
    private List<Item> items;
    private int totalHighlights;
    private int totalNotes;
    private int totalAnnotations;

    @Data
    public static class Item {
        private Anchor anchor;
        private List<Highlight> highlights;
        private List<Memo> notes;
    }

    @Data
    public static class Anchor {
        private Long id;
        private String signature;
        private String exact, prefix, suffix;
        private List<Rect> rects; // 정규화 좌표(0~1)
    }

    @Data
    public static class Rect {
        private double x, y, w, h;
    }

    @Data
    public static class Highlight {
        private Long id;
        private String color;
        private String createdBy;
    }

    @Data
    public static class Memo {
        private Long id;
        private String body;
        private String createdBy;
        private OffsetDateTime createdAt;
        private Long parentId;
    }

}
