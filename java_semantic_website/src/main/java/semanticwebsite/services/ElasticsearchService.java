package semanticwebsite.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import semanticwebsite.beans.DocumentView;
import semanticwebsite.beans.SearchPage;
import semanticwebsite.beans.SearchResultItem;

/**
 * Service for interacting with Elasticsearch (semantic and lexical indices) and
 * Ollama (embedding generation) to perform semantic search and document
 * retrieval.
 */

@Service
public class ElasticsearchService {

	/** Base URL for Elasticsearch (e.g. http://localhost:9200). */
	private final String elasticBaseUrl;

	/**
	 * Number of nearest neighbors to request from Elasticsearch for each search.
	 */
	private final int knnK;

	/**
	 * Number of candidate vectors to consider for kNN search (Elasticsearch
	 * setting).
	 */
	private final int knnNumCandidates;

	/** Name of the Elasticsearch index for lexical (full document) search. */
	private final String lexicalIndex;

	/** Name of the Elasticsearch index for semantic (vector) search. */
	private final ObjectMapper objectMapper;

	/** URL of the Ollama embeddings endpoint. */
	private final String ollamaEmbeddingsUrl;

	/** Name of the Ollama model to use for generating embeddings. */
	private final String ollamaModel;

	/** RestTemplate for making HTTP requests to Elasticsearch and Ollama. */
	private final RestTemplate restTemplate;

	/** Name of the Elasticsearch index for semantic (vector) search. */
	private final String semanticIndex;

	/** Maximum length of text snippets to return in search results. */
	private final int snippetLength;

	/**
	 * Constructor for ElasticsearchService. Initializes all required dependencies
	 * and configuration properties.
	 * 
	 * @param objectMapper        ObjectMapper for parsing JSON responses from
	 *                            Elasticsearch and Ollama.
	 * @param restTemplate        RestTemplate for making HTTP requests to
	 *                            Elasticsearch and Ollama.
	 * @param elasticBaseUrl      Base URL for Elasticsearch (e.g.
	 *                            http://localhost:9200).
	 * @param semanticIndex       Name of the Elasticsearch index for semantic
	 *                            (vector) search.
	 * @param lexicalIndex        Name of the Elasticsearch index for lexical (full
	 *                            document) search.
	 * @param ollamaEmbeddingsUrl URL of the Ollama embeddings endpoint.
	 * @param ollamaModel         Name of the Ollama model to use for generating
	 *                            embeddings.
	 * @param snippetLength       Maximum length of text snippets to return in
	 *                            search results.
	 * @param knnK                Number of nearest neighbors to request from
	 *                            Elasticsearch for each search.
	 * @param knnNumCandidates    Number of candidate vectors to consider for kNN
	 *                            search (Elasticsearch setting).
	 */
	public ElasticsearchService(ObjectMapper objectMapper, RestTemplate restTemplate,
			@Value("${app.elasticsearch.base-url}") String elasticBaseUrl,
			@Value("${app.elasticsearch.semantic-index}") String semanticIndex,
			@Value("${app.elasticsearch.lexical-index}") String lexicalIndex,
			@Value("${app.ollama.embeddings-url}") String ollamaEmbeddingsUrl,
			@Value("${app.ollama.model}") String ollamaModel,
			@Value("${app.elasticsearch.snippet-length:200}") int snippetLength,
			@Value("${app.search.knn-k:50}") int knnK,
			@Value("${app.search.knn-num-candidates:100}") int knnNumCandidates) {
		this.objectMapper = objectMapper;
		this.restTemplate = restTemplate;
		this.elasticBaseUrl = elasticBaseUrl;
		this.semanticIndex = semanticIndex;
		this.lexicalIndex = lexicalIndex;
		this.ollamaEmbeddingsUrl = ollamaEmbeddingsUrl;
		this.ollamaModel = ollamaModel;
		this.snippetLength = snippetLength;
		this.knnK = knnK;
		this.knnNumCandidates = knnNumCandidates;
	}

	/**
	 * Applies first-hit highlighting to the given content based on the query.
	 * 
	 * @param content The original content to apply highlighting to.
	 * @param query   The search query to find and highlight in the content.
	 * 
	 * @return The content with the first occurrence of the query term highlighted,
	 *         or the original content escaped for HTML if the query is blank or not
	 *         found.f
	 */
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

	/**
	 * Escapes special HTML characters in the given string to prevent HTML injection
	 * issues when rendering content in the browser.
	 * 
	 * @param value The input string to escape.
	 * 
	 * @return The escaped string safe for HTML rendering.
	 */
	private String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	/**
	 * Generates an embedding vector for the given query string by calling the
	 * Ollama embeddings endpoint. Expects a JSON response containing an "embedding"
	 * array of numbers.
	 * 
	 * @param query The input query string to generate an embedding for.
	 * 
	 * @return A list of doubles representing the embedding vector for the input
	 *         query.
	 */
	public List<Double> generateEmbedding(String query) {
		Map<String, Object> payload = Map.of("model", ollamaModel, "prompt", query);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplate.exchange(ollamaEmbeddingsUrl, HttpMethod.POST, entity, String.class);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Could not connect to Ollama embeddings endpoint", ex);
		}

