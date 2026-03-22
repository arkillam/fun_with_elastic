package semanticwebsite.beans;

import java.util.List;

/**
 * Represents a page of search results, including the list of results and the
 * total number of hits.
 */

public class SearchPage {

	private final List<SearchResultItem> results;

	private final long totalHits;

	public SearchPage(List<SearchResultItem> results, long totalHits) {
		this.results = results;
		this.totalHits = totalHits;
	}

	public List<SearchResultItem> getResults() {
		return results;
	}

	public long getTotalHits() {
		return totalHits;
	}
}
