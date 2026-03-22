
For this project I built two small search engines:  a lexical one and a semantic one.  I am using all free software and running it all locally on my desktop.  My desktop is an AMD 5600GT with 64 GB of RAM, Windows 11, and no meaningful GPU.

A word on storage:  I have a SATA SDD c: drive that I try to keep clear, a d: SATA HDD drive for large storage, and an m: M.2 SDD drive that I use for high performance storage.  You will see in my wsl setup instructions that I shifted the Ubuntu image from c: to m:, and keep incremental backups of my work on d:.

I know that Elasticsearch is called that, but I am a lazy/efficient typist, and so calling it "elastic" throughout.

I am using WSL to run Ubuntu Linux under Windows 11.  It works great.  I am not putting in the effort to sort out networking issues right now, so from Windows 11 I am accessing the servers running under Linux using http://localhost instead of an IP address, which works well for my purposes.

I have no worries about someone hacking into my computer and stealing the public domain Sherlock Holmes stories I am using as content, so to make life easier I am disabling security features as I go.  This project may be of value to someone learning how to use WSL, Elastic, Ollama, small LLMs (SLMs), and lexical and semantic search techniques, but please do not take any queues from this project on how to securely configure anything.  Security is important, but not for this project.

I needed some content to search, and selected Sherlock Holmes short stories that are in the public domain.  I wanted to see how Elastic and the two search types would handle larger documents, so I also uploaded my favourite full-length novels The Hound of the Baskervilles and The Sign of the Four.

Setup instructions are listed in the numbered files.

After I finished the setup, I wrote the "java_uploader" Java project and its classes.  There are classes to delete indices, list indices, create indices (lexical and semantic), upload documents to the indices, and search for documents.  I use SHA-256 hashes to generate ids for uploads so that re-uploading will replace instead of duplicate content (exception: modified content will result in extra documents).

The "java_lexical_website" folder contains a Java Spring Boot application that serves up a search website.  Users can search the documents for text, see results with highlighted matches, and click on them to read the full stories.

The "java_semantic_website" folder contains a Java Spring Boot application that serves up a search website.  Users can search the documents for text, see results with highlighted matches, and click on them to read the full stories.  Searches are semantic and return matched chunks.  Clicking on a match uses the lexical search to pull up the complete story.  Simple searches, usually one word, still get highlihghted in the final result, but more nuanced searches (e.g. "stores involving blackmail") do not.

There are some screen shots in the screen_shots folder.

To run all of this stuff, start wsl running with the Ubuntu image, and then start up either of the Java web projects.  Note that I have them both listening on port 8080, so you cannot run them on the same computer simultaneously.

Some useful links:
 - http://localhost:9200/_aliases (list of elastic indices)
 - http://localhost:9200/lex_text_files/_search?q=scandal (simple search)
 - http://localhost:5601/app/home#/ (Kibana)
 - http://localhost:8080/ (whichever Java Spring Boot web site you are running)

---------------------------------------------------------------------------------------

I thought it would be fun, and easy, to use Copilot to generate a Python MCP server (python_mcp_server) and a command-line chat (python_chat) that use Ollama and the qwen2.5:3b model to provide a chat interface.  It *was* easy, but the result is not great (or fun to use), and so is a work-in-progress waiting for a future weekend to polish up.

The model is only allowed to use the provided tools, and therefore the provided content.  The search systems I built first are responsive and I am happy with them.  By comparison, this chat system is slow, and so far not very good.  It was able to deduce that Dr. Watson is Sherlock Holmes' best friend, but could not tell me how many of the stories involve murder, even when I said to use a lexical search to figure it out.  So I have work to do if I am going to make this MCP server and chat business usable.

I will probably end up deleting this two Python folders and starting over, but they do work if anyone is interested.  To use them, run three things:
a) the wsl setup
b) server.py in python_mcp_server
c) chat.py in python_chat

