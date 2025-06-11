#!/bin/bash
cd ./InsightsBackend
java -jar target/backend.jar > application.log &
BACKEND_PID=$!

cd ../frontend
npm start &
FRONTEND_PID=$!

sleep 15

curl --fail http://localhost:4200/api/endpoint || (echo "Frontend-Backend endpoint unreachable" && kill $BACKEND_PID && kill $FRONTEND_PID && exit 1)

grep "Received request at /api/endpoint" ../InsightsBackend/application.log || (echo "Frontend request log missing" && kill $BACKEND_PID && kill $FRONTEND_PID && exit 1)

kill $BACKEND_PID
kill $FRONTEND_PID
