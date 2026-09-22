#!/usr/bin/env bash
# Runs one example: ./run.sh Ex02SmartBuilding --mode edge --cameras 4
# Any class in src/fogup works, as does any iFogSim example, e.g.
#   ./run.sh org.fog.test.perfeval.VRGameFog
set -euo pipefail
cd "$(dirname "$0")"

if [ $# -lt 1 ]; then
  echo "usage: ./run.sh <ExampleName> [options]" >&2
  echo "examples: Ex01Hello, Ex02SmartBuilding, Ex03Wearable" >&2
  exit 1
fi

if [ ! -d build/classes ]; then
  echo "Nothing compiled yet. Run ./build.sh first." >&2
  exit 1
fi

CLASS="$1"; shift
case "$CLASS" in
  *.*) ;;                      # already fully qualified
  *) CLASS="fogup.$CLASS" ;;   # bare name means one of ours
esac

CP="build/classes:ifogsim/jars/*:ifogsim/jars/commons-math3-3.5/*"
mkdir -p results
exec java -cp "$CP" "$CLASS" "$@"
