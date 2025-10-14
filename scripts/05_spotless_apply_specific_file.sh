
# From project root, target specific file
# See the source code in:
# https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/AbstractSpotlessMojo.java

mvn spotless:apply -DspotlessFiles=".*SourceContentHelper.java"

