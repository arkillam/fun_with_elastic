
I am working on a project to build two small search engines:  a lexical one, and then a semantic one.  I am using all free software (free as in beer), and running it all locally on my desktop.  My desktop is an AMD 5600GT with 64 GB of RAM, Windows 11, and no meaningful GPU.

A word on storage:  I have a SATA SDD c: drive that I try to keep clear, a d: SATA HDD drive for large storage, and an m: M.2 SDD drive that I use for high performance storage.  You will see in my wsl setup instructions that I shifted the Ubuntu image from c: to m:, and keep incremental backups of my work on d:.

I know that Elasticsearch is called that, but I am a lazy/efficient typist, and so claling it "elastic" throughout.

I am using WSL to run Ubuntu Linux under Windows 11.  It works great.  I am not putting in the effort to sort out networking issues right now, so from Windows 11 I am accessing the servers running under Linux using http://localhost instead of an IP address, which works well for my purposes.

I have no worries about someone hacking into my computer and stealing the public domain Sherlock Holmes stories I am using as content, so to make life easier I am disabling security features as I go.  This project may be of value to someone learning how to use WSL, Elastic, Ollama, small LLMs (SLMs), and lexical and semantic search techniques, but please do not take any queues from this project on how to securely configure anything.  Security is important, but not for this project.

I needed some content to search, and selected publicly available Sherlock Holmes short stories that are in the public domain.  I downloaded them from Project Gutgutenberg.

Setup instructions are listed in the numbered files.

After I finished the setup, I wrote the "java_uploader" Java project and its classes to create an index for lexical search, and upload the Sherlock Holmes short stories files in the documents folder.  I made a naming mistake and created an index with a name I did not want, so I then created classes to list indices and one to delete an unwanted one.

Some useful links:
 - http://localhost:5601/app/home#/ (Kibana)