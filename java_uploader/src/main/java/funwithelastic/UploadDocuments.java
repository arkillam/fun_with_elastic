package funwithelastic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

/**
 * Uploads text files from the documents folder to elastic. Reads all .txt files
 * from the ../documents directory and sends each file as a JSON document to the
 * elastic endpoint localhost:9200/text_files/_doc.
 */

public class UploadDocuments {

    private static final String documentsPath = "documents";

    private static final String elasticUrl = "http://localhost:9200/lex_text_files/_doc";

    public static void main(String[] args) {

        try {

            HttpClient client = HttpClient.newHttpClient();
            File documentsDir = new File(documentsPath);

            if (!documentsDir.exists() || !documentsDir.isDirectory()) {
                System.err.println("Documents directory not found: " + documentsPath);
                return;
            }

            File[] files = documentsDir.listFiles((dir, name) -> name.endsWith(".txt"));

            if (files == null || files.length == 0) {
                System.out.println("No txt files found in documents folder");
                return;
            }

            for (File file : files) {
                String filename = file.getName();
                String content = new String(Files.readAllBytes(file.toPath()));

                // Create JSON object with filename and content
                // JSONObject automatically escapes special characters
                JSONObject jsonDoc = new JSONObject();
                jsonDoc.put("filename", filename);
                jsonDoc.put("content", content);

                // Create and send HTTP POST request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(elasticUrl))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonDoc.toString()))
                        .header("Content-Type", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Uploaded: " + filename);
                System.out.println("Status Code: " + response.statusCode());

                if (response.statusCode() >= 400) {
                    System.out.println("Error Response: " + response.body());
                }
            }

            System.out.println("Upload complete!");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
