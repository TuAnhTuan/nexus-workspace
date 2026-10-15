#!/usr/bin/env bash
# PreToolUse hook: blocks a short list of destructive bash commands.
input=$(cat)
command=$(echo "$input" | jq -r '.tool_input.command // empty')

if echo "$command" | grep -qE 'rm[[:space:]]+(-[a-zA-Z]*r[a-zA-Z]*f|-[a-zA-Z]*f[a-zA-Z]*r)[[:space:]]'; then
  echo '{"decision":"block","reason":"Blocked: rm -rf detected. Confirm with the user before deleting recursively."}'
  exit 0
fi

if echo "$command" | grep -qE 'git[[:space:]]+push[[:space:]].*--force'; then
  echo '{"decision":"block","reason":"Blocked: git push --force detected. Confirm with the user before force-pushing."}'
  exit 0
fi

if echo "$command" | grep -qE 'git[[:space:]]+reset[[:space:]]+--hard'; then
  echo '{"decision":"block","reason":"Blocked: git reset --hard detected. Confirm with the user before discarding changes."}'
  exit 0
fi

exit 0