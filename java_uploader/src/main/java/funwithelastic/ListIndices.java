package funwithelastic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

/**
 * Queries elastic at localhost:9200 to retrieve and display the list of
 * available indices.
 * 
 * Uses the _aliases endpoint to fetch all index metadata and displays each
 * index name along with its aliases.
 * 
 * Only lists indices that do not start with a dot (.) to exclude system indices.
 */
public class ListIndices {

    private static final String elasticsearchUrl = "http://localhost:9200/_aliases";

    public static void main(String[] args) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(elasticsearchUrl))
                    .GET()
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status Code: " + response.statusCode());

            if (response.statusCode() == 200) {
                JSONObject indices = new JSONObject(response.body());

                if (indices.length() == 0) {
                    System.out.println("No indices found");
                } else {
                    System.out.println("\nIndices in Elasticsearch:");
                    System.out.println("=".repeat(50));

                    int count = 0;
                    for (String indexName : indices.keySet()) {
                        // Skip system indices that start with a dot
                        if (indexName.startsWith(".")) {
                            continue;
                        }
                        
                        JSONObject indexData = indices.getJSONObject(indexName);
                        JSONObject aliases = indexData.optJSONObject("aliases");

                        System.out.println("\nIndex: " + indexName);

                        if (aliases != null && aliases.length() > 0) {
                            System.out.println("  Aliases: " + aliases.keySet());
                        } else {
                            System.out.println("  Aliases: none");
                        }
                        count++;
                    }

                    System.out.println("\n" + "=".repeat(50));
                    System.out.println("Total indices: " + count);
                }
            } else {
                System.out.println("Error Response: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
