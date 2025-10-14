package capstone.paperhub_01.controller.category;

import capstone.paperhub_01.controller.category.response.CategorySummaryResp;
import capstone.paperhub_01.controller.category.response.PaperSummaryResp;
import capstone.paperhub_01.controller.category.response.SubCategorySummaryResp;
import capstone.paperhub_01.domain.category.Category;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import capstone.paperhub_01.service.CategoryService;
import capstone.paperhub_01.util.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories/root")
    public ResponseEntity<ApiResult<Page<CategorySummaryResp>>> getRootCategories(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "root") String level,
            @RequestParam(defaultValue = "false") boolean withCounts,
            @PageableDefault(size = 50) Pageable pageable) {
        Member member = userDetails.getUser();
        Page<CategorySummaryResp> page = categoryService.getRootSummaries(member.getId(), withCounts, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(page));

    }

    @GetMapping("/categories/{code}/children")
    public ResponseEntity<ApiResult<Page<SubCategorySummaryResp>>> getChildren(
            @PathVariable String code,
            @PageableDefault(size = 100) Pageable pageable
    ) {
        Page<SubCategorySummaryResp> page = categoryService.getChildrenSummaries(code, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(page));
    }

    @GetMapping("/categories/{code}/papers")
    public ResponseEntity<ApiResult<Page<PaperSummaryResp>>> getCategoryPapers(
            @PathVariable String code,
            @RequestParam(defaultValue = "false") boolean rollup,
            @PageableDefault(size = 50) Pageable pageable
    ){
        Page<PaperSummaryResp> page = categoryService.getCategoryPapers(code, rollup, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResult.success(page));
    }

}
