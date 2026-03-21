package lexicalwebsite.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lexicalwebsite.beans.DocumentView;
import lexicalwebsite.beans.SearchPage;
import lexicalwebsite.beans.SearchResultItem;

/**
 * Service for interacting with Elasticsearch to perform search queries and
 * fetch documents.
 */

@Service
public class ElasticsearchService {

	private final String elasticBaseUrl;
	
	private final String indexName;
	
	private final ObjectMapper objectMapper;
	
	private final RestTemplate restTemplate;
	
	private final int snippetLength;

	public ElasticsearchService(ObjectMapper objectMapper,
			@Value("${app.elasticsearch.base-url:http://localhost:9200}") String elasticBaseUrl,
			@Value("${app.elasticsearch.index-name:lex_text_files}") String indexName,
			@Value("${app.elasticsearch.snippet-length:200}") int snippetLength) {
		this.restTemplate = new RestTemplate();
		this.objectMapper = objectMapper;
		this.elasticBaseUrl = elasticBaseUrl;
		this.indexName = indexName;
		this.snippetLength = snippetLength;
	}

	private String applyFirstHighlight(String content, String query) {
		String safeContent = content == null ? "" : content;
		if (query == null || query.isBlank()) {
			return escapeHtml(safeContent);
		}

		String lowerContent = safeContent.toLowerCase();
		String lowerQuery = query.toLowerCase();
		int idx = lowerContent.indexOf(lowerQuery);
		if (idx < 0) {
			return escapeHtml(safeContent);
		}

		String before = escapeHtml(safeContent.substring(0, idx));
		String match = escapeHtml(safeContent.substring(idx, idx + query.length()));
		String after = escapeHtml(safeContent.substring(idx + query.length()));
		return before + "<mark id=\"firstHit\">" + match + "</mark>" + after;
	}

	private String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private JsonNode executeSearch(Map<String, Object> payload) {
		String url = elasticBaseUrl + "/" + indexName + "/_search";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Could not connect to Elasticsearch", ex);
		}

		try {
			return objectMapper.readTree(response.getBody());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not parse Elasticsearch response", ex);
		}
	}

	private String extractSnippet(JsonNode hit, String content) {
		JsonNode highlights = hit.path("highlight").path("content");
		if (highlights.isArray() && !highlights.isEmpty()) {
			return trimToSnippetLength(highlights.get(0).asText(""));
		}
		return trimToSnippetLength(content);
	}

	public DocumentView getDocumentById(String id, String query) {
		String url = elasticBaseUrl + "/" + indexName + "/_doc/" + id;
		ResponseEntity<String> response;
		try {
			response = restTemplate.getForEntity(url, String.class);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Could not fetch document from Elasticsearch", ex);
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(response.getBody());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not parse Elasticsearch document response", ex);
		}

		JsonNode source = root.path("_source");
		String filename = source.path("filename").asText("(unknown file)");
		String content = source.path("content").asText("");

		String highlighted = applyFirstHighlight(content, query);
		boolean hasHighlight = highlighted.contains("id=\"firstHit\"");
		return new DocumentView(id, filename, content, highlighted, hasHighlight);
	}

	public SearchPage search(String query, int page, int size) {
		int from = Math.max(page - 1, 0) * size;

		Map<String, Object> payload = Map.of("from", from, "size", size, "query",
				Map.of("multi_match", Map.of("query", query, "fields", List.of("filename^2", "content"))), "highlight",
				Map.of("fields", Map.of("content", Map.of("fragment_size", snippetLength, "number_of_fragments", 1)),
						"pre_tags", List.of("<mark>"), "post_tags", List.of("</mark>")));

		JsonNode root = executeSearch(payload);
		JsonNode hits = root.path("hits").path("hits");
		long total = root.path("hits").path("total").path("value").asLong(0);

		List<SearchResultItem> items = new ArrayList<>();
		for (JsonNode hit : hits) {
			String id = hit.path("_id").asText();
			JsonNode source = hit.path("_source");
			String filename = source.path("filename").asText("(unknown file)");
			String content = source.path("content").asText("");
			String snippet = extractSnippet(hit, content);
			items.add(new SearchResultItem(id, filename, snippet));
		}

		return new SearchPage(items, total);
	}

	private String trimToSnippetLength(String value) {
		String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
		if (normalized.length() <= snippetLength) {
			return normalized;
		}
		return normalized.substring(0, snippetLength) + "...";
	}
}