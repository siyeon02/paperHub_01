package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.config.FileStorageProperties;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {
    private final PaperRepository paperRepository;
    private final MemberRepository memberRepository;
    private final FileStorageProperties storageProperties;


    @Transactional
    public PaperCreateResp uploadAndExtract(MultipartFile file, String sourceId, String uploaderId) {
        try {
            // 1) 바이트 해시(SHA-256)
            byte[] bytes = file.getBytes();
            String sha256 = sha256Hex(bytes);

            // 2) 중복 체크
            Optional<Paper> existing = paperRepository.findBySha256(sha256);
            if (existing.isPresent()) {
                return PaperCreateResp.from(existing.get()); // idempotent
            }

            // 3) 파일 저장 (storageUri 생성)
            Path baseDir = Paths.get(storageProperties.getBaseDir());
            Files.createDirectories(baseDir);
            Path dest = baseDir.resolve(sha256 + ".pdf");
            Files.write(dest, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            String storageUri = "file://" + dest.toAbsolutePath();

            // 4) PDF 메타데이터/페이지 사이즈/XMP

            int numPages;
            List<Map<String, Object>> pageSizes = new ArrayList<>();
            Map<String, Object> infoJson = new LinkedHashMap<>();
            Map<String, Object> xmpJson = null;

            try (PDDocument doc = Loader.loadPDF(bytes)) {
                numPages = doc.getNumberOfPages();

                // Info
                PDDocumentInformation info = doc.getDocumentInformation();
                if (info != null) {
                    putIfNotNull(infoJson, "Title", info.getTitle());
                    putIfNotNull(infoJson, "Author", info.getAuthor());
                    putIfNotNull(infoJson, "Subject", info.getSubject());
                    putIfNotNull(infoJson, "Keywords", info.getKeywords());
                    putIfNotNull(infoJson, "Creator", info.getCreator());
                    putIfNotNull(infoJson, "Producer", info.getProducer());
                    putIfNotNull(infoJson, "CreationDate", info.getCreationDate());
                    putIfNotNull(infoJson, "ModificationDate", info.getModificationDate());
                }

                // Page sizes (MediaBox 기준; 단위: PDF points)
                for (int i = 0; i < numPages; i++) {
                    var page = doc.getPage(i);
                    var mb = page.getMediaBox();
                    Map<String, Object> s = new HashMap<>();
                    s.put("w", (double) mb.getWidth());
                    s.put("h", (double) mb.getHeight());
                    pageSizes.add(s);
                }

                // XMP (있을 경우)
                PDMetadata metadata = doc.getDocumentCatalog().getMetadata();
                if (metadata != null) {
                    try (InputStream is = metadata.createInputStream()) {
                        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        xmpJson = Map.of("raw", xml);
                    } catch (Exception ignore) {
                        // XMP 파싱 실패는 치명적이지 않음
                    }
                }
            }

            // 5) 엔티티 생성/저장
            Paper paper = Paper.builder()
                    .sha256(sha256)
                    .fingerprint(null) // 필요 시 프론트에서 보고한 fingerprint를 PATCH로 저장
                    .sourceId(sourceId)
                    .title((String) infoJson.getOrDefault("Title", null))
                    .author((String) infoJson.getOrDefault("Author", null))
                    .infoJson(infoJson.isEmpty() ? null : infoJson)
                    .xmpJson(xmpJson)
                    .numPages(numPages)
                    .pageSizes(pageSizes)
                    .storageUri(storageUri)
                    .uploaderId(uploaderId)
                    .createdAt(OffsetDateTime.now())
                    .build();

            paperRepository.save(paper);
            return PaperCreateResp.from(paper);

        } catch (IOException e) {
            throw new IllegalStateException("PDF 업로드/추출 실패: " + e.getMessage(), e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 계산 실패", e);
        }
    }


    private static void putIfNotNull(Map<String, Object> map, String k, Object v) {
        if (v != null) map.put(k, v);
    }
}
