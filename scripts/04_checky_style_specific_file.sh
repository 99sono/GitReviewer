
# We can check a specific file with a command like this
# ~/dev/mount/GitReviewer/java-parser(feature/99sono_maven_spotless_from_google_to_eclipse_formatter)$ mvn checkstyle:check -Dincludes=java-parser/src/test/java/com/sono99/javaparser/api/v1/service/ReadmeExamplesTest.java

mvn checkstyle:check -Dincludes=%1