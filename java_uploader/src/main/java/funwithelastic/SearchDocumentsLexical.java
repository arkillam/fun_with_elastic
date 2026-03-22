package funwithelastic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Searches Elasticsearch for documents by a configured query string.
 * 
 * Awkwardly named to differentiate it from the similar class that searches the
 * semantic index.
 */
public class SearchDocumentsLexical {

    private static final String ELASTIC_SEARCH_URL = "http://localhost:9200/lex_text_files/_search";

    private static final String SEARCH_TEXT = "scandal";

    private static final int RESULT_SIZE = 5;

    private static final int HIGHLIGHT_FRAGMENT_SIZE = 150;

    private static final int HIGHLIGHT_NUMBER_OF_FRAGMENTS = 10;

    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JSONObject requestBody = new JSONObject();
            requestBody.put("size", RESULT_SIZE);

            JSONObject match = new JSONObject();
            match.put("content", SEARCH_TEXT);

            JSONObject query = new JSONObject();
            query.put("match", match);
            requestBody.put("query", query);

            JSONObject contentHighlight = new JSONObject();
            contentHighlight.put("fragment_size", HIGHLIGHT_FRAGMENT_SIZE);
            contentHighlight.put("number_of_fragments", HIGHLIGHT_NUMBER_OF_FRAGMENTS);

            JSONObject fields = new JSONObject();
            fields.put("content", contentHighlight);

            JSONObject highlight = new JSONObject();
            highlight.put("fields", fields);
            requestBody.put("highlight", highlight);

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
                System.out.println("No results found for query: " + SEARCH_TEXT);
                return;
            }

            System.out.println("Results for query: " + SEARCH_TEXT);
            System.out.println("=".repeat(60));

            for (int i = 0; i < hits.length(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                JSONObject source = hit.optJSONObject("_source");
                JSONObject highlightObj = hit.optJSONObject("highlight");

                String filename = source != null ? source.optString("filename", "(unknown)") : "(unknown)";
                System.out.println("File: " + filename);

                if (highlightObj != null && highlightObj.has("content")) {
                    JSONArray snippets = highlightObj.getJSONArray("content");
                    for (int j = 0; j < snippets.length(); j++) {
                        System.out.println("  Highlight " + (j + 1) + ": " + snippets.getString(j));
                    }
                } else {
                    System.out.println("  No highlight returned.");
                }

                System.out.println("-".repeat(60));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
