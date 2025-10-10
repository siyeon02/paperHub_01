package capstone.paperhub_01.security.util;


import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TIME = 120 * 60 * 1000L; // 120분

    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(Long userId, String email) {
        Date date = new Date();

        return BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(String.valueOf(userId))
                        .claim("email", email)
                        //.claim("nickname", nickname)
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME))
                        .setIssuedAt(date) // 발급일
                        .signWith(key, signatureAlgorithm) // 암호화 알고리즘
                        .compact();
    }

    public String substringToken(String tokenValue) {
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring("Bearer ".length());
        }
        throw new BusinessException(ErrorCode.TOKEN_MISSING);
    }

    public Claims parseToken(String tokenValue) {
        try {
            // 토큰에서 Claims를 추출
            return Jwts.parserBuilder()
                    .setSigningKey(key)  // 암호화 키 설정
                    .build()
                    .parseClaimsJws(tokenValue)  // JWT 토큰 파싱
                    .getBody();  // claims 반환
        } catch (ExpiredJwtException e) {
            // 토큰 만료 예외 처리
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            // JWT 형식이 지원되지 않을 때 예외 처리
            throw new BusinessException(ErrorCode.INVALID_TOKEN_TYPE);
        } catch (MalformedJwtException e) {
            // 잘못된 JWT 형식에 대한 예외 처리
            throw new BusinessException(ErrorCode.INVALID_TOKEN_FORMAT);
        } catch (Exception e) {
            // 일반적인 예외 처리 (파서오류)
            throw new BusinessException(ErrorCode.INVALID_TOKEN_PARSING);
        }
    }

    public boolean verifyAccessToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
