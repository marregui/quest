# pygupsql

## Requirements

- [Python 3.9](https://www.python.org/downloads/release/python-390/)
- [Java Azul](https://www.azul.com/downloads/?package=jdk)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/download)

## Install

`pip3 install -e .`

To uninstall `pip3 uninstall pygupsql`

To publish to Pypi [read this.](https://gist.github.com/asaah18/5dfda79cbddf9ef6a5b74587dfb9e706#publish-a-package-in-pypi)

### Start/Update QuestDB

*pygupsql* requires a local instance of QuestDB running along, for convenience, you can have *pygupsql* launch it:

- `python3 -m pygupsql start`: starts QuestDB from the local build.
- `python3 -m pygupsql update`: clones QuestDB (or pulls master) and builds it locally (called by `start` as needed).

The running instance's working directory can be found in folder `.pygupsql` under the user's home folder, with structure:

- **clone**: contains QuestDB's git clone, the local build, and the logs file.
- **ROOT/conf**: contains QuestDB's configuration files (default files are created on first start).
- **ROOT/db**: contains QuestDB's database files. Each table will have a matching folder (same name) in here.

```shell
<user home>/.pygupsql/ROOT 
<user home>/.pygupsql/ROOT/conf/server.conf
<user home>/.pygupsql/ROOT/conf/log.conf
<user home>/.pygupsql/ROOT/conf/date.formats
<user home>/.pygupsql/ROOT/conf/mime.types
<user home>/.pygupsql/ROOT/db 
<user home>/.pygupsql/clone
<user home>/.pygupsql/clone/questdb.log 
```
