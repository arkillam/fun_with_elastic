import httpx
from config import (
    ELASTICSEARCH_URL,
    OLLAMA_URL,
    OLLAMA_EMBED_MODEL,
    LEX_INDEX,
    SEM_INDEX,
)

_http = httpx.AsyncClient(timeout=30.0)


async def _get_embedding(text: str) -> list[float]:
    """Generate a 768-dimensional embedding via Ollama nomic-embed-text."""
    resp = await _http.post(
        f"{OLLAMA_URL}/api/embeddings",
        json={"model": OLLAMA_EMBED_MODEL, "prompt": text},
    )
    resp.raise_for_status()
    return resp.json()["embedding"]


async def lexical_search(
    query: str,
    num_results: int = 5,
    return_full_documents: bool = False,
) -> list[dict]:
    """
    Keyword / lexical search against lex_text_files.
    Uses multi-match on filename (boosted) and content with highlighting.
    """
    body: dict = {
        "size": num_results,
        "query": {
            "multi_match": {
                "query": query,
                "fields": ["filename^2", "content"],
            }
        },
        "highlight": {
            "fields": {
                "content": {
                    "fragment_size": 150,
                    "number_of_fragments": 10,
                }
            }
        },
    }
    if not return_full_documents:
        body["_source"] = {"includes": ["filename"]}

    resp = await _http.post(
        f"{ELASTICSEARCH_URL}/{LEX_INDEX}/_search",
        json=body,
    )
    resp.raise_for_status()
    hits = resp.json().get("hits", {}).get("hits", [])

    results = []
    for hit in hits:
        src = hit["_source"]
        entry: dict = {
            "filename": src.get("filename", ""),
            "score": hit.get("_score", 0),
            "highlights": hit.get("highlight", {}).get("content", []),
        }
        if return_full_documents:
            entry["content"] = src.get("content", "")
        results.append(entry)
    return results


async def semantic_search(
    query: str,
    num_results: int = 5,
    return_full_documents: bool = False,
) -> list[dict]:
    """
    Semantic / vector search against sem_text_files.
    Embeds the query via Ollama, then runs kNN on the embedding field.
    """
    query_vector = await _get_embedding(query)

    body = {
        "size": num_results,
        "knn": {
            "field": "embedding",
            "query_vector": query_vector,
            "k": num_results,
            "num_candidates": max(50, num_results * 10),
        },
        "_source": {"includes": ["filename", "chunk_index", "chunk"]},
    }
    resp = await _http.post(
        f"{ELASTICSEARCH_URL}/{SEM_INDEX}/_search",
        json=body,
    )
    resp.raise_for_status()
    hits = resp.json().get("hits", {}).get("hits", [])

    results = []
    seen_filenames: set[str] = set()
    for hit in hits:
        src = hit["_source"]
        entry: dict = {
            "filename": src.get("filename", ""),
            "chunk_index": src.get("chunk_index", 0),
            "chunk": src.get("chunk", ""),
            "score": hit.get("_score", 0),
        }
        seen_filenames.add(entry["filename"])
        results.append(entry)

    if return_full_documents and seen_filenames:
        full_docs = await _get_full_docs_by_filenames(list(seen_filenames))
        for entry in results:
            entry["content"] = full_docs.get(entry["filename"], "")

    return results


async def get_document(filename: str) -> dict:
    """Retrieve the full text of a single document by exact filename."""
    body = {
        "size": 1,
        "query": {"term": {"filename": filename}},
    }
    resp = await _http.post(
        f"{ELASTICSEARCH_URL}/{LEX_INDEX}/_search",
        json=body,
    )
    resp.raise_for_status()
    hits = resp.json().get("hits", {}).get("hits", [])
    if not hits:
        return {"filename": filename, "content": "", "error": "Document not found"}
    src = hits[0]["_source"]
    return {"filename": src.get("filename", ""), "content": src.get("content", "")}


async def list_documents() -> list[str]:
    """Return all unique filenames in the lexical index."""
    body = {
        "size": 0,
        "aggs": {
            "filenames": {
                "terms": {"field": "filename", "size": 1000}
            }
        },
    }
    resp = await _http.post(
        f"{ELASTICSEARCH_URL}/{LEX_INDEX}/_search",
        json=body,
    )
    resp.raise_for_status()
    buckets = (
        resp.json()
        .get("aggregations", {})
        .get("filenames", {})
        .get("buckets", [])
    )
    return [b["key"] for b in buckets]


async def _get_full_docs_by_filenames(filenames: list[str]) -> dict[str, str]:
    """Fetch full content for a list of filenames from the lexical index."""
    body = {
        "size": len(filenames),
        "query": {"terms": {"filename": filenames}},
    }
    resp = await _http.post(
        f"{ELASTICSEARCH_URL}/{LEX_INDEX}/_search",
        json=body,
    )
    resp.raise_for_status()
    hits = resp.json().get("hits", {}).get("hits", [])
    return {
        hit["_source"]["filename"]: hit["_source"].get("content", "")
        for hit in hits
    }
