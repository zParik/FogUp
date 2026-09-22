#!/usr/bin/env bash
# Finds a JDK, and downloads a portable one into jdk/ when the machine has none.
# Sourced by setup.sh, build.sh and run.sh; not meant to be run directly.
#
# The portable route needs no root and no package manager, which matters on a
# shared or locked-down machine. Delete jdk/ to undo it.

FOGUP_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JDK_ROOT="$FOGUP_ROOT/jdk"

find_javac() {
  # The portable JDK this repository downloaded wins, so a run is reproducible
  # even on a machine that also has some other JDK on PATH.
  if [ -d "$JDK_ROOT" ]; then
    local local_javac
    local_javac="$(find "$JDK_ROOT" -name javac -type f -perm -u+x 2>/dev/null | head -n 1)"
    if [ -n "$local_javac" ]; then
      dirname "$local_javac"
      return 0
    fi
  fi
  if command -v javac > /dev/null; then
    dirname "$(command -v javac)"
    return 0
  fi
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    echo "$JAVA_HOME/bin"
    return 0
  fi
  return 1
}

install_portable_jdk() {
  local os arch url tarball
  case "$(uname -s)" in
    Linux*)  os=linux ;;
    Darwin*) os=mac ;;
    *) echo "Unsupported system $(uname -s); install a JDK by hand." >&2; return 1 ;;
  esac
  case "$(uname -m)" in
    x86_64|amd64) arch=x64 ;;
    arm64|aarch64) arch=aarch64 ;;
    *) echo "Unsupported CPU $(uname -m); install a JDK by hand." >&2; return 1 ;;
  esac

  url="https://api.adoptium.net/v3/binary/latest/21/ga/$os/$arch/jdk/hotspot/normal/eclipse"
  tarball="$FOGUP_ROOT/.fogup-jdk.tar.gz"

  echo "      downloading a portable Temurin 21 JDK ($os/$arch, about 200 MB)"
  if command -v curl > /dev/null; then
    curl -fL "$url" -o "$tarball"
  else
    wget -O "$tarball" "$url"
  fi

  rm -rf "$JDK_ROOT"
  mkdir -p "$JDK_ROOT"
  tar -xzf "$tarball" -C "$JDK_ROOT" || { echo "could not unpack $tarball" >&2; return 1; }
  rm -f "$tarball"
  echo "      unpacked into jdk/ (delete that folder to undo this)"
}

# use_jdk [--install]
use_jdk() {
  local bin
  if ! bin="$(find_javac)"; then
    if [ "${1:-}" != "--install" ]; then
      echo "No JDK found. Run ./setup.sh, which downloads a portable one into jdk/." >&2
      return 1
    fi
    install_portable_jdk || return 1
    bin="$(find_javac)" || { echo "The JDK downloaded but no javac turned up under jdk/." >&2; return 1; }
  fi
  case ":$PATH:" in
    *":$bin:"*) ;;
    *) PATH="$bin:$PATH" ;;
  esac
  export PATH
  export JAVA_HOME="$(dirname "$bin")"
}
