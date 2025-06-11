#!/bin/bash
cd ./InsightsBackend
java -jar target/backend.jar > application.log &
PID=$!
sleep 10

curl --fail http://localhost:8080/api/github-check || (echo "GitHub check endpoint unreachable" && kill $PID && exit 1)

grep "GraphQLClient initialized successfully with base URL: https://api.github.com/graphql" application.log || (echo "GitHub API initialization log missing" && kill $PID && exit 1)

kill $PID
