package funwithelastic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.json.JSONObject;

/**
 * Uploads text files from the documents folder to elastic. Reads all .txt files
 * from the ../documents directory and sends each file as a JSON document to the
 * elastic endpoint localhost:9200/text_files/_doc.
 * 
 * Generates a SHA-256 hash of the file content to use as the document ID in
 * elastic. This ensures that if you run the uploader multiple times with the
 * same files, it will update the same documents in elastic rather than creating
 * duplicates. If the content of a file changes, it will generate a new document
 * ID and create a new document in elastic.
 * 
 * This class is awkwardly named to differentiate it from the similar class that
 * uploads documents for the semantic index.
 */

public class UploadDocumentsLexical {

    private static final String documentsPath = "documents";

    private static final String elasticUrl = "http://localhost:9200/lex_text_files/_doc";

    private static String calculateSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            StringBuilder hash = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

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
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String content = new String(fileBytes, StandardCharsets.UTF_8);
                String documentId = calculateSha256(fileBytes);

                // Create JSON object with filename and content
                // JSONObject automatically escapes special characters
                JSONObject jsonDoc = new JSONObject();
                jsonDoc.put("filename", filename);
                jsonDoc.put("content", content);

                // Use deterministic SHA-256 as id so re-uploading same content updates same
                // document.
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(elasticUrl + "/" + documentId))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonDoc.toString()))
                        .header("Content-Type", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Uploaded: " + filename);
                System.out.println("Document ID (SHA-256): " + documentId);
                System.out.println("Status Code: " + response.statusCode());
                System.out.println("Response Body: " + response.body());

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
