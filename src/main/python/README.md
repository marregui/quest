# quest

## Requirements

- [Python 3](https://www.python.org/downloads/)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/download)

## Install

`pip3 install -e .`

To uninstall `pip3 uninstall quest`

To publish to Pypi [read this.](https://gist.github.com/asaah18/5dfda79cbddf9ef6a5b74587dfb9e706#publish-a-package-in-pypi)

### Start/Update QuestDB

*quest* requires a local instance of QuestDB running along, for convenience, you can have *quest* launch it:

- `python3 -m quest start`: starts QuestDB from the local build.
- `python3 -m quest update`: clones QuestDB (or pulls master) and builds it locally.

The running instance's working directory can be found in folder `.quest` under the user's home folder, with structure:

- **clone**: contains QuestDB's git clone, the local build, and the logs file.
- **ROOT/conf**: contains QuestDB's configuration files (default files are created on first start).
- **ROOT/db**: contains QuestDB's database files. Each table will have a matching folder (same name) in here.

```shell
<user home>/.quest/ROOT 
<user home>/.quest/ROOT/conf/server.conf
<user home>/.quest/ROOT/conf/log.conf
<user home>/.quest/ROOT/conf/date.formats
<user home>/.quest/ROOT/conf/mime.types
<user home>/.quest/ROOT/db 
<user home>/.quest/clone
<user home>/.quest/clone/questdb.log 
```
