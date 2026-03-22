
I am working on a project to build two small search engines:  a lexical one and a semantic one.  I am using all free software and running it all locally on my desktop.  My desktop is an AMD 5600GT with 64 GB of RAM, Windows 11, and no meaningful GPU.

A word on storage:  I have a SATA SDD c: drive that I try to keep clear, a d: SATA HDD drive for large storage, and an m: M.2 SDD drive that I use for high performance storage.  You will see in my wsl setup instructions that I shifted the Ubuntu image from c: to m:, and keep incremental backups of my work on d:.

I know that Elasticsearch is called that, but I am a lazy/efficient typist, and so calling it "elastic" throughout.

I am using WSL to run Ubuntu Linux under Windows 11.  It works great.  I am not putting in the effort to sort out networking issues right now, so from Windows 11 I am accessing the servers running under Linux using http://localhost instead of an IP address, which works well for my purposes.

I have no worries about someone hacking into my computer and stealing the public domain Sherlock Holmes stories I am using as content, so to make life easier I am disabling security features as I go.  This project may be of value to someone learning how to use WSL, Elastic, Ollama, small LLMs (SLMs), and lexical and semantic search techniques, but please do not take any queues from this project on how to securely configure anything.  Security is important, but not for this project.

I needed some content to search, and selected Sherlock Holmes short stories that are in the public domain.  I wanted to see how Elastic and the two search types would handle larger documents, so I also uploaded my favourite full-length novels The Hound of the Baskervilles and The Sign of the Four.

Setup instructions are listed in the numbered files.

After I finished the setup, I wrote the "java_uploader" Java project and its classes.  There are classes to delete indices, list indices, create indices (lexical and semantic), upload documents to the indices, and search for documents.  I use SHA-256 hashes to generate ids for uploads so that re-uploading will replace instead of duplicate content (exception: modified content will result in extra documents).

The "java_lexical_website" folder contains a Java Spring Boot application that serves up a search website.  Users can search the documents for text, see results with highlighted matches, and click on them to read the full stories.

The "java_semantic_website" folder contains a Java Spring Boot application that serves up a search website.  Users can search the documents for text, see results with highlighted matches, and click on them to read the full stories.  Searches are semantic and return matched chunks.  Clicking on a match uses the lexical search to pull up the complete story.  Simple searches, usually one word, still get highlihghted in the final result, but more nuanced searches (e.g. "stores involving blackmail") do not.

Some useful links:
 - http://localhost:9200/_aliases (list of elastic indices)
 - http://localhost:9200/lex_text_files/_search?q=scandal (simple search)
 - http://localhost:5601/app/home#/ (Kibana)
 - http://localhost:8080/ (whichever Java Spring Boot web site you are running)

Future:
 - agentic setup that offers lexical and semantic search functionality
 - chat that uses said agent