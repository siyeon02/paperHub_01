package capstone.paperhub_01.security.service;

import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.security.entity.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

        private final MemberRepository memberRepository;

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자 찾을 수 없습니다"));
            return new UserDetailsImpl(member);
        }

}
