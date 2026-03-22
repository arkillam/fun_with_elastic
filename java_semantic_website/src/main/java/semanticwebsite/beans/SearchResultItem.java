package semanticwebsite.beans;

/**
 * Represents a single search result item containing the document filename
 * and a snippet of the matched chunk.
 */

public class SearchResultItem {

	private final String filename;

	private final String snippet;

	public SearchResultItem(String filename, String snippet) {
		this.filename = filename;
		this.snippet = snippet;
	}

	public String getFilename() {
		return filename;
	}

	public String getSnippet() {
		return snippet;
	}
}
