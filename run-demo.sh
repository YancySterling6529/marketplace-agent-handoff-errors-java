#!/usr/bin/env sh
set -eu
mkdir -p out
javac -d out src/main/java/InfraiErrorsClient.java src/main/java/MarketplaceAgentLoop.java src/main/java/MarketplaceServiceConfiguration.java src/main/java/MarketplaceHandoffDemo.java
java -cp out MarketplaceHandoffDemo
