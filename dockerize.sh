#!/bin/bash

#
# builds Docker images for all components of this project
#

set -euo pipefail

./gradlew build -x check

cd redfox-app/
docker build -t redfox-app:latest .
cd ../

cd redfox-authserver/
docker build -t redfox-authserver:latest .
cd ../

cd redfox-flyway/
docker build -t redfox-flyway:latest .
cd ../

cd redfox-webapp/
docker build -t redfox-webapp:latest .
cd ../
