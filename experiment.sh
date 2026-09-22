#!/usr/bin/env bash
# Sweeps Ex02SmartBuilding over both placements and three topology sizes, then
# leaves every run in one CSV. iFogSim calls System.exit() at the end of a
# simulation, so each point has to be its own JVM.
set -euo pipefail
cd "$(dirname "$0")"

OUT=results/experiment.csv
rm -f "$OUT"
mkdir -p results

for cameras in 2 4 8; do
  for mode in cloud edge; do
    echo "--- mode=$mode cameras=$cameras"
    ./run.sh Ex02SmartBuilding --mode "$mode" --gateways 2 --cameras "$cameras" --csv "$OUT" > /dev/null
  done
done

echo
echo "$OUT:"
column -s, -t < "$OUT" 2>/dev/null || cat "$OUT"
