#!/usr/bin/env bash
# One-shot setup for Linux and macOS: check the JDK, fetch iFogSim, compile,
# run the smallest example. Safe to re-run.
set -euo pipefail
cd "$(dirname "$0")"

IFOGSIM_ZIP="https://codeload.github.com/Cloudslab/iFogSim/zip/refs/heads/main"

echo "[1/4] checking for a JDK"
if ! command -v javac > /dev/null; then
  cat >&2 <<'MSG'
javac was not found. Install a JDK (17 or 21 are both fine), then re-run:
  Debian/Ubuntu   sudo apt install openjdk-21-jdk
  Fedora          sudo dnf install java-21-openjdk-devel
  Arch            sudo pacman -S jdk-openjdk
  macOS           brew install openjdk@21
MSG
  exit 1
fi
javac -version

echo "[2/4] fetching iFogSim"
if [ -d ifogsim/src ]; then
  echo "ifogsim/ already present, skipping download"
else
  rm -rf ifogsim ifogsim-main .fogup-tmp.zip
  if command -v curl > /dev/null; then
    curl -fL "$IFOGSIM_ZIP" -o .fogup-tmp.zip
  else
    wget -O .fogup-tmp.zip "$IFOGSIM_ZIP"
  fi
  unzip -q .fogup-tmp.zip
  mv iFogSim-main ifogsim
  rm -f .fogup-tmp.zip
  echo "iFogSim unpacked into ifogsim/"
fi

echo "[3/4] compiling"
./build.sh

echo "[4/4] smoke test: Ex01Hello"
./run.sh Ex01Hello

cat <<'MSG'

Setup finished. Next:
  ./run.sh Ex02SmartBuilding --mode cloud
  ./run.sh Ex02SmartBuilding --mode edge
  ./experiment.sh
Session plan: docs/02-two-hour-session.md
MSG
