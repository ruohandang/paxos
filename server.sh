#!/bin/bash

# Default values
CENTRAL_REGISTRY_HOST="localhost"
RMI_PORT=1099

# Check if at least one argument is provided (for host)
if [ "$#" -ge 1 ]; then
    CENTRAL_REGISTRY_HOST=$1
fi

# Check if two arguments are provided (for host and port)
if [ "$#" -eq 2 ]; then
    RMI_PORT=$2
fi

echo "Starting RMI registry on host $CENTRAL_REGISTRY_HOST and port $RMI_PORT..."

# Start RMI registry on the specified port and store its PID
rmiregistry $RMI_PORT &
RMI_PID=$!
echo "RMI registry started with PID $RMI_PID"

# Wait a bit for the registry to start up
sleep 2

# Start five server instances with unique identifiers
for i in {1..5}
do
  echo "Starting server instance $i..."
  java -cp . server.ServerApp $CENTRAL_REGISTRY_HOST $RMI_PORT "$i" &
  SERVER_PIDS+=($!)
done

echo -e "\nAll servers and central registry have been started.\n"
read -p $'Press enter to shut down servers and RMI registry...\n\n'

# Shut down server instances gracefully
echo "Shutting down server instances..."
for PID in "${SERVER_PIDS[@]}"
do
  kill -SIGTERM $PID
  wait $PID
  echo "Server with PID $PID has been shut down."
done


# Kill the RMI registry process
echo "Shutting down RMI registry..."
kill $RMI_PID
echo "RMI registry shut down."
