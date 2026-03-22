"""
MCP server exposing lexical and semantic search over text files
stored in Elasticsearch.  Transport: SSE.
"""

from mcp.server.fastmcp import FastMCP
import elasticsearch_client as es
from config import MCP_HOST, MCP_PORT

mcp = FastMCP("text-file-search", host=MCP_HOST, port=MCP_PORT)

@mcp.tool()
async def lexical_search(
    query: str,
    num_results: int = 5,
    return_full_documents: bool = False,
) -> str:
    """Search text files using keyword / lexical matching.

    Uses BM25 scoring across filenames and file content.
    Returns highlighted text snippets by default.
    Set return_full_documents=True to include the complete file text for each match.

    Args:
        query: Search terms (e.g. "red-headed league", "poison murder").
        num_results: Number of results to return (default 5).
        return_full_documents: If True, include the full document text in each result.
    """
    results = await es.lexical_search(query, num_results, return_full_documents)
    if not results:
        return "No results found."
    return _format_lexical(results, return_full_documents)


@mcp.tool()
async def semantic_search(
    query: str,
    num_results: int = 5,
    return_full_documents: bool = False,
) -> str:
    """Search text files using semantic similarity (vector search).

    The query is converted to an embedding and matched against pre-indexed file
    chunks using cosine similarity.  Returns the most semantically relevant text
    chunks by default.
    Set return_full_documents=True to also include the complete file text.

    Args:
        query: Natural-language query (e.g. "a case involving a mysterious letter").
        num_results: Number of chunk results to return (default 5).
        return_full_documents: If True, include the full document text alongside each chunk.
    """
    results = await es.semantic_search(query, num_results, return_full_documents)
    if not results:
        return "No results found."
    return _format_semantic(results, return_full_documents)


@mcp.tool()
async def get_full_document(filename: str) -> str:
    """Retrieve the complete text of a file by its exact filename.

    Use list_documents to find available filenames first.

    Args:
        filename: Exact filename (e.g. "A SCANDAL IN BOHEMIA.txt").
    """
    doc = await es.get_document(filename)
    if doc.get("error"):
        return f"Error: {doc['error']}"
    return f"=== {doc['filename']} ===\n\n{doc['content']}"


@mcp.tool()
async def list_documents() -> str:
    """List all available text file filenames in the search index.

    Returns a newline-separated list of filenames that can be used with
    get_full_document.
    """
    filenames = await es.list_documents()
    if not filenames:
        return "No documents found in the index."
    return f"Found {len(filenames)} documents:\n\n" + "\n".join(
        sorted(filenames)
    )


# ── formatting helpers ──────────────────────────────────────────────


def _format_lexical(results: list[dict], full: bool) -> str:
    parts: list[str] = []
    for i, r in enumerate(results, 1):
        header = f"[{i}] {r['filename']}  (score: {r['score']:.4f})"
        highlights = "\n".join(f"  ... {h} ..." for h in r.get("highlights", []))
        section = f"{header}\n{highlights}" if highlights else header
        if full and r.get("content"):
            section += f"\n\n--- Full Document ---\n{r['content']}"
        parts.append(section)
    return "\n\n".join(parts)


def _format_semantic(results: list[dict], full: bool) -> str:
    parts: list[str] = []
    for i, r in enumerate(results, 1):
        header = (
            f"[{i}] {r['filename']}  chunk #{r['chunk_index']}"
            f"  (score: {r['score']:.4f})"
        )
        section = f"{header}\n  {r['chunk'][:500]}"
        if full and r.get("content"):
            section += f"\n\n--- Full Document ---\n{r['content']}"
        parts.append(section)
    return "\n\n".join(parts)


if __name__ == "__main__":
    mcp.run(transport="sse")
