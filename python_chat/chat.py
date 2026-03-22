"""
CLI chat interface: Ollama qwen2.5:3b + MCP tool server.
"""

import asyncio
import json
import sys

import httpx

from config import OLLAMA_URL, OLLAMA_MODEL, MCP_SERVER_URL
from mcp_bridge import McpBridge

SYSTEM_PROMPT = (
    "You are a helpful assistant. You have access to search tools that can "
    "search and retrieve text files from an Elasticsearch index. Use the tools "
    "when the user asks questions that require looking up or reading documents. "
    "Always cite the filename when referencing a document."
)


async def ollama_chat(
    messages: list[dict],
    tools: list[dict],
    http: httpx.AsyncClient,
) -> dict:
    """Send a chat request to Ollama and return the assistant message."""
    body: dict = {
        "model": OLLAMA_MODEL,
        "messages": messages,
        "stream": False,
    }
    if tools:
        body["tools"] = tools

    resp = await http.post(f"{OLLAMA_URL}/api/chat", json=body, timeout=120.0)
    resp.raise_for_status()
    return resp.json()["message"]


async def run() -> None:
    bridge = McpBridge(MCP_SERVER_URL)
    http = httpx.AsyncClient()

    print(f"Connecting to MCP server at {MCP_SERVER_URL} ...")
    try:
        await bridge.connect()
    except Exception as exc:
        print(f"Failed to connect to MCP server: {exc}")
        print("Make sure the MCP server is running (python_mcp_server/server.py).")
        return

    tools = await bridge.get_ollama_tools()
    tool_names = [t["function"]["name"] for t in tools]
    print(f"Connected. Available tools: {', '.join(tool_names)}")
    print(f"Model: {OLLAMA_MODEL}")
    print("Type your message (or 'quit' to exit).\n")

    messages: list[dict] = [{"role": "system", "content": SYSTEM_PROMPT}]

    try:
        while True:
            try:
                user_input = input("You: ").strip()
            except EOFError:
                break
            if not user_input or user_input.lower() in ("quit", "exit"):
                break

            messages.append({"role": "user", "content": user_input})

            # Conversation loop: keep going while the model wants to call tools
            while True:
                assistant_msg = await ollama_chat(messages, tools, http)
                messages.append(assistant_msg)

                tool_calls = assistant_msg.get("tool_calls")
                if not tool_calls:
                    # No tool calls — print the response and break to next input
                    print(f"\nAssistant: {assistant_msg.get('content', '')}\n")
                    break

                # Execute each tool call via MCP
                for tc in tool_calls:
                    fn = tc["function"]
                    name = fn["name"]
                    args = fn.get("arguments", {})
                    print(f"  [calling tool: {name}({json.dumps(args)})]")

                    try:
                        result = await bridge.call_tool(name, args)
                    except Exception as exc:
                        result = f"Tool error: {exc}"

                    messages.append({"role": "tool", "content": result})

    except KeyboardInterrupt:
        print("\n")
    finally:
        await http.aclose()
        await bridge.close()
        print("Goodbye.")


def main() -> None:
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(run())


if __name__ == "__main__":
    main()
