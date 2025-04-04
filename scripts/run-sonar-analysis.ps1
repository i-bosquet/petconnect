# Run-Sonar-Analysis.PS1 (Read from .env)
# Script to execute Sonarqube's analysis by reading conflict from .env

Write-Host "Reading configuration from the .env file..."

$envFilePath = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFilePath)) {
    Write-Error "Error: The .env file was not found in the expected path: $envFilePath"
    exit 1
}

# Read the .env file and load the variables in the current session
Get-Content $envFilePath | Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } | ForEach-Object {
    $name, $value = $_.Split('=', 2).Trim()
    # Avoid overwriting variables if they already exist in the upper environment (optional)
     if (-not (Test-Path Env:\$name)) {
        Set-Content "env:\$name" $value
        Write-Host "Variable $name set."
     }
}

# Verifies that the necessary variables now exist in the environment
if (-not ($env:POSTGRES_DB -and $env:POSTGRES_USER -and $env:POSTGRES_PASSWORD -and $env:SONAR_TOKEN)) {
     Write-Error "Error: One or more required variables are missing (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD, SONAR_TOKEN) in the .env file or could not be loaded."
     exit 1
}

Write-Host "Environment variables loaded from .env."

Write-Host "Navigating to the backend directory..."
Set-Location -Path "..\backend"

# Check if the location change was successful
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error: Could not change directory to backend."
    exit 1
}

Write-Host "Running Maven clean verify sonar:sonar..."
# Maven will use the environment variables loaded in the session
mvn clean verify sonar:sonar

$exitCode = $LASTEXITCODE
Write-Host "Maven process terminated with exit code: $exitCode"

exit $exitCode