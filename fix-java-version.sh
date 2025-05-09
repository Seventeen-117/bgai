#!/bin/bash

# This script extracts a Maven project's pom.xml, modifies all Java 21 references to Java 17,
# and builds the project using Java 17.

echo "=== Java Version Fix Script ==="
echo "Checking current Java installation..."

if command -v java &>/dev/null; then
    java -version
else
    echo "WARNING: Java not found!"
fi

echo -e "\n=== Maven Version Check ==="
if command -v mvn &>/dev/null; then
    mvn --version
else
    echo "WARNING: Maven not found! Will use ./mvnw if available."
    if [ -f "./mvnw" ]; then
        chmod +x ./mvnw
        ./mvnw --version
    else
        echo "ERROR: Neither Maven nor mvnw wrapper found!"
        exit 1
    fi
fi

echo -e "\n=== Backing up original pom.xml ==="
if [ -f "pom.xml" ]; then
    cp pom.xml pom.xml.original
    echo "Original pom.xml backed up to pom.xml.original"
else
    echo "ERROR: pom.xml not found in current directory!"
    exit 1
fi

echo -e "\n=== Modifying pom.xml to use Java 17 ==="
# Replace Java version
sed -i 's/<java.version>21<\/java.version>/<java.version>17<\/java.version>/g' pom.xml
# Replace compiler source/target versions in maven-compiler-plugin
sed -i 's/<source>21<\/source>/<source>17<\/source>/g' pom.xml
sed -i 's/<target>21<\/target>/<target>17<\/target>/g' pom.xml

echo -e "\n=== Verifying changes ==="
# Check for remaining Java 21 references
if grep -q "<java.version>21</java.version>" pom.xml || grep -q "<source>21</source>" pom.xml || grep -q "<target>21</target>" pom.xml; then
    echo "WARNING: Some Java 21 references remain in pom.xml! Manual inspection required."
    grep -n "java.version\|source\|target" pom.xml
else
    echo "All Java 21 references successfully changed to Java 17."
    grep -n "java.version\|source\|target" pom.xml
fi

echo -e "\n=== Building project ==="
# Use mvnw if available, otherwise use maven
if [ -f "./mvnw" ]; then
    echo "Building with mvnw..."
    ./mvnw clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Djava.version=17
else
    echo "Building with mvn..."
    mvn clean package -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 -Djava.version=17
fi

if [ $? -eq 0 ]; then
    echo -e "\n=== Build SUCCESS ==="
    echo "The project was successfully built with Java 17."
    
    if [ -d "./target" ]; then
        echo "JAR files in target directory:"
        ls -la ./target/*.jar
    fi
    
    echo -e "\nYou can now build the Docker image with:"
    echo "docker build -t bgai:latest ."
else
    echo -e "\n=== Build FAILED ==="
    echo "The project build failed. Please check the errors above."
    echo "You may need to manually modify other Java 21 references in the project."
fi 