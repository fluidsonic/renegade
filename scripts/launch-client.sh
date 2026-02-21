#!/bin/bash
export CX_ROOT="/Applications/CrossOver.app/Contents/SharedSupport/CrossOver"
export CX_BOTTLE="Renegade"
SERVER="${1:-127.0.0.1:4849}"

"$CX_ROOT/bin/wine" "C:\\Program Files\\Renegade\\Game.exe" multi +connect "$SERVER"
