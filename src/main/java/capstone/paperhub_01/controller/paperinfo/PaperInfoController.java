package capstone.paperhub_01.controller.paperinfo;

import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
import capstone.paperhub_01.controller.category.response.SubCategorySummaryResp;
import capstone.paperhub_01.controller.paperinfo.response.PaperInfoSummaryResp;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
import capstone.paperhub_01.util.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaperInfoController {

    private final PaperInfoRepository paperInfoRepository;

    @GetMapping("/paper-infos")
    public ResponseEntity<ApiResult<Page<PaperInfoSummaryResp>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean rollup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 200)));
        Page<PaperInfo> infos = paperInfoRepository.searchByKeywordAndCategory(q, category, rollup, pageable);
        List<PaperInfoSummaryResp> mapped = infos.getContent().stream()
                .map(PaperInfoSummaryResp::from)
                .toList();
        Page<PaperInfoSummaryResp> resp = new PageImpl<>(mapped, pageable, infos.getTotalElements());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    @GetMapping("/paper-infos/categories/root")
    public ResponseEntity<ApiResult<Page<CategorySummaryResp>>> rootCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 200)));
        Page<PaperInfoRepository.CategoryAgg> agg = paperInfoRepository.findRootCategoriesFromInfos(pageable);
        Page<CategorySummaryResp> resp = agg.map(a -> new CategorySummaryResp(a.getCode(), a.getName(), a.getPaperCount(), a.getChildrenCount()));
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }

    @GetMapping("/paper-infos/categories/{code}/children")
    public ResponseEntity<ApiResult<Page<SubCategorySummaryResp>>> childrenCategories(
            @PathVariable String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 500)));
        Page<PaperInfoRepository.CategoryAgg> agg = paperInfoRepository.findChildrenCategoriesFromInfos(code, pageable);
        Page<SubCategorySummaryResp> resp = agg.map(a -> new SubCategorySummaryResp(a.getCode(), a.getName(), a.getPaperCount(), a.getChildrenCount()));
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(resp));
    }
}

