#!/bin/bash
cd ./InsightsBackend
java -jar target/backend.jar > application.log &
PID=$!
sleep 10

curl --fail http://localhost:8080/actuator/health/db || (echo "DB health endpoint unreachable" && kill $PID && exit 1)

grep "Added connection org.postgresql.jdbc.PgConnection" application.log || (echo "DB connection log missing" && kill $PID && exit 1)

kill $PID
