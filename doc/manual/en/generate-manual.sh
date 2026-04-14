#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
output_dir="${1:-"$script_dir/../../../target"}"

if [[ "$output_dir" != /* ]]; then
  output_dir="$PWD/$output_dir"
fi

html_output="$output_dir/billetsys-en.html"
pdf_output="$output_dir/billetsys-en.pdf"

require_command() {
  local command_name="$1"

  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

resolve_eisvogel_template() {
  local candidate
  local candidates=()

  if [[ -n "${EISVOGEL_TEMPLATE:-}" ]]; then
    candidates+=("$EISVOGEL_TEMPLATE")
  fi

  candidates+=(
    "$script_dir/templates/eisvogel.latex"
    "$HOME/.local/share/pandoc/templates/eisvogel.latex"
    "$HOME/.pandoc/templates/eisvogel.latex"
    "/usr/share/pandoc/data/templates/eisvogel.latex"
  )

  for candidate in "${candidates[@]}"; do
    if [[ -f "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  cat >&2 <<'EOF'
Unable to locate the Eisvogel template.

Install eisvogel.latex in one of these locations:
  - $HOME/.local/share/pandoc/templates/eisvogel.latex
  - $HOME/.pandoc/templates/eisvogel.latex
  - doc/manual/en/templates/eisvogel.latex

Alternatively, set EISVOGEL_TEMPLATE to the full path of the template.
EOF
  exit 1
}

require_command pandoc
require_command xelatex
require_command kpsewhich

mapfile -t markdown_files < <(find "$script_dir" -maxdepth 1 -type f -name '*.md' | sort)

if [[ "${#markdown_files[@]}" -eq 0 ]]; then
  echo "No markdown sources found in $script_dir" >&2
  exit 1
fi

eisvogel_template="$(resolve_eisvogel_template)"

required_tex_files=(
  "fvextra.sty"
  "footnote.sty"
  "footnotebackref.sty"
  "pagecolor.sty"
  "hardwrap.sty"
  "mdframed.sty"
  "sourcesanspro.sty"
  "ly1enc.def"
  "sourcecodepro.sty"
  "titling.sty"
  "csquotes.sty"
  "zref-abspage.sty"
  "needspace.sty"
  "selnolig.sty"
)

missing_tex_files=()
for tex_file in "${required_tex_files[@]}"; do
  if ! kpsewhich "$tex_file" >/dev/null 2>&1; then
    missing_tex_files+=("$tex_file")
  fi
done

if [[ "${#missing_tex_files[@]}" -gt 0 ]]; then
  printf 'Missing TeX dependencies required for the Eisvogel PDF build:\n' >&2
  printf '  - %s\n' "${missing_tex_files[@]}" >&2
  echo "Install the packages listed in doc/DEVELOPERS.md and rerun the generator." >&2
  exit 1
fi

mkdir -p "$output_dir"
cd "$script_dir"

common_args=(
  --from=markdown+smart
  --standalone
  --toc
  --number-sections
  --resource-path="$script_dir"
)

pandoc \
  "${common_args[@]}" \
  --to=html5 \
  --embed-resources \
  --output="$html_output" \
  "${markdown_files[@]}"

pandoc \
  "${common_args[@]}" \
  --pdf-engine=xelatex \
  --template="$eisvogel_template" \
  --output="$pdf_output" \
  "${markdown_files[@]}"

printf 'Generated manual:\n  HTML: %s\n  PDF:  %s\n' "$html_output" "$pdf_output"
