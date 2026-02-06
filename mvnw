#!/usr/bin/env bash
# Minimal mvnw shim: delegates to system "mvn" if available.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

cat <<'EOF' >&2
Maven wrapper not installed. This repository provides a minimal mvnw shim that delegates
to the system 'mvn' command. Please install Maven (https://maven.apache.org/install.html)
or add 'mvn' to your PATH.

If you want the full Maven Wrapper behavior, run:
  mvn -N io.takari:maven:wrapper
from a machine that has Maven and network access; then commit the generated
.mvn/wrapper files and the maven-wrapper.jar.
EOF
exit 127
