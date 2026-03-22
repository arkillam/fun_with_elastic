package funwithelastic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Deletes an index from elastic at localhost:9200.
 */

public class DeleteIndex {

    private static final String INDEX_NAME = "lex_text_files";

    private static final String ELASTICSEARCH_URL = "http://localhost:9200";

    public static void main(String[] args) {
        String deleteUrl = ELASTICSEARCH_URL + "/" + INDEX_NAME;

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .DELETE()
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response: " + response.body());

            if (response.statusCode() == 200) {
                System.out.println("\nIndex '" + INDEX_NAME + "' deleted successfully!");
            } else if (response.statusCode() == 404) {
                System.out.println("\nIndex '" + INDEX_NAME + "' not found.");
            } else {
                System.out.println("\nError deleting index: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
