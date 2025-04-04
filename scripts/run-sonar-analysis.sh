#!/bin/bash
# run-sonar-analysis.sh (Reads from .env)
# Script to execute SonarQube analysis reading configuration from .env

echo "Reading configuration from .env file..."

# Get the directory where the script resides
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
# Assume .env file is in the same directory as the script (project root)
ENV_FILE="$SCRIPT_DIR/../.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found at expected path: $ENV_FILE"
    exit 1
fi

# Export all variables defined in .env to the current session
# Skips comments and empty lines implicitly via 'source' behavior with 'set -a'
set -a # Automatically export all variables defined from now on
source "$ENV_FILE"
set +a # Stop automatically exporting variables

# Verify that the required variables are now set in the environment
if [ -z "$POSTGRES_DB" ] || [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_PASSWORD" ] || [ -z "$SONAR_TOKEN" ]; then
     echo "Error: One or more required variables (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD, SONAR_TOKEN) are missing from .env or could not be loaded."
     # Unset potentially partially loaded variables before exiting (optional cleanup)
     unset POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD SONAR_TOKEN
     exit 1
fi

echo "Environment variables loaded from .env."

echo "Navigating to the backend directory..."
cd "$SCRIPT_DIR/../backend" || { echo "Error: Could not change directory to backend"; exit 1; } # Exit if cd fails

echo "Executing Maven clean verify sonar:sonar..."
# Maven will use the environment variables exported to the session
mvn clean verify sonar:sonar

# Capture the exit code from Maven
exit_code=$?
echo "Maven process finished with exit code: $exit_code"

# Return to the original directory (optional good practice)
cd "$SCRIPT_DIR" || exit 1

exit $exit_code