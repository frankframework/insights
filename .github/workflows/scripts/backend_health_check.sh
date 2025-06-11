#!/bin/bash
cd ./InsightsBackend
java -jar target/backend.jar > application.log &
PID=$!
sleep 10

curl --fail http://localhost:8080/actuator/health || (echo "Health endpoint unreachable" && kill $PID && exit 1)

grep "Started InsightsApplication" application.log || (echo "InsightsApplication log missing" && kill $PID && exit 1)

kill $PID
