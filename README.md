# koreader-sync-server

A rewrite of [koreader-sync-server](https://github.com/koreader/koreader-sync-server)
in Kotlin using Ktor and Exposed.

By using Exposed, it adds support for a few different DBMS like MySQL, PostgreSQL and SQLite.

It uses SQLite by default.

### Building
```bash
./gradlew buildFatJar
```
This generates the file `build/libs/koreader-sync-ktor-all.jar`.

### Running
```bash
java -jar koreader-sync-ktor-all.jar
```

### Environment variables
```bash
PORT=8080 # The port to listen to http calls

# The URL to the database
DB_URL="jdbc:sqlite:koreader.db" # SQLite example. This is the default if not specified
DB_URL="jdbc:mariadb://localhost:3306/koreader-sync" # MariaDB Example

# The fully qualified name of the JDBC Driver. If not declared, SQLite driver is used by default.
DB_DRIVER="org.mariadb.jdbc.Driver" # Example for mariadb

DB_USER="koreader" # The database user, not needed when using SQLite
DB_PASSWORD="readerko" # The password for the database user, not needed when using SQLite
```

### Adding support for other databases
You need to download the appropiate JDBC driver for the database you are going to use.
Then add it to the classpath when running the server, like this:
```bash
java -cp "<path-to-jdbc-driver-jar>:koreader-sync-ktor-all.jar" es.hgg.koreader.sync.ApplicationKt
```

### Using Docker

To build the docker image just run:
```bash
docker build -t koreader-sync-server .
```

To run the docker image:
```bash
docker run -d --rm koreader-sync-server
```

#### Docker noteworthy paths

- `/app/data`: When using sqlite, the database is stored in this folder by default (unless overriden with the `DB_URL` variable). Mount a volume to this path to persist the database across containers.
- `/app/drivers`: This folder is scanned for extra Jar files. This is where you need to mount the JDBC driver for the database you are going to use. The name of the jar file inside the volume is unimportant.