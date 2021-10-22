# qdbpy

## Requirements

- [Python 3.9](https://www.python.org/downloads/release/python-390/)
- [Java Azul](https://www.azul.com/downloads/?package=jdk)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/download)

## Install

```shell
pip3 install -r requirements.txt
pip3 install -e . 
```

To uninstall `pip3 uninstall qdbpy`

To publish to Pypi [read this.](https://gist.github.com/asaah18/5dfda79cbddf9ef6a5b74587dfb9e706#publish-a-package-in-pypi)

### Start/Update QuestDB

*qdbpy* requires a local instance of QuestDB running along, for convenience, you can have *qdbpy* launch it:

- `python3 -m qdbpy start`: starts QuestDB from the local build.
- `python3 -m qdbpy update`: clones QuestDB (or pulls master) and builds it locally (called by `start` as needed).

The running instance's working directory can be found in folder `.qdbpy` under the user's home folder, with structure:

- **clone**: contains QuestDB's git clone, the local build, and the logs file.
- **ROOT/conf**: contains QuestDB's configuration files (default files are created on first start).
- **ROOT/db**: contains QuestDB's database files. Each table will have a matching folder (same name) in here.

```shell
<user home>/.qdbpy/ROOT 
<user home>/.qdbpy/ROOT/conf/server.conf
<user home>/.qdbpy/ROOT/conf/log.conf
<user home>/.qdbpy/ROOT/conf/date.formats
<user home>/.qdbpy/ROOT/conf/mime.types
<user home>/.qdbpy/ROOT/db 
<user home>/.qdbpy/clone
<user home>/.qdbpy/clone/questdb.log 
```
