package semanticwebsite.beans;

/**
 * A view model for representing a full document retrieved from the lexical
 * index, with optional first-hit highlighting.
 */

public class DocumentView {

	private final String content;

	private final String filename;

	private final boolean hasHighlight;

	private final String highlightedContent;

	public DocumentView(String filename, String content, String highlightedContent, boolean hasHighlight) {
		this.filename = filename;
		this.content = content;
		this.highlightedContent = highlightedContent;
		this.hasHighlight = hasHighlight;
	}

	public String getContent() {
		return content;
	}

	public String getFilename() {
		return filename;
	}

	public String getHighlightedContent() {
		return highlightedContent;
	}

	public boolean isHasHighlight() {
		return hasHighlight;
	}
}