		try {
			JsonNode root = objectMapper.readTree(response.getBody());
			JsonNode embeddingNode = root.path("embedding");
			if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
				throw new IllegalStateException("Ollama returned an empty or missing embedding");
			}
			List<Double> embedding = new ArrayList<>();
			for (JsonNode val : embeddingNode) {
				embedding.add(val.asDouble());
			}
			return embedding;
		} catch (IllegalStateException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalStateException("Could not parse Ollama embeddings response", ex);
		}
	}

	/**
	 * Retrieves a document from the lexical index by filename. Applies first-hit
	 * highlighting to the content based on the query. If the document is not found,
	 * throws an exception.
	 * 
	 * @param filename The filename of the document to retrieve.
	 * @param query    The search query to use for highlighting the content (can be
	 *                 blank or null).
	 * 
	 * @return A DocumentView containing the filename, original content, highlighted
	 *         content, and a flag indicating if a highlight was applied.
	 */
	public DocumentView getDocumentByFilename(String filename, String query) {
		Map<String, Object> payload = Map.of("size", 1, "query", Map.of("term", Map.of("filename", filename)));

		String url = elasticBaseUrl + "/" + lexicalIndex + "/_search";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Could not connect to Elasticsearch", ex);
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(response.getBody());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not parse Elasticsearch response", ex);
		}

		JsonNode hits = root.path("hits").path("hits");
		if (!hits.isArray() || hits.isEmpty()) {
			throw new IllegalStateException("Document not found: " + filename);
		}

		JsonNode source = hits.get(0).path("_source");
		String resolvedFilename = source.path("filename").asText(filename);
		String content = source.path("content").asText("");

		String highlighted = applyFirstHighlight(content, query);
		boolean hasHighlight = highlighted.contains("id=\"firstHit\"");
		return new DocumentView(resolvedFilename, content, highlighted, hasHighlight);
	}

	/**
	 * Performs a semantic search using the given query string. Generates an
	 * embedding for the query, then calls the Elasticsearch kNN search API to find
	 * relevant chunks. Deduplicates results by filename, keeping only the
	 * highest-scoring chunk for each file. Applies pagination to the deduplicated
	 * results and returns a SearchPage containing the search results and total hit
	 * count.
	 * 
	 * @param query The search query string to use for generating the embedding and
	 *              performing the
	 * @param page  The page number for pagination (1-based index).
	 * @param size  The number of results to return per page.
	 * 
	 * @return A SearchPage containing a list of SearchResultItem objects and the
	 *         total
	 */
	public SearchPage search(String query, int page, int size) {
		List<Double> embedding = generateEmbedding(query);

		// Request more candidates than needed so we can deduplicate by filename
		int requestSize = knnK;

		Map<String, Object> knn = Map.of("field", "embedding", "query_vector", embedding, "k", knnK, "num_candidates",
				knnNumCandidates);

		Map<String, Object> payload = Map.of("size", requestSize, "knn", knn, "_source", List.of("filename", "chunk"));

		String url = elasticBaseUrl + "/" + semanticIndex + "/_search";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

		ResponseEntity<String> response;
		try {
			response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		} catch (RestClientException ex) {
			throw new IllegalStateException("Could not connect to Elasticsearch", ex);
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(response.getBody());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not parse Elasticsearch response", ex);
		}

		JsonNode hits = root.path("hits").path("hits");

		// Deduplicate by filename, keeping the first (highest-scoring) chunk
		LinkedHashMap<String, String> uniqueByFilename = new LinkedHashMap<>();
		for (JsonNode hit : hits) {
			JsonNode source = hit.path("_source");
			String filename = source.path("filename").asText("(unknown file)");
			String chunk = source.path("chunk").asText("");
			uniqueByFilename.putIfAbsent(filename, chunk);
		}

		List<Map.Entry<String, String>> allEntries = new ArrayList<>(uniqueByFilename.entrySet());
		long total = allEntries.size();

		// Apply pagination over the deduplicated results
		int from = Math.max(page - 1, 0) * size;
		int to = Math.min(from + size, allEntries.size());

		List<SearchResultItem> items = new ArrayList<>();
		if (from < allEntries.size()) {
			for (Map.Entry<String, String> entry : allEntries.subList(from, to)) {
				String snippet = trimToSnippetLength(entry.getValue());
				items.add(new SearchResultItem(entry.getKey(), snippet));
			}
		}

		return new SearchPage(items, total);
	}

	/**
	 * Trims the given text value to the configured snippet length, adding ellipsis
	 * if it was truncated. Also normalizes whitespace to single spaces and trims
	 * leading/trailing whitespace.
	 * 
	 * @param value The input text value to trim and normalize for display as a
	 *              search result
	 * 
	 * @return A cleaned-up snippet of the input text, suitable for display in
	 *         search results, with a maximum length defined by the snippetLength
	 *         property.
	 */
	private String trimToSnippetLength(String value) {
		String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
		if (normalized.length() <= snippetLength) {
			return normalized;
		}
		return normalized.substring(0, snippetLength) + "...";
	}
}
