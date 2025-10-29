package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.paper.response.PaperCreateResp;
import capstone.paperhub_01.controller.paper.response.PaperViewResp;
import capstone.paperhub_01.domain.member.Member;
import capstone.paperhub_01.domain.member.repository.MemberRepository;
import capstone.paperhub_01.domain.paper.Paper;
import capstone.paperhub_01.domain.paper.PaperInfo;
import capstone.paperhub_01.domain.paper.config.FileStorageProperties;
import capstone.paperhub_01.domain.paper.repository.PaperInfoRepository;
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
    private final PaperInfoRepository paperInfoRepository;
    private final CategoryService categoryService;

//    @Transactional
//    public PaperCreateResp uploadAndExtractFromPath(Path path, Long memberId) {
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
//
//        try {
//            byte[] bytes = Files.readAllBytes(path);
//            String sha256 = sha256Hex(bytes);
//
//            // 1) DB 중복 체크: 있으면 파일은 건드리지 않고 바로 리턴
//            var existing = paperRepository.findBySha256(sha256);
//            if (existing.isPresent()) {
//                Paper paper = existing.get();
//                categoryService.syncFromPaperInfo(paper.getId(), null);
//
//                return PaperCreateResp.from(existing.get());
//            }
//
//            // 2) 최종 저장 경로
//            Path baseDir = Paths.get(storageProperties.getBaseDir());
//            Files.createDirectories(baseDir);
//            Path dest = baseDir.resolve(sha256 + ".pdf");
//
//            // 3) 파일이 이미 있으면 재사용 (DB만 새로 저장)
//            if (Files.exists(dest)) {
//                // pass
//            } else {
//                // 3-1) 임시 파일에 먼저 쓰고 → 원자적 이동(경합에도 안전)
//                Path tmp = Files.createTempFile(baseDir, sha256, ".part");
//                try {
//                    Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
//                    try {
//                        Files.move(tmp, dest, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
//                    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
//                        // 파일시스템에 따라 ATOMIC_MOVE가 안 될 수 있어 대체 플랜
//                        Files.move(tmp, dest);
//                    }
//                } catch (FileAlreadyExistsException e) {
//                    // 동시성: 다른 쓰레드/요청이 먼저 만들었을 수 있음 → 무시하고 진행
//                    try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
//                }
//            }
//
//            String storageUri = "file://" + dest.toAbsolutePath();
//
//            // 4) PDF 메타데이터 추출 (bytes 그대로 사용해도 OK)
//            int numPages;
//            List<Map<String,Object>> pageSizes = new ArrayList<>();
//            Map<String,Object> infoJson = new LinkedHashMap<>();
//            Map<String,Object> xmpJson = null;
//
//            try (PDDocument doc = Loader.loadPDF(bytes)) {
//                numPages = doc.getNumberOfPages();
//
//                var info = doc.getDocumentInformation();
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
//                for (int i = 0; i < numPages; i++) {
//                    var mb = doc.getPage(i).getMediaBox();
//                    pageSizes.add(Map.of("w", (double) mb.getWidth(), "h", (double) mb.getHeight()));
//                }
//
//                var md = doc.getDocumentCatalog().getMetadata();
//                if (md != null) {
//                    try (InputStream is = md.createInputStream()) {
//                        xmpJson = Map.of("raw", new String(is.readAllBytes(), StandardCharsets.UTF_8));
//                    } catch (Exception ignore) {}
//                }
//            }
//
//            // 5) 엔티티 저장
//            var paper = Paper.builder()
//                    .sha256(sha256)
//                    .fingerprint(null)
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
//
//            String extractedArxivId = extractArxivIdFromPath(path);
//
//            categoryService.syncFromPaperInfo(paper.getId(), extractedArxivId);
//
//            return PaperCreateResp.from(paper);
//
//        } catch (IOException e) {
//            throw new IllegalStateException("PDF 업로드/추출 실패: " + e.getMessage(), e);
//        }
//    }

    @Transactional
    public PaperCreateResp uploadAndExtractFromPath(Path path, String originalFilename, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            byte[] bytes = Files.readAllBytes(path);
            String sha256 = sha256Hex(bytes);

            // 1) 중복 문서면 멱등 동기화 후 반환
            var existing = paperRepository.findBySha256(sha256);
            if (existing.isPresent()) {
                Paper paper = existing.get();
                categoryService.syncFromPaperInfo(paper.getId(), null); // paper_id 우선
                return PaperCreateResp.from(paper);
            }

            // 2) 파일 저장 (sha256.pdf)
            Path baseDir = Paths.get(storageProperties.getBaseDir());
            Files.createDirectories(baseDir);
            Path dest = baseDir.resolve(sha256 + ".pdf");
            if (!Files.exists(dest)) {
                Path tmp = Files.createTempFile(baseDir, sha256, ".part");
                try {
                    Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    try {
                        Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(tmp, dest);
                    }
                } catch (FileAlreadyExistsException e) {
                    try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
                }
            }

            String storageUri = "file://" + dest.toAbsolutePath();

            // 3) 메타 추출
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

            // 4) Paper 저장
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

            // 5) ✅ arXiv ID 추출: originalFilename → path → PDF 본문(1~2p) 순서로 시도
            String arxivId = tryExtractArxivId(originalFilename);
            if (arxivId == null) arxivId = tryExtractArxivId(path.getFileName().toString());
            if (arxivId == null) arxivId = tryExtractArxivIdFromPdf(bytes);

            // 6) 카테고리 동기화 (paper_id 우선, arxivId 버전/비버전 둘 다 시도는 CategoryService에서)
            categoryService.syncFromPaperInfo(paper.getId(), arxivId);

            return PaperCreateResp.from(paper);

        } catch (IOException e) {
            throw new IllegalStateException("PDF 업로드/추출 실패: " + e.getMessage(), e);
        }
    }

    private static final java.util.regex.Pattern ARXIV_RX_FULL =
            java.util.regex.Pattern.compile("(?:arXiv\\s*:\\s*)?(\\d{4}\\.\\d{4,5}(?:v\\d+)?)",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private String tryExtractArxivId(String source) {
        if (source == null || source.isBlank()) return null;
        var m = ARXIV_RX_FULL.matcher(source);
        return m.find() ? m.group(1) : null; // 예: 1812.10425v1
    }

    private String tryExtractArxivIdFromPdf(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(2, doc.getNumberOfPages())); // 1~2페이지에서만 검색
            String txt = stripper.getText(doc);
            return tryExtractArxivId(txt);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractArxivIdFromPath(Path path) {
        String name = path.getFileName().toString();
        // 예: 2509.11104.pdf → 2509.11104
        int dot = name.indexOf('.');
        return (dot > 0) ? name.substring(0, dot) : null;
    }

    /** PaperInfo의 저장 형태에 맞춰 카테고리 코드 파싱 */
    private List<String> extractCodesFromInfo(PaperInfo info) {
        List<String> arr = info.getCategories();
        if (arr == null) return List.of();
        return arr.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(this::normalize)
                .distinct()
                .toList();
    }

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", "");
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
