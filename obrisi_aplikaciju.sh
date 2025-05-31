#!/bin/bash
docker compose down --volumes --remove-orphans
docker rmi structured-logging-backend structured-logging-frontend -f 2>/dev/null
