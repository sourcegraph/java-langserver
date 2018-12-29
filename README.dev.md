> Note: this is WIP

# Development README

## Upgrading tools.jar

The `com.sun:tools` artifact is simply the `tools.jar` from the JDK. To depend on it directly, you
can use the following snippet in the `pom.xml` (the reason we don't due this is that some system JDK
installations do not have a `tools.jar` at the expected location):

```xml
        <!-- Javac compiler tools -->
        <dependency>
            <groupId>com.sun</groupId>
            <artifactId>tools</artifactId>
            <version>1.8</version>
            <scope>system</scope>
            <systemPath>${java.home}/../lib/tools.jar</systemPath>
        </dependency>

```

To update the remote `tools.jar`, do the following:

1. Locate the `tools.jar` in your JDK installation. It is typically at `$JAVA_HOME/lib/tools.jar`.
1. Authorize `mvn` to upload files to the Sourcegraph Bintray. Incorporate the following into `~/.m2/settings.xml`, substituting in your Bintray API key:
   ```xml
   <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
     <servers>
       <server>
         <id>bintray-sourcegraph-java</id>
         <username>beyang</username>
         <password>${BINTRAY_API_KEY}</password>
       </server>
     </servers>
   </settings>
   ```
1. Deploy `tools.jar` to the Bintray Maven repository. Substitute the version and file path into this command:
   ```
   mvn deploy:deploy-file -DgroupId=com.sun -DartifactId=tools -Dversion=${VERSION} -Dpackaging=jar -Dfile=${PATH_TO_TOOLS_JAR} -DrepositoryId=bintray-sourcegraph-java -Durl=https://api.bintray.com/maven/sourcegraph/java/tools/;publish=1
   ```
