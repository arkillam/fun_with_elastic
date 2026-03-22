package semanticwebsite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import semanticwebsite.beans.DocumentView;
import semanticwebsite.beans.SearchPage;
import semanticwebsite.services.ElasticsearchService;

import java.nio.charset.StandardCharsets;

/**
 * Controller for handling web requests related to semantic searching and
 * viewing documents.
 */

@Controller
public class PageController {

	private final int pageSize;

	private final int maxVisiblePages;

	private final ElasticsearchService elasticsearchService;

	public PageController(
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
			model.addAttribute("error",
					"Could not load results. Verify Elasticsearch and Ollama are running.");
		}

		return "results";
	}

	@GetMapping("/document")
	public String document(
			@RequestParam(name = "filename") String filename,
			@RequestParam(name = "q", required = false, defaultValue = "") String query,
			Model model) {

		String normalizedQuery = query == null ? "" : query.trim();
		model.addAttribute("query", normalizedQuery);
		model.addAttribute("encodedQuery", UriUtils.encode(normalizedQuery, StandardCharsets.UTF_8));

		try {
			DocumentView doc = elasticsearchService.getDocumentByFilename(filename, normalizedQuery);
			model.addAttribute("document", doc);
		} catch (Exception ex) {
			model.addAttribute("error",
					"Could not load the selected document from Elasticsearch.");
		}

		return "document";
	}
}

