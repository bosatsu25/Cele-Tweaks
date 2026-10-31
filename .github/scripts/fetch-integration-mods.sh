#!/usr/bin/env bash
# Downloads the newest Fabric build of every optional integration mod for one Minecraft version from
# Modrinth into <dir>, for the integration game test. A mod with no build for that version is skipped
# and reported; its mixins are then simply not selected by IntegrationMixinPlugin.
# Usage: fetch-integration-mods.sh <minecraft version> <dir>
set -euo pipefail
MC="$1"
OUT="$2"
API="https://api.modrinth.com/v2"
UA="bosatsu25/Cele-Tweaks CI (https://github.com/bosatsu25/Cele-Tweaks)"
# malilib first: every masa-family mod depends on it.
SLUGS=(malilib litematica tweakeroo item-scroller tweakermore syncmatica)

mkdir -p "$OUT"
summary="${GITHUB_STEP_SUMMARY:-/dev/null}"
{
  echo "### Integration mods for Minecraft $MC"
  echo
  echo "| Modrinth project | Version | File |"
  echo "| --- | --- | --- |"
} >> "$summary"

for slug in "${SLUGS[@]}"; do
  query="loaders=%5B%22fabric%22%5D&game_versions=%5B%22${MC}%22%5D"
  versions=$(curl -fsS --retry 3 -A "$UA" "$API/project/$slug/version?$query")
  pick=$(printf '%s' "$versions" | jq -r '
    if length == 0 then empty else
      .[0] as $v | ($v.files | (map(select(.primary)) + .)[0]) as $f
      | [$v.version_number, $f.url, $f.filename] | @tsv
    end')
  if [ -z "$pick" ]; then
    echo "$slug: no Fabric build for $MC, skipped"
    echo "| $slug | — (none for $MC) | — |" >> "$summary"
    continue
  fi
  IFS=$'\t' read -r version url filename <<< "$pick"
  curl -fsS --retry 3 -A "$UA" -o "$OUT/$filename" "$url"
  echo "$slug: $version ($filename)"
  echo "| $slug | $version | \`$filename\` |" >> "$summary"
done

ls -la "$OUT"
# Zero downloads means the project slugs or the API changed, not that there is nothing to test.
[ -n "$(find "$OUT" -name '*.jar' -print -quit)" ] || { echo "no integration mod was downloaded for $MC"; exit 1; }
