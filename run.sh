#!/usr/bin/env sh
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -d out src/*.java || exit 1
java -cp out Main
