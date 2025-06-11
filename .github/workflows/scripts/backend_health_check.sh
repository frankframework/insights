#!/bin/bash
BACKEND_JAR=./InsightsBackend/target/insights-0.0.1-SNAPSHOT.jar

if [ ! -f "$BACKEND_JAR" ]; then
  echo "Backend JAR not found at $BACKEND_JAR"
  exit 1
fi

java -jar "$BACKEND_JAR" > backend.log 2>&1 &
PID=$!
sleep 10

if ! curl --fail http://localhost:8080/actuator/health; then
  echo "Health endpoint unreachable"
  kill $PID || true
  exit 1
fi

if ! grep "Started InsightsApplication" backend.log; then
  echo "InsightsApplication log missing"
  kill $PID || true
  exit 1
fi

kill $PID
