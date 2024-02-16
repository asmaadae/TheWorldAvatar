#!/bin/bash

# Check if the correct number of arguments is provided
if [ "$#" -ne 2 ] || [ "$1" != "start" ]; then
    echo "Usage: $0 start <STACK_NAME>"
    exit 1
fi

STACK_NAME="$2"

# Set the base URL
BASE_URL="http://localhost:3838"

# IsochroneAgent
ISOCHRONE_URL="$BASE_URL/isochroneagent/update?function=Toilet&timethreshold=10&timeinterval=2"
curl -X POST "$ISOCHRONE_URL"
