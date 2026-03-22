"""
Bridge between Ollama tool-calling format and the MCP tool server.

Connects to the MCP SSE endpoint, discovers tools, converts them to
Ollama-compatible tool definitions, and executes tool calls on demand.
"""

import json
from contextlib import AsyncExitStack

from mcp.client.session import ClientSession
from mcp.client.sse import sse_client


class McpBridge:
    """Manages a persistent MCP session and exposes tools in Ollama format."""

    def __init__(self, server_url: str):
        self._server_url = server_url
        self._session: ClientSession | None = None
        self._exit_stack = AsyncExitStack()
        self._tools_cache: list[dict] | None = None

    async def connect(self) -> None:
        """Open SSE transport and initialise the MCP session."""
        read_stream, write_stream = await self._exit_stack.enter_async_context(
            sse_client(self._server_url)
        )
        self._session = await self._exit_stack.enter_async_context(
            ClientSession(read_stream, write_stream)
        )
        await self._session.initialize()

    async def close(self) -> None:
        await self._exit_stack.aclose()

    async def get_ollama_tools(self) -> list[dict]:
        """Return MCP tools converted to Ollama / OpenAI tool-call format."""
        if self._tools_cache is not None:
            return self._tools_cache

        result = await self._session.list_tools()
        ollama_tools = []
        for tool in result.tools:
            ollama_tools.append({
                "type": "function",
                "function": {
                    "name": tool.name,
                    "description": tool.description or "",
                    "parameters": tool.inputSchema,
                },
            })
        self._tools_cache = ollama_tools
        return ollama_tools

    async def call_tool(self, name: str, arguments: dict) -> str:
        """Execute an MCP tool call and return its text result."""
        result = await self._session.call_tool(name, arguments)
        parts = []
        for block in result.content:
            if hasattr(block, "text"):
                parts.append(block.text)
            else:
                parts.append(json.dumps(block.model_dump()))
        return "\n".join(parts)
