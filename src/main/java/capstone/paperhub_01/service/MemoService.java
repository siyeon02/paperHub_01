package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Annotation.request.MemoCreateReq;
import capstone.paperhub_01.controller.Annotation.response.MemoCreateResp;
import capstone.paperhub_01.domain.anchor.repository.AnchorRepository;
import capstone.paperhub_01.domain.memo.repository.Memo;
import capstone.paperhub_01.domain.memo.repository.MemoRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final AnchorRepository anchorRepository;

    @Transactional
    public Memo create(MemoCreateReq req, Long memberId) {
        var anchor = anchorRepository.findById(req.getAnchorId())
        .orElseThrow(() -> new BusinessException(ErrorCode.ANCHOR_NOT_FOUND));

        var m = new Memo();
        m.setAnchor(anchor);
        m.setPaperSha256(anchor.getPaperSha256());
        m.setPage(anchor.getPage());
        m.setBody(req.getBody().trim());
        m.setCreatedBy(String.valueOf(memberId));
        m.setCreatedAt(OffsetDateTime.now());
        m.setUpdatedAt(OffsetDateTime.now());

        if (req.getParentId()!=null) {
            var parent = memoRepository.findById(req.getParentId()).orElseThrow();
            m.setParent(parent);
        }
        return memoRepository.save(m);
    }

    @Transactional(readOnly = true)
    public List<MemoCreateResp> listByPage(String sha256, Integer page) {
        return memoRepository.findByPaperSha256AndPage(sha256, page)
                .stream().map(MemoCreateResp::from).toList();
    }

    @Transactional
    public MemoCreateResp updateBody(Long id, String body, String editor) {
        var m = memoRepository.findById(id).orElseThrow();
        // TODO: 권한 체크(작성자만 수정 등)
        m.setBody(body.trim());
        return MemoCreateResp.from(m);
    }

    @Transactional
    public void delete(Long id, String requester) {
        // TODO: 권한 체크
        memoRepository.deleteById(id);
    }

}
