#!/bin/bash

# Load environment variables from .env file
set -a
source .env
set +a

# Start Spring Boot application
./mvnw spring-boot:run
