package funwithelastic;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Creates an index in Elastic called sem_text_files for semantic search.
 * Each document is a file chunk with its filename, chunk text, and embedding.
 * 
 * The first time you run this, it will create the index. If you run it again,
 * it will return an error.
 * 
 * The created index is meant for documents that will be searched semantically.
 */

public class CreateSemanticIndex {

  public static final String INDEX_NAME = "sem_text_files";

  public static final String ELASTICSEARCH_URL = "http://localhost:9200";

  public static void main(String[] args) {

    // Sent to Elasticsearch to define fields for chunk-level semantic documents.
    String indexMapping = """
        {
          "mappings": {
            "properties": {
              "filename": {
                "type": "keyword"
              },
              "chunk_index": {
                "type": "integer"
              },
              "chunk": {
                "type": "text",
                "analyzer": "english"
              },
              "embedding": {
                "type": "dense_vector",
                "dims": 768,
                "index": true,
                "similarity": "cosine"
              }
            }
          }
        }
        """;

    String createIndexUrl = ELASTICSEARCH_URL + "/" + INDEX_NAME;

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(java.net.URI.create(createIndexUrl))
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(indexMapping))
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      System.out.println("Response status code: " + response.statusCode());
      System.out.println("Response body: " + response.body());

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.out.println("Index created.");
    System.exit(0);
  }

}