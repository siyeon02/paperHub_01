package capstone.paperhub_01.controller.auth.response;

import capstone.paperhub_01.domain.member.Member;
import lombok.Getter;

@Getter
public class SignupRespDto {
    private final Long memberId;

    public SignupRespDto(Member member){
        this.memberId = member.getId();
    }
}
