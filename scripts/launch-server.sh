#!/bin/bash
CX_ROOT="/Applications/CrossOver.app/Contents/SharedSupport/CrossOver"
CX_BOTTLE="Renegade"

cd '/Users/marc/Library/Application Support/CrossOver/Bottles/Renegade/drive_c/Program Files/Renegade/RenegadeFDS'
exec \
  "$CX_ROOT/bin/cxstart" --bottle "$CX_BOTTLE" \
  "C:\\Program Files\\Renegade\\RenegadeFDS\\RenegadeServer.exe" "$@"
