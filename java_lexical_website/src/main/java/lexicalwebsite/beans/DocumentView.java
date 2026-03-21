package lexicalwebsite.beans;

/**
 * A simple view model for representing a document in the application.
 */

public class DocumentView {

	private final String id;

	private final String filename;

	private final String content;

	private final String highlightedContent;

	private final boolean hasHighlight;

	public DocumentView(String id, String filename, String content, String highlightedContent, boolean hasHighlight) {
		this.id = id;
		this.filename = filename;
		this.content = content;
		this.highlightedContent = highlightedContent;
		this.hasHighlight = hasHighlight;
	}

	public String getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public String getContent() {
		return content;
	}

	public String getHighlightedContent() {
		return highlightedContent;
	}

	public boolean isHasHighlight() {
		return hasHighlight;
	}
}