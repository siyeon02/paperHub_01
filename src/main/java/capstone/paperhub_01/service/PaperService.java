package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.controller.paper.response.PaperViewResp;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.config.FileStorageProperties;
import capstone.paperhub_01.domain.paper.repository.PaperRepository;
import capstone.paperhub_01.ex.BusinessException;
import capstone.paperhub_01.ex.ErrorCode;
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
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {
    private final PaperRepository paperRepository;
    private final MemberRepository memberRepository;
    private final FileStorageProperties storageProperties;


//    @Transactional
//    public PaperCreateResp uploadAndExtract(MultipartFile file,  Long memberId) {
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
//        try {
//            // 1) 바이트 해시(SHA-256)
//            byte[] bytes = file.getBytes();
//            String sha256 = sha256Hex(bytes);
//
//            // 2) 중복 체크
//            Optional<Paper> existing = paperRepository.findBySha256(sha256);
//            if (existing.isPresent()) {
//                return PaperCreateResp.from(existing.get()); // idempotent
//            }
//
//            // 3) 파일 저장 (storageUri 생성)
//            Path baseDir = Paths.get(storageProperties.getBaseDir());
//            Files.createDirectories(baseDir);
//            Path dest = baseDir.resolve(sha256 + ".pdf");
//            Files.write(dest, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
//            String storageUri = "file://" + dest.toAbsolutePath();
//
//            // 4) PDF 메타데이터/페이지 사이즈/XMP
//
//            int numPages;
//            List<Map<String, Object>> pageSizes = new ArrayList<>();
//            Map<String, Object> infoJson = new LinkedHashMap<>();
//            Map<String, Object> xmpJson = null;
//
//            try (PDDocument doc = Loader.loadPDF(bytes)) {
//                numPages = doc.getNumberOfPages();
//
//                // Info
//                PDDocumentInformation info = doc.getDocumentInformation();
//                if (info != null) {
//                    putIfNotNull(infoJson, "Title", info.getTitle());
//                    putIfNotNull(infoJson, "Author", info.getAuthor());
//                    putIfNotNull(infoJson, "Subject", info.getSubject());
//                    putIfNotNull(infoJson, "Keywords", info.getKeywords());
//                    putIfNotNull(infoJson, "Creator", info.getCreator());
//                    putIfNotNull(infoJson, "Producer", info.getProducer());
//                    putIfNotNull(infoJson, "CreationDate", info.getCreationDate());
//                    putIfNotNull(infoJson, "ModificationDate", info.getModificationDate());
//                }
//
//                // Page sizes (MediaBox 기준; 단위: PDF points)
//                for (int i = 0; i < numPages; i++) {
//                    var page = doc.getPage(i);
//                    var mb = page.getMediaBox();
//                    Map<String, Object> s = new HashMap<>();
//                    s.put("w", (double) mb.getWidth());
//                    s.put("h", (double) mb.getHeight());
//                    pageSizes.add(s);
//                }
//
//                // XMP (있을 경우)
//                PDMetadata metadata = doc.getDocumentCatalog().getMetadata();
//                if (metadata != null) {
//                    try (InputStream is = metadata.createInputStream()) {
//                        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//                        xmpJson = Map.of("raw", xml);
//                    } catch (Exception ignore) {
//                        // XMP 파싱 실패는 치명적이지 않음
//                    }
//                }
//            }
//
//            // 5) 엔티티 생성/저장
//            Paper paper = Paper.builder()
//                    .sha256(sha256)
//                    .fingerprint(null) // 필요 시 프론트에서 보고한 fingerprint를 PATCH로 저장
//                    .infoJson(infoJson.isEmpty() ? null : infoJson)
//                    .xmpJson(xmpJson)
//                    .numPages(numPages)
//                    .pageSizes(pageSizes)
//                    .storageUri(storageUri)
//                    .uploaderId(String.valueOf(member.getId()))
//                    .createdAt(OffsetDateTime.now())
//                    .build();
//
//            paperRepository.save(paper);
//            return PaperCreateResp.from(paper);
//
//        } catch (IOException e) {
//            throw new IllegalStateException("PDF 업로드/추출 실패: " + e.getMessage(), e);
//        }
//    }

    @Transactional
    public PaperCreateResp uploadAndExtractFromPath(Path path, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            byte[] bytes = Files.readAllBytes(path);
            String sha256 = sha256Hex(bytes);

            // 1) DB 중복 체크: 있으면 파일은 건드리지 않고 바로 리턴
            var existing = paperRepository.findBySha256(sha256);
            if (existing.isPresent()) {
                return PaperCreateResp.from(existing.get());
            }

            // 2) 최종 저장 경로
            Path baseDir = Paths.get(storageProperties.getBaseDir());
            Files.createDirectories(baseDir);
            Path dest = baseDir.resolve(sha256 + ".pdf");

            // 3) 파일이 이미 있으면 재사용 (DB만 새로 저장)
            if (Files.exists(dest)) {
                // pass
            } else {
                // 3-1) 임시 파일에 먼저 쓰고 → 원자적 이동(경합에도 안전)
                Path tmp = Files.createTempFile(baseDir, sha256, ".part");
                try {
                    Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    try {
                        Files.move(tmp, dest, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                        // 파일시스템에 따라 ATOMIC_MOVE가 안 될 수 있어 대체 플랜
                        Files.move(tmp, dest);
                    }
                } catch (FileAlreadyExistsException e) {
                    // 동시성: 다른 쓰레드/요청이 먼저 만들었을 수 있음 → 무시하고 진행
                    try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
                }
            }

            String storageUri = "file://" + dest.toAbsolutePath();

            // 4) PDF 메타데이터 추출 (bytes 그대로 사용해도 OK)
            int numPages;
            List<Map<String,Object>> pageSizes = new ArrayList<>();
            Map<String,Object> infoJson = new LinkedHashMap<>();
            Map<String,Object> xmpJson = null;

            try (PDDocument doc = Loader.loadPDF(bytes)) {
                numPages = doc.getNumberOfPages();

                var info = doc.getDocumentInformation();
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

                for (int i = 0; i < numPages; i++) {
                    var mb = doc.getPage(i).getMediaBox();
                    pageSizes.add(Map.of("w", (double) mb.getWidth(), "h", (double) mb.getHeight()));
                }

                var md = doc.getDocumentCatalog().getMetadata();
                if (md != null) {
                    try (InputStream is = md.createInputStream()) {
                        xmpJson = Map.of("raw", new String(is.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (Exception ignore) {}
                }
            }

            // 5) 엔티티 저장
            var paper = Paper.builder()
                    .sha256(sha256)
                    .fingerprint(null)
                    .infoJson(infoJson.isEmpty() ? null : infoJson)
                    .xmpJson(xmpJson)
                    .numPages(numPages)
                    .pageSizes(pageSizes)
                    .storageUri(storageUri)
                    .uploaderId(String.valueOf(member.getId()))
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

    @Transactional(readOnly = true)
    public PaperViewResp getById(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(()-> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
        return PaperViewResp.from(paper);
    }


    @Transactional
    public Paper setFingerprint(Long paperId, String fingerprint) {
        var paper = paperRepository.findById(paperId)
                .orElseThrow(()-> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        // 동일 fingerprint가 다른 문서에 이미 있으면 에러
        paperRepository.findByFingerprint(fingerprint)
                .filter(p -> !p.getId().equals(paperId))
                .ifPresent(p -> { throw new IllegalStateException("Fingerprint already used by paperId=" + p.getId()); });


        paper.setFingerprint(fingerprint);
        return paper;
    }

    @Transactional(readOnly = true)
    public Paper findBySha256OrFingerPrint(String sha256, String fingerprint) {
        if(sha256 != null && !sha256.isBlank()){
            return paperRepository.findBySha256(sha256)
                    .orElseThrow(()-> new BusinessException(ErrorCode.PAPER_NOT_FOUND));

        }

        if(fingerprint != null && !fingerprint.isBlank()){
            return paperRepository.findByFingerprint(fingerprint)
                    .orElseThrow(()-> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
        }
        throw new IllegalArgumentException("Either sha256 or fingerprint must be provided.");
    }
}
