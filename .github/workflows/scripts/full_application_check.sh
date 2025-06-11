#!/bin/bash

echo "Full stack integration test started"

BACKEND_JAR=./InsightsBackend/target/insights-0.0.1-SNAPSHOT.jar
BACKEND_LOG=backend.log
FRONTEND_LOG=frontend.log
GITHUB_API_SECRET="${GITHUB_API_SECRET}"
EXIT_CODE=0

if [ ! -f "$BACKEND_JAR" ]; then
  echo "Backend JAR not found at $BACKEND_JAR"
  exit 1
fi

echo "Starting backend..."
java -Dspring.datasource.url=jdbc:postgresql://localhost:5432/insights \
     -Dspring.datasource.username=postgres \
     -Dspring.datasource.password=postgres \
     -Dspring.profiles.active=local \
     -Dgithub.secret="$GITHUB_API_SECRET" \
     -jar "$BACKEND_JAR" > $BACKEND_LOG 2>&1 &
BACKEND_PID=$!

sleep 15

if ! kill -0 $BACKEND_PID 2>/dev/null; then
  echo "Backend failed to start"
  cat $BACKEND_LOG
  exit 1
fi
echo "Backend running (PID $BACKEND_PID)"

if ! curl --fail http://localhost:8080/actuator/health; then
  echo "Backend health check failed"
  cat $BACKEND_LOG
  kill $BACKEND_PID
  exit 1
fi

for msg in \
  "Started InsightsApplication" \
  "Added connection org.postgresql.jdbc.PgConnection" \
  "GraphQLClient initialized successfully with base URL: https://api.github.com/graphql"
do
  if ! grep "$msg" $BACKEND_LOG; then
    echo "Log message missing: $msg"
    cat $BACKEND_LOG
    kill $BACKEND_PID
    exit 1
  fi
done
echo "Backend initialization verified"

echo "Starting frontend..."
cd InsightsFrontend
npm start > ../$FRONTEND_LOG 2>&1 &
FRONTEND_PID=$!
cd ..

sleep 15

if ! kill -0 $FRONTEND_PID 2>/dev/null; then
  echo "Frontend failed to start"
  cat $FRONTEND_LOG
  kill $BACKEND_PID
  exit 1
fi

if ! curl --fail http://localhost:4200; then
  echo "Frontend health check failed"
  cat $FRONTEND_LOG
  kill $BACKEND_PID
  kill $FRONTEND_PID
  exit 1
fi
echo "Frontend reachable"

echo "Triggering frontend in headless browser to cause real API call..."
node .github/workflows/scripts/trigger_frontend.mjs

echo "🔗 Checking frontend-backend communication via backend logs..."
sleep 5

if ! grep -E "Successfully fetched and mapped [0-9]+ releases from the database" "$BACKEND_LOG"; then
  echo "Expected fetch of releases, but log line is not found in backend logs"
  cat "$BACKEND_LOG"
  kill $BACKEND_PID
  kill $FRONTEND_PID
  exit 1
fi

echo "Frontend-Backend communication verified"

kill $BACKEND_PID
kill $FRONTEND_PID

echo "Full stack integration test completed successfully"
exit $EXIT_CODE
