package capstone.paperhub_01.service;

import capstone.paperhub_01.domain.anchor.Anchor;
import capstone.paperhub_01.domain.anchor.repository.AnchorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnchorService {
    private final AnchorRepository anchorRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Anchor upsert(String sha256, int page, List<Map<String, Object>> rects, String exact, String prefix, String suffix, String signature, String createdBy) {
        String sig = (signature != null && !signature.isBlank())
                ? signature
                : computeSignature(exact, prefix, suffix, rects);

        return anchorRepository.findByPaperSha256AndPageAndSignature(sha256, page, sig)
                .orElseGet(() -> {
                    var a = new Anchor();
                    a.setPaperSha256(sha256);
                    a.setPage(page);
                    a.setRects(rects);
                    a.setExact(exact);
                    a.setPrefix(prefix);
                    a.setSuffix(suffix);
                    a.setSignature(sig);
                    a.setCreatedBy(createdBy);
                    a.setCreatedAt(OffsetDateTime.now());
                    return anchorRepository.save(a);
                });
    }

    private String computeSignature(String exact, String prefix, String suffix,
                                    List<Map<String, Object>> rects) {
        // 우선순위: 텍스트 해시 → 없으면 rects 해시
        if (exact != null && !exact.isBlank()) {
            var base = (prefix == null ? "" : prefix) + "|" + exact + "|" + (suffix == null ? "" : suffix);
            return "t:" + HexFormat.of().formatHex(sha256(base.getBytes(StandardCharsets.UTF_8)));
        }
        try {
            String json = objectMapper.writeValueAsString(rects == null ? List.of() : rects);
            return "r:" + HexFormat.of().formatHex(sha256(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("signature 계산 실패", e);
        }
    }

    private byte[] sha256(byte[] b) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(b);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

