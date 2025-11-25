package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Annotation.request.MemoCreateReq;
import capstone.paperhub_01.controller.Annotation.request.MemoEditReq;
import capstone.paperhub_01.controller.Annotation.response.MemoCreateResp;
import capstone.paperhub_01.controller.Annotation.response.MemoDeleteResp;
import capstone.paperhub_01.controller.Annotation.response.MemoEditResp;
import capstone.paperhub_01.domain.anchor.repository.AnchorRepository;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.memo.Memo;
import capstone.paperhub_01.domain.memo.repository.MemoRepository;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
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
    private final UserPaperStatsService userPaperStatsService;
    private final MemberRepository memberRepository;
    private final PaperRepository paperRepository;

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

        Memo saved =  memoRepository.save(m);


        Long paperId = paperRepository.findBySha256(anchor.getPaperSha256())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND))
                .getId();

        //하이라이트 1개 추가)
        userPaperStatsService.addMemo(memberId, paperId);

        return saved;

        //return memoRepository.save(m);
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
    public MemoDeleteResp delete(Long id, String requester) {
        memoRepository.deleteById(id);
        return new MemoDeleteResp(id);
    }

    @Transactional
    public MemoEditResp edit(Long id, MemoEditReq req, Long memberId) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(()-> new BusinessException(ErrorCode.MEMO_NOT_FOUND));

        if (!String.valueOf(memberId).equals(memo.getCreatedBy())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newBody = req.getBody() == null ? "" : req.getBody().trim();
        if (newBody.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_BODY);
        }
        if (newBody.length() > 4000) { // 운영 안전용 길이 제한
            newBody = newBody.substring(0, 4000);
        }

        if (newBody.equals(memo.getBody())) {
            return new MemoEditResp(memo);
        }

        memo.setUpdatedAt(OffsetDateTime.now());

        return new MemoEditResp(memo);
    }
}
