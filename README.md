# mygupsql

Pronounced **"/my-goop-see-ku-el/"**, is a desktop user interface to common 
`postgress wire protocol`-compatible databases such as:

- [**cratedb**](https://github.com/crate/crate)
- [**questdb**](https://github.com/questdb/questdb)

## Build commands

- <your system's gradle command> wrapper: regeneates the gradle scaffolding,
  *eg.* `gradle whapper`, so that then you can use the subsequent commands.
- **build**: `./gradlew clean build`

## Run commands (to develop)

- windows: `gradlew.bat run`
- mac/linux: `./gradlew run`

## Installation

After the **build** command completes, you will find a zip file in `build/distributions/`:

- `cd build/distributions`
- `unzip mygupsql-*.zip`
- `cd mygupsql-<version>`
- `bin/mygupsql` (or `bin\mygupsql.bat` in windows)
 