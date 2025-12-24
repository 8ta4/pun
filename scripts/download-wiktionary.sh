#!/bin/bash

DATA_PATH="$DEVENV_ROOT/clj/resources"
URL="https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz"

# Create cache directory if it doesn't exist
mkdir -p "$DATA_PATH"

# Download the file
wget -Nc -P "$DATA_PATH" "$URL"