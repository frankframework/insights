#!/bin/bash
cd InsightsFrontend
npm start &
PID=$!
sleep 10
curl --fail http://localhost:4200 || (echo "Frontend health check failed" && kill $PID && exit 1)
kill $PID
