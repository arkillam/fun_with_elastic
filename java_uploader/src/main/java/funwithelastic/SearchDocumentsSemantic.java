package funwithelastic;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Searches Elasticsearch semantically by creating an embedding for a configured
 * query string and performing a kNN ("k-nearest neighbors") search on the semantic index.
 */
public class SearchDocumentsSemantic {

    private static final String ELASTIC_SEARCH_URL = "http://localhost:9200/sem_text_files/_search";

    private static final String EMBEDDING_URL = "http://localhost:11434/api/embeddings";

    private static final String EMBEDDING_MODEL = "nomic-embed-text";

    private static final String SEARCH_TEXT = "mistaken identity";

    private static final int RESULT_SIZE = 5;

    private static final int NUM_CANDIDATES = 50;

    private static final int CHUNK_PREVIEW_MAX_LENGTH = 220;

    /**
     * Generates an embedding for the given text using the configured embedding model.
     * 
     * @param client The HttpClient to use for making the request.
     * @param text   The text to generate an embedding for.
     * 
     * @return A JSONArray representing the embedding vector.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private static JSONArray generateEmbedding(HttpClient client, String text)
            throws IOException, InterruptedException {
        JSONObject embeddingRequest = new JSONObject();
        embeddingRequest.put("model", EMBEDDING_MODEL);
        embeddingRequest.put("prompt", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMBEDDING_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(embeddingRequest.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException(
                    "Embedding request failed with status " + response.statusCode() + ": " + response.body());
        }

        JSONObject responseJson = new JSONObject(response.body());
        if (!responseJson.has("embedding") || responseJson.isNull("embedding")) {
            throw new IOException("Embedding response missing 'embedding' field: " + response.body());
        }

        return responseJson.getJSONArray("embedding");
    }

    /**
     * Builds the JSON request body for the Elasticsearch kNN search using the provided
     * 
     * @param queryEmbedding The embedding vector for the search query, which will be used in the kNN search.
     * 
     * @return A JSONObject representing the request body for the Elasticsearch search API.
     */
    private static JSONObject buildSearchRequestBody(JSONArray queryEmbedding) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("size", RESULT_SIZE);

        JSONObject knn = new JSONObject();
        knn.put("field", "embedding");
        knn.put("query_vector", queryEmbedding);
        knn.put("k", RESULT_SIZE);
        knn.put("num_candidates", NUM_CANDIDATES);
        requestBody.put("knn", knn);

        JSONArray includes = new JSONArray();
        includes.put("filename");
        includes.put("chunk_index");
        includes.put("chunk");

        JSONObject source = new JSONObject();
        source.put("includes", includes);
        requestBody.put("_source", source);

        return requestBody;
    }

    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JSONArray queryEmbedding = generateEmbedding(client, SEARCH_TEXT);
            JSONObject requestBody = buildSearchRequestBody(queryEmbedding);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTIC_SEARCH_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status Code: " + response.statusCode());

            if (response.statusCode() != 200) {
                System.out.println("Error Response: " + response.body());
                return;
            }

            JSONObject root = new JSONObject(response.body());
            JSONObject hitsObject = root.optJSONObject("hits");
            if (hitsObject == null) {
                System.out.println("No hits object in response.");
                return;
            }

            JSONArray hits = hitsObject.optJSONArray("hits");
            if (hits == null || hits.length() == 0) {
                System.out.println("No semantic results found for query: " + SEARCH_TEXT);
                return;
            }

            System.out.println("Semantic results for query: " + SEARCH_TEXT);
            System.out.println("=".repeat(60));

            for (int i = 0; i < hits.length(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                JSONObject source = hit.optJSONObject("_source");

                String filename = source != null ? source.optString("filename", "(unknown)") : "(unknown)";
                int chunkIndex = source != null ? source.optInt("chunk_index", -1) : -1;
                String chunk = source != null ? source.optString("chunk", "") : "";
                double score = hit.optDouble("_score", 0.0);

                String cleanedChunk = chunk.replaceAll("\\s+", " ").trim();
                String preview = cleanedChunk.length() > CHUNK_PREVIEW_MAX_LENGTH
                        ? cleanedChunk.substring(0, CHUNK_PREVIEW_MAX_LENGTH) + "..."
                        : cleanedChunk;

                System.out.println("File: " + filename);
                System.out.println("Chunk Index: " + chunkIndex);
                System.out.println("Score: " + score);
                System.out.println("Chunk Preview: " + (preview.isEmpty() ? "(empty chunk)" : preview));
                System.out.println("-".repeat(60));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}