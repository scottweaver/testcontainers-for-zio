#!/usr/bin/env bash
#
# clean.sh — remove Scala / sbt build output and tooling caches from this repo.
#
# Walks the whole tree (all sub-projects under modules/, docs-project/, and
# every nested meta-build level under project/, project/project/, ...) and
# removes:
#
#   directories : target/  .bloop/  .bsp/  .metals/  .scala-build/
#                 lib_managed/  src_managed/  .history/  .cache/
#   files       : metals.sbt (Metals-generated)  *.class  *.log
#                 *.tmp  *.bak  *.swp  .DS_Store
#
# Skipped on purpose: .git/, website/node_modules/, anything under a src/ dir.
#
# Usage:
#   ./clean.sh            delete build output
#   ./clean.sh -n         dry run — list what would be deleted
#   ./clean.sh --ide      also delete .idea/ and .vscode/ (editor state)
#   ./clean.sh --website  also delete website/build and website/.docusaurus
#   ./clean.sh -h         help
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DRY_RUN=0
IDE=0
WEBSITE=0

usage() { sed -n '2,/^set -e/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; }

for arg in "$@"; do
  case "$arg" in
    -n|--dry-run) DRY_RUN=1 ;;
    --ide)        IDE=1 ;;
    --website)    WEBSITE=1 ;;
    -h|--help)    usage; exit 0 ;;
    *) echo "unknown option: $arg" >&2; usage >&2; exit 2 ;;
  esac
done

cd "$ROOT"

# Directory names that are always build output.
DIRS=(target .bloop .bsp .metals .scala-build lib_managed src_managed .history .cache)
# File globs that are always safe to drop.
FILES=(metals.sbt '*.class' '*.log' '*.tmp' '*.bak' '*.swp' .DS_Store)

(( IDE ))     && DIRS+=(.idea .vscode)
(( WEBSITE )) && DIRS+=(build .docusaurus)

# Build the -name alternation for find.
dir_expr=()
for d in "${DIRS[@]}";  do dir_expr+=(-o -name "$d"); done
file_expr=()
for f in "${FILES[@]}"; do file_expr+=(-o -name "$f"); done

# Collect matches. Pruned subtrees (.git, node_modules, src) are never entered,
# and a matched directory is pruned so we don't list its children too.
TARGETS=()
while IFS= read -r -d '' p; do TARGETS+=("$p"); done < <(
  find . \
    \( -name .git -o -name node_modules -o \( -type d -name src \) \) -prune -o \
    \( -type d \( -false "${dir_expr[@]}" \) -prune -print0 \) -o \
    \( -type f \( -false "${file_expr[@]}" \) -print0 \)
)

if (( ${#TARGETS[@]} == 0 )); then
  echo "Nothing to clean."
  exit 0
fi

if (( DRY_RUN )); then
  echo "Would remove ${#TARGETS[@]} path(s):"
  printf '  %s\n' "${TARGETS[@]}" | sort
  exit 0
fi

printf '%s\0' "${TARGETS[@]}" | xargs -0 rm -rf --
echo "Removed ${#TARGETS[@]} path(s)."
