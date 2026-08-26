#!/usr/bin/env bash
set -e

export JAVA_OPTS="\
  -XX:MaxRAMPercentage=50 \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  $JAVA_OPTS"

exec "$@"
