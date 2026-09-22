#!/usr/bin/env bash
# Compiles iFogSim plus the FogUp examples into build/classes.
# Usage: ./build.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -d ifogsim/src ]; then
  echo "ifogsim/ is missing. Run ./setup.sh first." >&2
  exit 1
fi

CP="ifogsim/jars/*:ifogsim/jars/commons-math3-3.5/*"

rm -rf build/classes
mkdir -p build/classes
# Quote every path: javac argument files split on unquoted whitespace, so a
# checkout under a directory with a space in its name would otherwise fail.
find ifogsim/src src -name '*.java' -printf '"%p"\n' > build/sources.txt
echo "compiling $(wc -l < build/sources.txt) files..."
javac -nowarn -encoding UTF-8 -cp "$CP" -d build/classes @build/sources.txt
echo "build/classes is up to date."
