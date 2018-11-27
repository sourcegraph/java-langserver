# Java Language Server

Sourcegraph language server for the Java programming language

# Support

|      | Hover | Jump to def | Find references | Workspace symbols | VFS extension | Isolated | Parallel |
|------|-------|-------------|-----------------|-------------------|---------------|----------|----------|
| Java |   x   |      x      |        x        |         x         |       x       |     x    |     x    |


# Development environment

- Manually vendor `tools.jar` from your system Java JDK. This is required because Maven otherwise will not include these dependency classes in the generated `.jar`.
  - Find the `tools.jar` in your JDK system path (e.g., `$JAVA_HOME/lib/tools.jar`).
  - Find the `com.sun:tools` dependency declaration in [`pom.xml`](pom.xml).
  - Follow the guidelines at
    https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html
    to manually vendor your system `tools.jar` at the location
    described in [`pom.xml`](pom.xml).
- You can build from the command line using `mvn clean compile
  assembly:single`. The output is called
  `target/java-language-server.jar`.
- Run the language server with `java -jar target/java-language-server.jar -l INFO`
- During development, it is often easier to build and run from the
  IDE. The preferred IDE is IntelliJ.
