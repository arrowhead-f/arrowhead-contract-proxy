#!/bin/bash

# Make the directory of this file to the current directory.
cd "$(dirname "$0")" || exit

# Build maven packages.
cd .. && mvn clean package

# Move back to this directory.
cd "$(dirname "$0")" || exit

# Bring up docker cluster.
docker-compose up --build