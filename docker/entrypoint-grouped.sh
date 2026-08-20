#!/bin/sh

# Trap SIGTERM and SIGINT to forward them to children and exit
trap 'kill $(jobs -p) 2>/dev/null; exit 0' TERM INT

echo "Starting grouped services..."

# Parse space-separated list of service:port in STARTUP_SERVICES
for svc_pair in $STARTUP_SERVICES; do
  svc=$(echo "$svc_pair" | cut -d':' -f1)
  port=$(echo "$svc_pair" | cut -d':' -f2)
  jar_path="/app/${svc}.jar"

  if [ -f "$jar_path" ]; then
    echo "Launching service $svc on port $port..."
    java $JAVA_OPTS -jar "$jar_path" --server.port="$port" &
  else
    echo "Error: Jar for service $svc not found at $jar_path"
    exit 1
  fi
done

# Monitor child processes. If any process exits (fails), terminate the whole group and exit.
while true; do
  # Get child PIDs.
  pids=$(jobs -p)
  if [ -z "$pids" ]; then
    echo "No background services found. Exiting..."
    exit 1
  fi
  for pid in $pids; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "Service process $pid died. Terminating group..."
      kill $(jobs -p) 2>/dev/null
      exit 1
    fi
  done
  sleep 2
done
