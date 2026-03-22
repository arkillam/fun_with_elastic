# Text File Chat

A command-line chat interface using Ollama (`qwen2.5:3b`) with MCP tool access
to search and retrieve text files from Elasticsearch.

## Prerequisites

| Dependency | Purpose |
|------------|---------|
| Python ≥ 3.10 | Runtime |
| Ollama with `qwen2.5:3b` | Chat / reasoning model |
| MCP server running | `python_mcp_server/server.py` on port 3001 |

## Install

```bash
cd python_chat
pip install -e .
```

## Run

1. Start the MCP tool server first:
   ```bash
   cd ../python_mcp_server
   python server.py
   ```

2. In a second terminal, start the chat:
   ```bash
   cd python_chat
   python chat.py
   ```

## Configuration (environment variables)

| Variable | Default | Description |
|----------|---------|-------------|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama API base URL |
| `OLLAMA_MODEL` | `qwen2.5:3b` | Ollama model for chat |
| `MCP_SERVER_URL` | `http://localhost:3001/sse` | MCP server SSE endpoint |

## How It Works

1. `McpBridge` connects to the MCP server via SSE, discovers all 4 tools, and converts their schemas to Ollama's tool-calling format
2. The chat loop sends user messages + tool definitions to Ollama's `/api/chat` endpoint using `qwen2.5:3b`
3. When the model returns `tool_calls`, each call is executed against the MCP server and the result is fed back into the conversation
4. The loop continues until the model produces a final text response (no more tool calls)
5. The model has access **only** to the MCP tools — no other tools or external access

## Usage

```
Connecting to MCP server at http://localhost:3001/sse ...
Connected. Available tools: lexical_search, semantic_search, get_full_document, list_documents
Model: qwen2.5:3b
Type your message (or 'quit' to exit).

You: What documents are available?
  [calling tool: list_documents({})]