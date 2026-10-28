# Sharing Isla so anyone can play

> Not run in the build sandbox (it cannot reach Maven Central), so try these on your machine and send me any error.

## 1. One runnable jar (needs Java 21 on the player's computer)

Add this inside `<build><plugins>` in `pom.xml`:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.3</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <finalName>isla</finalName>
        <createDependencyReducedPom>false</createDependencyReducedPom>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>isla.Launcher</mainClass>
          </transformer>
          <!-- keeps the H2 and MySQL JDBC drivers registered after merging the jars -->
          <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
        </transformers>
        <filters>
          <filter>
            <artifact>*:*</artifact>
            <excludes>
              <exclude>META-INF/*.SF</exclude>
              <exclude>META-INF/*.DSA</exclude>
              <exclude>META-INF/*.RSA</exclude>
            </excludes>
          </filter>
        </filters>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Build and run:

```bash
mvn clean package -DskipTests
java -jar target/isla.jar
```

Why `isla.Launcher`? JavaFX will not start from a plain jar whose main class extends `Application`; `Launcher` is a tiny class that starts the real app.
The JavaFX libraries inside the jar are for the operating system you build on, so build the jar on Windows for Windows players, on macOS for Macs, and so on.

## 2. A double-click app with Java bundled (nothing to install)

After step 1, on the same operating system:

```bash
# Windows (PowerShell) or Linux
jpackage --type app-image --name Isla --input target --main-jar isla.jar --main-class isla.Launcher --dest dist

# macOS
jpackage --type app-image --name Isla --input target --main-jar isla.jar --main-class isla.Launcher --dest dist
```

`dist/Isla` is a folder you can zip and share; players open `Isla.exe` (Windows), `Isla.app` (macOS) or `bin/Isla` (Linux).
(`--type exe`/`msi` need the free WiX tool on Windows, `--type dmg` works on macOS: use those if you want an installer.)

## Where the game keeps its files

Run from the project folder and everything stays there. Launched from anywhere else (for example by double-clicking
the packaged app) it uses a `.isla` folder in the player's home directory: the saved game (`isla-data/`), `settings.properties`
and, if wanted, `db.properties`. Delete that folder to reset everything.
