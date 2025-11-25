package capstone.paperhub_01.service;

import capstone.paperhub_01.controller.external.response.ArxivPaperResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Slf4j
@Service
public class ArxivService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_ALLOWED_RESULTS = 50;
    private static final String USER_AGENT = "PaperHub/1.0 (+https://paperhub.local)";

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://export.arxiv.org")
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .build();

    public List<ArxivPaperResp> search(String keyword, int start, int maxResults) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("검색어가 필요합니다.");
        }

        int safeStart = Math.max(0, start);
        int safeMaxResults = Math.min(Math.max(1, maxResults), MAX_ALLOWED_RESULTS);
        String trimmed = keyword.trim();

        String xml = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/query")
                        .queryParam("search_query", "all:" + trimmed)
                        .queryParam("start", safeStart)
                        .queryParam("max_results", safeMaxResults)
                        .build())
                .accept(MediaType.APPLICATION_ATOM_XML)
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        return parseResponse(xml);
    }

    private List<ArxivPaperResp> parseResponse(String xml) {
        if (!StringUtils.hasText(xml)) {
            return Collections.emptyList();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList entries = doc.getElementsByTagName("entry");

            List<ArxivPaperResp> papers = new ArrayList<>();
            for (int i = 0; i < entries.getLength(); i++) {
                Node node = entries.item(i);
                if (!(node instanceof Element entry)) {
                    continue;
                }

                String id = textContent(entry, "id");
                String title = textContent(entry, "title");
                String summary = textContent(entry, "summary");
                String published = textContent(entry, "published");
                List<String> authors = extractAuthors(entry);
                List<String> categories = extractCategories(entry);
                String pdfLink = extractPdfLink(entry, id);

                papers.add(ArxivPaperResp.builder()
                        .id(safeString(id))
                        .title(safeString(title))
                        .summary(safeString(summary))
                        .authors(authors)
                        .pdfLink(pdfLink)
                        .published(safeString(published))
                        .categories(categories)
                        .build());
            }

            return papers;
        } catch (Exception e) {
            log.error("Failed to parse arXiv response", e);
            throw new IllegalStateException("arXiv 응답 파싱에 실패했습니다.", e);
        }
    }

    private String textContent(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node != null ? node.getTextContent().trim() : "";
    }

    private List<String> extractAuthors(Element entry) {
        NodeList authorNodes = entry.getElementsByTagName("author");
        if (authorNodes.getLength() == 0) {
            return Collections.emptyList();
        }
        List<String> authors = new ArrayList<>();
        for (int i = 0; i < authorNodes.getLength(); i++) {
            Node node = authorNodes.item(i);
            if (!(node instanceof Element authorEl)) {
                continue;
            }
            String name = textContent(authorEl, "name");
            if (StringUtils.hasText(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private List<String> extractCategories(Element entry) {
        NodeList categoryNodes = entry.getElementsByTagName("category");
        if (categoryNodes.getLength() == 0) {
            return Collections.emptyList();
        }

        List<String> categories = new ArrayList<>();
        for (int i = 0; i < categoryNodes.getLength(); i++) {
            Node node = categoryNodes.item(i);
            if (node instanceof Element categoryEl) {
                String term = categoryEl.getAttribute("term");
                if (StringUtils.hasText(term)) {
                    categories.add(term);
                }
            }
        }
        return categories;
    }

    private String extractPdfLink(Element entry, String idFallback) {
        NodeList linkNodes = entry.getElementsByTagName("link");
        for (int i = 0; i < linkNodes.getLength(); i++) {
            Node node = linkNodes.item(i);
            if (!(node instanceof Element linkEl)) {
                continue;
            }
            String title = linkEl.getAttribute("title");
            String type = linkEl.getAttribute("type");
            if ("pdf".equalsIgnoreCase(title) || "application/pdf".equalsIgnoreCase(type)) {
                String href = linkEl.getAttribute("href");
                if (StringUtils.hasText(href)) {
                    return href;
                }
            }
        }

        if (StringUtils.hasText(idFallback) && idFallback.contains("/abs/")) {
            return idFallback.replace("/abs/", "/pdf/") + ".pdf";
        }
        return "";
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
