#!/usr/bin/env bash
# Runs ./gradlew with the given arguments. When the build fails because a repository did not serve a
# dependency (Maven Central's CDN has answered 404 for artifacts it holds, to some runners, 2026-11-19),
# waits a minute and runs the same build once more with --refresh-dependencies, which also discards
# the "missing" verdicts Gradle caches for 24 hours. Any other failure, a failed test above all, is
# reported as is: this only retries what the build could not have influenced.
# Usage: gradle.sh <gradle arguments...>
set -uo pipefail
log=$(mktemp)
trap 'rm -f "$log"' EXIT

./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
[ "$status" -eq 0 ] && exit 0

# Gradle's own wording for a repository failure; a game or test log never contains these.
if grep -qE "Could not resolve all (artifacts|dependencies|files) for configuration|Could not (GET|HEAD) '|Read timed out|Connection reset" "$log"; then
  echo "::warning::Dependency resolution failed; retrying once with --refresh-dependencies in 60 s"
  sleep 60
  ./gradlew "$@" --refresh-dependencies
  exit $?
fi
exit "$status"
