package funwithelastic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Uploads chunked semantic documents to Elasticsearch.
 *
 * For each .txt file in the documents folder:
 * 1. Splits content into fixed-size chunks by word count.
 * 2. Calls the embedding endpoint to generate a vector for each chunk.
 * 3. Computes SHA-256 hash of each chunk.
 * 4. Uploads filename, chunk, hash, and embedding to Elasticsearch using hash
 * as _id.
 * 
 * This class is awkwardly named to differentiate it from the similar class that
 * uploads documents for the semantic index.
 */

public class UploaDocumentsdSemantic {

    private static final String DOCUMENTS_PATH = "documents";

    private static final String ELASTIC_URL = "http://localhost:9200/sem_text_files/_doc";

    private static final String EMBEDDING_URL = "http://localhost:11434/api/embeddings";

    private static final String EMBEDDING_MODEL = "nomic-embed-text";

    private static final int CHUNK_SIZE_WORDS = 200;

    private static final int MAX_CHUNK_RETRIES = 3;

    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Simple logging method with timestamp and log level.
     * 
     * @param level   The log level (e.g., INFO, ERROR).
     * @param message The log message.
     */
    private static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(LOG_TIME_FORMAT);
        System.out.println("[" + timestamp + "] [" + level + "] " + message);
    }

    /**
     * Counts the number of words in a string. Returns 0 for null or empty strings.
     * 
     * @param text The input text.
     * 
     * @return The number of words in the input text.
     */
    private static int countWords(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }

    /**
     * Splits the input content into chunks based on a specified word count. Handles
     * 
     * @param content        The input text content to be chunked.
     * @param chunkSizeWords The maximum number of words allowed in each chunk.
     * 
     * @return A list of text chunks, where each chunk contains at most
     *         chunkSizeWords words.
     */
    private static List<String> chunkByWordCount(String content, int chunkSizeWords) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }

        String[] words = content.trim().split("\\s+");

        for (int start = 0; start < words.length; start += chunkSizeWords) {
            int end = Math.min(start + chunkSizeWords, words.length);
            String chunk = String.join(" ", Arrays.copyOfRange(words, start, end));
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * Calculates the SHA-256 hash of the given byte array and returns it as a
     * 
     * @param bytes The input byte array for which the SHA-256 hash is to be
     *              calculated.
     * 
     * @return A hexadecimal string representation of the SHA-256 hash of the input
     *         byte array.
     */
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

    /**
     * Generates an embedding vector for the given text chunk by calling the
     * embedding
     * 
     * @param client The HttpClient instance to use for making the embedding
     *               request.
     * @param chunk  The text chunk for which to generate the embedding vector.
     * 
     * @return A JSONArray representing the embedding vector returned by the
     *         embedding endpoint.
     * 
     * @throws IOException          If an I/O error occurs when sending or receiving
     *                              the embedding request.
     * @throws InterruptedException If the embedding request is interrupted while
     *                              waiting for a response.
     */
    private static JSONArray generateEmbedding(HttpClient client, String chunk)
            throws IOException, InterruptedException {
        JSONObject embeddingRequest = new JSONObject();
        embeddingRequest.put("model", EMBEDDING_MODEL);
        embeddingRequest.put("prompt", chunk);

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

    public static void main(String[] args) {

        try {
            log("INFO", "Starting semantic upload process.");
            log("INFO", "Documents path: " + DOCUMENTS_PATH);
            log("INFO", "Elasticsearch endpoint: " + ELASTIC_URL);
            log("INFO", "Embedding endpoint: " + EMBEDDING_URL + " (model=" + EMBEDDING_MODEL + ")");
            log("INFO", "Chunk size (words): " + CHUNK_SIZE_WORDS);
            log("INFO", "Max chunk retries: " + MAX_CHUNK_RETRIES);

            HttpClient client = HttpClient.newHttpClient();
            File documentsDir = new File(DOCUMENTS_PATH);

            if (!documentsDir.exists() || !documentsDir.isDirectory()) {
                log("ERROR", "Documents directory not found: " + DOCUMENTS_PATH);
                return;
            }

            File[] files = documentsDir.listFiles((dir, name) -> name.endsWith(".txt"));

            if (files == null || files.length == 0) {
                log("WARN", "No .txt files found in documents folder.");
                return;
            }

            Arrays.sort(files, Comparator.comparing(File::getName));
            log("INFO", "Found " + files.length + " text files to process.");

            int totalChunksUploaded = 0;
            int totalChunksFailed = 0;

            for (File file : files) {
                String filename = file.getName();
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String content = new String(fileBytes, StandardCharsets.UTF_8);

                int wordCount = countWords(content);
                List<String> chunks = chunkByWordCount(content, CHUNK_SIZE_WORDS);

                log("INFO", "Processing file: " + filename);
                log("INFO", "File size (bytes): " + fileBytes.length + ", words: " + wordCount + ", chunks: "
                        + chunks.size());

                if (chunks.isEmpty()) {
                    log("WARN", "Skipping file with no chunkable content: " + filename);
                    continue;
                }

                for (int i = 0; i < chunks.size(); i++) {
                    int chunkIndex = i + 1;
                    String chunk = chunks.get(i);
                    String chunkHash = calculateSha256(chunk.getBytes(StandardCharsets.UTF_8));

                    String preview = chunk.length() > 80 ? chunk.substring(0, 80) + "..." : chunk;
                    log("DEBUG", "Chunk hash=" + chunkHash + ", preview=\"" + preview + "\"");

                    boolean chunkUploaded = false;

                    for (int attempt = 1; attempt <= MAX_CHUNK_RETRIES; attempt++) {
                        try {
                            log("INFO", "Processing file=" + filename + ", chunk=" + chunkIndex + "/" + chunks.size()
                                    + ", attempt=" + attempt + "/" + MAX_CHUNK_RETRIES);

                            JSONArray embedding = generateEmbedding(client, chunk);
                            log("INFO", "Embedding generated for hash=" + chunkHash + " (dimensions="
                                    + embedding.length() + ")");

                            JSONObject jsonDoc = new JSONObject();
                            jsonDoc.put("filename", filename);
                            jsonDoc.put("chunk_index", chunkIndex);
                            jsonDoc.put("chunk", chunk);
                            jsonDoc.put("hash", chunkHash);
                            jsonDoc.put("embedding", embedding);

                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(ELASTIC_URL + "/" + chunkHash))
                                    .PUT(HttpRequest.BodyPublishers.ofString(jsonDoc.toString()))
                                    .header("Content-Type", "application/json")
                                    .build();

                            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                            if (response.statusCode() >= 400) {
                                throw new IOException("Elasticsearch upload failed with status " + response.statusCode()
                                        + ": " + response.body());
                            }

                            chunkUploaded = true;
                            totalChunksUploaded++;
                            log("INFO", "Uploaded chunk successfully. file=" + filename + ", chunk=" + chunkIndex
                                    + ", hash=" + chunkHash + ", status=" + response.statusCode());
                            break;
                        } catch (Exception chunkEx) {
                            if (attempt < MAX_CHUNK_RETRIES) {
                                log("WARN", "Chunk attempt failed; retrying. file=" + filename + ", chunk=" + chunkIndex
                                        + ", hash=" + chunkHash + ", attempt=" + attempt + ", error="
                                        + chunkEx.getMessage());
                            } else {
                                totalChunksFailed++;
                                log("ERROR",
                                        "Chunk failed after max retries. file=" + filename + ", chunk=" + chunkIndex
                                                + ", hash=" + chunkHash + ", attempts=" + MAX_CHUNK_RETRIES
                                                + ", error=" + chunkEx.getMessage());
                                chunkEx.printStackTrace();
                            }
                        }
                    }

                    if (!chunkUploaded) {
                        log("ERROR", "Giving up on chunk. file=" + filename + ", chunk=" + chunkIndex + ", hash="
                                + chunkHash);
                    }
                }
            }

            log("INFO", "Semantic upload complete.");
            log("INFO", "Total chunks uploaded: " + totalChunksUploaded);
            log("INFO", "Total chunks failed: " + totalChunksFailed);

        } catch (IOException e) {
            log("ERROR", "Fatal error in upload process: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
