package lexicalwebsite.beans;

/**
 * Represents a single search result item containing the document ID, filename,
 * and a snippet of the content.
 */

public class SearchResultItem {

	private final String id;

	private final String filename;

	private final String snippet;

	public SearchResultItem(String id, String filename, String snippet) {
		this.id = id;
		this.filename = filename;
		this.snippet = snippet;
	}

	public String getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public String getSnippet() {
		return snippet;
	}
}