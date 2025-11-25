package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.auth.request.SignupReqDto;
import capstone.paperhub_01.controller.auth.response.SignupRespDto;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import capstone.paperhub_01.security.entity.UserRoleEnum;
import capstone.paperhub_01.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public SignupRespDto signup(SignupReqDto dto){
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXIST);
        }

        String encodePassword = passwordEncoder.encode(dto.getPassword());

        Member member = Member.builder()
                .name(dto.getName())
                .nickname(dto.getNickname())
                .password(encodePassword)
                .email(dto.getEmail())
                .role(UserRoleEnum.USER)
                .build();

        Member savedMember = memberRepository.save(member);
        String accessToken = jwtUtil.createToken(savedMember.getId(), savedMember.getEmail());

        return new SignupRespDto(savedMember);
    }

}
