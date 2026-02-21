#!/bin/bash
set -e

CX_ROOT="/Applications/CrossOver.app/Contents/SharedSupport/CrossOver"
BOTTLES_DIR="$HOME/Library/Application Support/CrossOver/Bottles"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CLIENT_DIR="$PROJECT_DIR/client"
BOTTLE_NAME="Renegade"

echo "Setting up C&C Renegade CrossOver bottle..."

# Remove stale symlink if present
if [ -L "$BOTTLES_DIR/$BOTTLE_NAME" ]; then
    echo "Removing stale symlink..."
    rm "$BOTTLES_DIR/$BOTTLE_NAME"
fi

# Create CrossOver Bottles directory
mkdir -p "$BOTTLES_DIR"

# Let cxbottle create the bottle normally in CrossOver's own directory
echo "Creating bottle (this may take a minute)..."
CX_ROOT="$CX_ROOT" "$CX_ROOT/bin/cxbottle" --bottle "$BOTTLE_NAME" --create --template winxp

# Move the created bottle to ./client/
echo "Moving bottle to ./client/ ..."
rm -rf "$CLIENT_DIR"
mv "$BOTTLES_DIR/$BOTTLE_NAME" "$CLIENT_DIR"

# Symlink back so CrossOver can still find it
ln -s "$CLIENT_DIR" "$BOTTLES_DIR/$BOTTLE_NAME"
echo "Symlinked: $BOTTLES_DIR/$BOTTLE_NAME -> $CLIENT_DIR"

echo ""
echo "Done! Bottle is at ./client/, symlinked into CrossOver."
echo "Launch with: open -a CrossOver"
