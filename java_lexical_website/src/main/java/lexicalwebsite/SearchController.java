package lexicalwebsite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import lexicalwebsite.beans.DocumentView;
import lexicalwebsite.beans.SearchPage;
import lexicalwebsite.services.ElasticsearchService;

/**
 * Controller for handling web requests related to searching and viewing documents.
 */

@Controller
public class SearchController {

	private final int pageSize;
	
	private final int maxVisiblePages;
	
	private final ElasticsearchService elasticsearchService;

	public SearchController(
			ElasticsearchService elasticsearchService,
			@Value("${app.search.page-size:10}") int pageSize,
			@Value("${app.search.max-visible-pages:10}") int maxVisiblePages) {
		this.elasticsearchService = elasticsearchService;
		this.pageSize = pageSize;
		this.maxVisiblePages = maxVisiblePages;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/search";
	}

	@GetMapping("/search")
	public String searchPage() {
		return "search";
	}

	@GetMapping("/results")
	public String results(
			@RequestParam(name = "q", required = false, defaultValue = "") String query,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			Model model) {

		String normalizedQuery = query == null ? "" : query.trim();
		int safePage = Math.max(page, 1);

		model.addAttribute("query", normalizedQuery);
		model.addAttribute("currentPage", safePage);
		model.addAttribute("pageSize", pageSize);

		if (normalizedQuery.isBlank()) {
			model.addAttribute("results", java.util.List.of());
			model.addAttribute("totalHits", 0L);
			model.addAttribute("totalPages", 0);
			model.addAttribute("startPage", 1);
			model.addAttribute("endPage", 0);
			model.addAttribute("error", "Enter a search term to see results.");
			return "results";
		}

		try {
			SearchPage searchPage = elasticsearchService.search(normalizedQuery, safePage, pageSize);
			long totalHits = searchPage.getTotalHits();
			int totalPages = (int) Math.ceil((double) totalHits / pageSize);

			int startPage = Math.max(1, safePage - (maxVisiblePages / 2));
			int endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
			if ((endPage - startPage + 1) < maxVisiblePages) {
				startPage = Math.max(1, endPage - maxVisiblePages + 1);
			}

			model.addAttribute("results", searchPage.getResults());
			model.addAttribute("totalHits", totalHits);
			model.addAttribute("totalPages", totalPages);
			model.addAttribute("startPage", startPage);
			model.addAttribute("endPage", endPage);
		} catch (Exception ex) {
			model.addAttribute("results", java.util.List.of());
			model.addAttribute("totalHits", 0L);
			model.addAttribute("totalPages", 0);
			model.addAttribute("startPage", 1);
			model.addAttribute("endPage", 0);
			model.addAttribute("error", "Could not load results from Elasticsearch. Verify it is running at http://localhost:9200.");
		}

		return "results";
	}

	@GetMapping("/document/{id}")
	public String document(
			@PathVariable("id") String id,
			@RequestParam(name = "q", required = false, defaultValue = "") String query,
			Model model) {
		model.addAttribute("query", query == null ? "" : query.trim());

		try {
			DocumentView doc = elasticsearchService.getDocumentById(id, query);
			model.addAttribute("document", doc);
		} catch (Exception ex) {
			model.addAttribute("error", "Could not load the selected document from Elasticsearch.");
		}

		return "document";
	}
}