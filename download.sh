#!/bin/bash

CACHE_PATH="$HOME/.cache/pun"
URL="https://kaikki.org/dictionary/raw-wiktextract-data.jsonl.gz"

# Create cache directory if it doesn't exist
mkdir -p "$CACHE_PATH"

# Download the file
wget -Nc -P "$CACHE_PATH" "$URL"