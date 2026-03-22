package funwithelastic;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Creates an index in Elastic called lex_text_files with a mapping that has two
 * fields: filename and content. The index is optimized for English content.
 * 
 * The first time you run this, it will create the index. If you run it again,
 * it will return an error.
 * 
 * The created index is meant for documents that will be searched lexically.
 */

public class CreateLexicalIndex {

  public static final String INDEX_NAME = "lex_text_files";

  public static final String ELASTICSEARCH_URL = "http://localhost:9200";

  public static void main(String[] args) {

    // sent to elastic to define the index
    String indexMapping = """
        {
          "settings": {
            "analysis": {
              "analyzer": {
                "default": {
                  "type": "english"
                }
              }
            }
          },
          "mappings": {
            "properties": {
              "filename": { "type": "keyword" },
              "content":  { "type": "text",
                "analyzer": "english"}
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