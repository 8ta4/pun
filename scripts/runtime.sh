#!/bin/bash

# Define the data path (uses DEVENV_ROOT if available, otherwise defaults to current directory)
DATA_PATH="${DEVENV_ROOT:-.}/resources"
BASE_URL="https://raw.githubusercontent.com/8ta4/pun-data/4b5a2c1eeb992d2c1b8faea2488768eaac6be9dc"

# Create the resources directory if it doesn't exist
mkdir -p "$DATA_PATH"

echo "Downloading and decompressing data to $DATA_PATH..."

# Download and decompress normalized.edn.gz
wget -Nc -P "$DATA_PATH" "$BASE_URL/normalized.edn.gz"
gzip -d -f "$DATA_PATH/normalized.edn.gz"

# Download and decompress ipa.edn.gz
wget -Nc -P "$DATA_PATH" "$BASE_URL/ipa.edn.gz"
gzip -d -f "$DATA_PATH/ipa.edn.gz"

echo "Done."