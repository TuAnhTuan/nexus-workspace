#!/usr/bin/env bash
# Stop hook: appends a short session-end line to root MEMORY.md.
memory_file="$(dirname "$0")/../../MEMORY.md"

# Ensure the file ends with a newline before appending, so entries don't run together.
if [ -s "$memory_file" ] && [ "$(tail -c1 "$memory_file")" != "" ]; then
  echo >> "$memory_file"
fi
echo "- $(date '+%Y-%m-%d %H:%M') — session ended" >> "$memory_file"
echo '{"systemMessage":"Session logged to MEMORY.md"}'
exit 0