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
import capstone.paperhub_01.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

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
    @Lazy @Autowired private PaperService self;
    private final S3Service s3Service;
    private final UserPaperStatsService userPaperStatsService;

//s3이전 버전
//    @Transactional
//    public PaperCreateResp uploadAndExtractFromPath(Path path, String originalFilename, Long memberId) {
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
//
//        try {
//            byte[] bytes = Files.readAllBytes(path);
//            String sha256 = sha256Hex(bytes);
//
//            // --- (A) 빠른 경로: 이미 있으면 바로 반환 (낙관 경로) ---
//            var existing = paperRepository.findBySha256(sha256);
//            if (existing.isPresent()) {
//                Paper paper = existing.get();
//                categoryService.syncFromPaperInfo(paper.getId(), null);
//                return PaperCreateResp.from(paper);
//            }
//
//            // --- 파일 저장(멱등) ---
//            Path baseDir = Paths.get(storageProperties.getBaseDir());
//            Files.createDirectories(baseDir);
//            Path dest = baseDir.resolve(sha256 + ".pdf");
//            if (!Files.exists(dest)) {
//                Path tmp = Files.createTempFile(baseDir, sha256, ".part");
//                try {
//                    Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
//                    try {
//                        Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
//                    } catch (AtomicMoveNotSupportedException e) {
//                        Files.move(tmp, dest);
//                    }
//                } catch (FileAlreadyExistsException e) {
//                    try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
//                }
//            }
//            String storageUri = "file://" + dest.toAbsolutePath();
//
//            // --- 메타 추출 ---
//            int numPages;
//            List<Map<String,Object>> pageSizes = new ArrayList<>();
//            Map<String,Object> infoJson = new LinkedHashMap<>();
//            Map<String,Object> xmpJson = null;
//            try (PDDocument doc = Loader.loadPDF(bytes)) {
//                numPages = doc.getNumberOfPages();
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
//                for (int i = 0; i < numPages; i++) {
//                    var mb = doc.getPage(i).getMediaBox();
//                    pageSizes.add(Map.of("w", (double) mb.getWidth(), "h", (double) mb.getHeight()));
//                }
//                var md = doc.getDocumentCatalog().getMetadata();
//                if (md != null) {
//                    try (InputStream is = md.createInputStream()) {
//                        xmpJson = Map.of("raw", new String(is.readAllBytes(), StandardCharsets.UTF_8));
//                    } catch (Exception ignore) {}
//                }
//            }
//
//            // --- (B) INSERT 시도. 중복이면 캐치해서 기존 행 반환 ---
//            Paper paper = Paper.builder()
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
//            try {
//                self.savePaperSafely(paper); // 즉시 INSERT 발생시켜 충돌 빨리 감지
//            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
//                // 유니크 제약(uk_paper_sha256) 충돌: 다른 트랜잭션이 먼저 넣었음
//                paper = paperRepository.findBySha256(sha256)
//                        .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND)); // 이 경우는 거의 없음
//            }
//
//            // --- arXiv ID 추출 & 카테고리 동기화 ---
//            String arxivId = tryExtractArxivId(originalFilename);
//            if (arxivId == null) arxivId = tryExtractArxivId(path.getFileName().toString());
//            if (arxivId == null) arxivId = tryExtractArxivIdFromPdf(bytes);
//
//            categoryService.syncFromPaperInfo(paper.getId(), arxivId);
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


            var existing = paperRepository.findBySha256(sha256);
            if (existing.isPresent()) {
                Paper paper = existing.get();
                categoryService.syncFromPaperInfo(paper.getId(), null);
                return PaperCreateResp.from(paper);
            }


            // 로컬 파일 대신 S3에 업로드
            String key = sha256 + ".pdf";
            String storageUri = s3Service.putPdfIfAbsent(key, bytes, sha256);


            // 메타 추출
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

            // Paper 저장
            var paper = Paper.builder()
                    .sha256(sha256)
                    .fingerprint(null)
                    .infoJson(infoJson.isEmpty() ? null : infoJson)
                    .xmpJson(xmpJson)
                    .numPages(numPages)
                    .pageSizes(pageSizes)
                    .storageUri(storageUri)  // ← S3 URI/URL
                    .uploaderId(String.valueOf(member.getId()))
                    .createdAt(OffsetDateTime.now())
                    .build();

            try {
                self.savePaperSafely(paper);
            } catch (DataIntegrityViolationException dup) {
                paper = paperRepository.findBySha256(sha256)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
            }

            // arXiv ID 추출 + 카테고리 동기화
            String arxivId = tryExtractArxivId(originalFilename);
            if (arxivId == null) arxivId = tryExtractArxivId(path.getFileName().toString());
            if (arxivId == null) arxivId = tryExtractArxivIdFromPdf(bytes);
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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void savePaperSafely(Paper paper) {
        paperRepository.saveAndFlush(paper);
    }

    @Transactional(readOnly = true)
    public byte[] loadPaperFile(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
        String key = paper.getSha256() + ".pdf";
        return s3Service.getBytes(key);
    }
}
