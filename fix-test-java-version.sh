#!/bin/bash

echo "===== Java Version Fix for Tests ====="
echo "This script will help configure the right Java version for running tests"
echo

echo "Checking current Java version..."
java -version

echo
echo "Checking available Java installations..."
which java
echo

read -p "Enter the path to your JDK 21 installation (e.g., /usr/lib/jvm/java-21-openjdk): " JAVA_HOME_PATH

if [ ! -f "$JAVA_HOME_PATH/bin/java" ]; then
    echo "Error: Java executable not found at $JAVA_HOME_PATH/bin/java"
    echo "Please verify the path and try again."
    exit 1
fi

echo
echo "Setting JAVA_HOME to $JAVA_HOME_PATH..."
export JAVA_HOME=$JAVA_HOME_PATH
export PATH=$JAVA_HOME/bin:$PATH

echo
echo "Verifying new Java version..."
java -version

echo
echo "Cleaning and recompiling the project with the new Java version..."
./mvnw clean compile test-compile

echo
echo "Setup complete!"
echo "You can now run tests with JDK 21. Example:"
echo "./mvnw test -Dtest=ChatGatWayInternalTest#testChatGatWayInternal_WithXUserIdHeader_Integration"
echo
echo "Note: This Java version change is only active in the current terminal session."
echo "To make it permanent, add these lines to your ~/.bashrc or ~/.zshrc:"
echo "  export JAVA_HOME=$JAVA_HOME_PATH"
echo "  export PATH=\$JAVA_HOME/bin:\$PATH"

read -p "Press Enter to continue..." 