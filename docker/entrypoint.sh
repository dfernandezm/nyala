#!/bin/bash

rm /opt/app/config.json

# Configmap volume is mounted at /kubernetes/config from the deployment
ln -s /kubernetes/config/config.json /opt/app/config.json

echo "Starting with config:"
cat /opt/app/config.json

# launch the app
echo "API_OPTS: $API_OPTS, JAVA_OPTS: $JAVA_OPTS"

term_handler() {
  if [ $PID -ne 0 ]; then
    echo "Forwarding TERM signal to JVM -- pid $PID"
    kill -SIGTERM "$PID"
    wait $PID
    EXIT_STATUS=$?
    echo "Exited with code $EXIT_STATUS"
  fi
  exit 143; # 128 + 15 -- SIGTERM
}

trap "echo 'Starting handler for termination...'; term_handler" SIGINT SIGTERM
echo "PID of shell is $$"
java -jar -Dfile.encoding=UTF-8 $JAVA_OPTS $API_OPTS nyala.jar -conf /opt/app/config.json &
PID=$!
wait $PID
