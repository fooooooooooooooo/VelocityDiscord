#!/bin/bash

./gradlew build

dest="$HOME/minecraft_test_servers/velocity/plugins"

rm -f "$dest/velocity-discord-"*.jar

jars=$(ls ./build/libs)
sorted=$(echo "$jars" | sed '/-/!{s/$/_/}' | sort -V | sed 's/_$//')
latest=$(echo "$sorted" | tail -n 1)

echo "build/libs/$latest -> $dest/$latest"
cp "./build/libs/$latest" "$dest/"
