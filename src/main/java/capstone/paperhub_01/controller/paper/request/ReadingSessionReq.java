package capstone.paperhub_01.controller.paper.request;

import lombok.Data;

public record ReadingSessionReq (
    int sessionSeconds,
    Integer lastPage,
    Integer maxPage,
    Integer pageCount
) {}
