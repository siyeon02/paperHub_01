package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.Collection.request.CollectionPaperCreateReq;
import capstone.paperhub_01.controller.Collection.response.CollectionPaperCreateResp;
import capstone.paperhub_01.domain.collection.CollectionPaper;
import capstone.paperhub_01.domain.collection.ReadingStatus;
import capstone.paperhub_01.domain.collection.repository.CollectionPaperRepository;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;


@Service
@RequiredArgsConstructor
public class CollectionService {

    private final PaperRepository paperRepository;
    private final CollectionPaperRepository collectionPaperRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CollectionPaperCreateResp createCollectionPapers(String status, CollectionPaperCreateReq req, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Paper paper = paperRepository.findById(req.getPaperId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        ReadingStatus rs = parseStatus(status);



        CollectionPaper cp = new CollectionPaper();
            cp.setMember(member);
            cp.setPaper(paper);
            cp.setStatus(rs);
            cp.setLastOpenedAt(OffsetDateTime.now());
            cp.setAddedAt(OffsetDateTime.now());
            cp.setUpdatedAt(OffsetDateTime.now());

            collectionPaperRepository.save(cp);


        CollectionPaperCreateResp resp = new CollectionPaperCreateResp();
        resp.setCollectionPaperId(cp.getId());
        resp.setPaperId(cp.getPaper().getId());
        resp.setStatus(cp.getStatus());
        resp.setLastOpenedAt(cp.getLastOpenedAt());
        resp.setAddedAt(cp.getAddedAt());
        resp.setUpdatedAt(cp.getUpdatedAt());

        return resp;

    }

    private ReadingStatus parseStatus(String s) {
        return switch (s.toLowerCase()) {
            case "to-read", "to_read", "toread" -> ReadingStatus.TO_READ;
            case "in-progress", "in_progress", "inprogress" -> ReadingStatus.IN_PROGRESS;
            case "done" -> ReadingStatus.DONE;
            default -> throw new BusinessException(ErrorCode.INVALID_STATUS);
        };
    }
}
