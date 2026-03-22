import os

OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://localhost:11434")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "qwen2.5:3b")

MCP_SERVER_URL = os.environ.get("MCP_SERVER_URL", "http://localhost:3001/sse")
