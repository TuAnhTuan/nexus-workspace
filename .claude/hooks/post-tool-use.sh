#!/usr/bin/env bash
# PostToolUse hook: logs which tool ran and on what file.
mkdir -p "$(dirname "$0")/../logs"
log_file="$(dirname "$0")/../logs/tool-use.log"

input=$(cat)
tool=$(echo "$input" | jq -r '.tool_name // "unknown"')
target=$(echo "$input" | jq -r '.tool_input.file_path // .tool_input.command // empty')

echo "$(date '+%Y-%m-%d %H:%M:%S') | $tool | $target" >> "$log_file"
exit 0