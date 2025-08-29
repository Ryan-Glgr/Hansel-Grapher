#!/bin/bash
rm debug
rm *.class
javac *.java

# Check if debug argument is passed
if [ "$1" = "debug" ]; then
    java Main debug *> debug
else
    java Main *> debug
fi

rm *.class
