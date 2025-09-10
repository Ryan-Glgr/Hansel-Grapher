#!/bin/bash

# ----------------------------
# Clean up old debug file
# ----------------------------
rm -f debug

# ----------------------------
# Enter source directory
# ----------------------------
cd src/ || { echo "Failed to enter src/"; exit 1; }

# ----------------------------
# Remove old compiled classes
# ----------------------------
rm -f *.class

# ----------------------------
# Compile Java source files
# ----------------------------
javac *.java || { echo "Compilation failed"; exit 1; }

# ----------------------------
# Run program
# ----------------------------
if [ "$1" = "debug" ]; then
    java Main debug > debug 2>&1
else
    java Main > debug 2>&1
fi

# ----------------------------
# Clean up compiled classes
# ----------------------------
rm -f *.class

# ----------------------------
# Move debug output to parent
# ----------------------------
mv debug ../

# ----------------------------
# Move known PDFs to parent and open them
# ----------------------------
mv *.pdf ../

# ----------------------------
# Return to parent directory
# ----------------------------
cd ../ || exit 1

# ----------------------------
# Display debug output
# ----------------------------
cat debug
open *.pdf
exit 0
