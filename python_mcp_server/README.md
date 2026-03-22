# Text File Search MCP Server

An MCP (Model Context Protocol) server that exposes lexical and semantic search
over text files stored in Elasticsearch.

## Prerequisites

| Dependency | Version | Purpose |
|------------|---------|---------|
| Python | ≥ 3.10 | Runtime |
| Elasticsearch | 8.x | Search backend (running on `localhost:9200`) |
| Ollama | any | Embedding generation (`nomic-embed-text` model) |

Make sure the Elasticsearch indices (`lex_text_files` and `sem_text_files`) are
already populated using the `java_uploader` project.

Ollama must have `nomic-embed-text` pulled:

```bash
ollama pull nomic-embed-text
```

## Install

```bash
cd python_mcp_server
pip install -e .
```

## Run

```bash
python server.py
```

The server starts on **http://0.0.0.0:3001** using SSE transport by default.

## Configuration (environment variables)

| Variable | Default | Description |
|----------|---------|-------------|
| `ELASTICSEARCH_URL` | `http://localhost:9200` | Elasticsearch base URL |
| `OLLAMA_URL` | `http://localhost:11434` | Ollama API base URL |
| `OLLAMA_EMBED_MODEL` | `nomic-embed-text` | Embedding model name |
| `LEX_INDEX` | `lex_text_files` | Lexical search index |
| `SEM_INDEX` | `sem_text_files` | Semantic search index |
| `MCP_HOST` | `0.0.0.0` | SSE server bind address |
| `MCP_PORT` | `3001` | SSE server port |

## MCP Tools

### `lexical_search`
Keyword search using BM25 scoring on filenames and content.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `query` | string | *(required)* | Search terms |
| `num_results` | int | 5 | Number of results |
| `return_full_documents` | bool | false | Include complete file text |

### `semantic_search`
Vector similarity search using cosine distance on pre-indexed chunks.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `query` | string | *(required)* | Natural language query |
| `num_results` | int | 5 | Number of chunk results |
| `return_full_documents` | bool | false | Include complete file text |

### `get_full_document`
Retrieve complete text of a file by filename.

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `filename` | string | *(required)* | Exact filename (e.g. `A SCANDAL IN BOHEMIA.txt`) |

### `list_documents`
List all available text file filenames. No parameters.

## How It Works

1. On startup, `FastMCP` registers 4 tools and begins serving over SSE on the configured host/port
2. When a client calls `lexical_search`, the server sends a multi-match query (BM25) to the `lex_text_files` Elasticsearch index, returning highlighted snippets or full documents
3. When a client calls `semantic_search`, the server first embeds the query text via Ollama's `nomic-embed-text` model, then runs a kNN vector search against the `sem_text_files` index
4. `get_full_document` retrieves the complete content of a single file from the lexical index by exact filename
5. `list_documents` returns all unique filenames via an Elasticsearch terms aggregation
6. All Elasticsearch and Ollama communication uses async HTTP (`httpx`) — no authentication required

## MCP Client Configuration

For VS Code / Claude Desktop, add to your MCP settings:

```json
{
  "mcpServers": {
    "text-file-search": {
      "url": "http://localhost:3001/sse"
    }
  }
}
```
