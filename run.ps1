<#
.SYNOPSIS
    Starts all dependencies for support-assistant and runs the Spring Boot app.

.DESCRIPTION
    1. Starts the pgvector (Postgres) container via Docker Compose.
    2. Waits for Postgres to become healthy.
    3. Ensures a local Ollama server is running and required models are pulled.
    4. Runs the Spring Boot app with ./gradlew bootRun.
#>

$ErrorActionPreference = "Stop"

$OllamaModels = @("llama3.2", "nomic-embed-text")
$OllamaBaseUrl = "http://localhost:11434"

function Write-Step($msg) {
    Write-Host "==> $msg" -ForegroundColor Cyan
}

# 1. Docker check
Write-Step "Checking Docker..."
try {
    docker info *> $null
} catch {
    Write-Error "Docker does not appear to be running. Please start Docker Desktop and try again."
    exit 1
}

# 2. Start pgvector container
Write-Step "Starting pgvector (Postgres) container..."
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start docker compose services."
    exit 1
}

# 3. Wait for Postgres to be healthy
Write-Step "Waiting for Postgres to be ready..."
$containerId = docker compose ps -q pgvector
$maxRetries = 30
$retries = 0
while ($true) {
    $health = docker inspect --format='{{.State.Health.Status}}' $containerId 2>$null
    if ($health -eq "healthy") {
        Write-Host "Postgres is ready." -ForegroundColor Green
        break
    }
    $retries++
    if ($retries -ge $maxRetries) {
        Write-Error "Postgres did not become healthy in time."
        exit 1
    }
    Start-Sleep -Seconds 2
}

# 4. Ensure Ollama server is running
Write-Step "Checking Ollama server at $OllamaBaseUrl..."
$ollamaUp = $false
try {
    Invoke-RestMethod -Uri "$OllamaBaseUrl/api/tags" -Method Get -TimeoutSec 3 | Out-Null
    $ollamaUp = $true
} catch {
    $ollamaUp = $false
}

if (-not $ollamaUp) {
    Write-Step "Ollama server not reachable. Starting 'ollama serve'..."
    Start-Process -FilePath "ollama" -ArgumentList "serve" -WindowStyle Hidden
    $retries = 0
    while (-not $ollamaUp -and $retries -lt 15) {
        Start-Sleep -Seconds 2
        try {
            Invoke-RestMethod -Uri "$OllamaBaseUrl/api/tags" -Method Get -TimeoutSec 3 | Out-Null
            $ollamaUp = $true
        } catch {
            $retries++
        }
    }
    if (-not $ollamaUp) {
        Write-Error "Could not start Ollama server. Please start it manually with 'ollama serve'."
        exit 1
    }
}
Write-Host "Ollama server is up." -ForegroundColor Green

# 5. Ensure required models are pulled
Write-Step "Checking required Ollama models..."
$tags = Invoke-RestMethod -Uri "$OllamaBaseUrl/api/tags" -Method Get
$installedModels = $tags.models | ForEach-Object { $_.name }

foreach ($model in $OllamaModels) {
    $found = $installedModels | Where-Object { $_ -like "$model*" }
    if (-not $found) {
        Write-Step "Pulling missing model '$model' (this may take a while)..."
        ollama pull $model
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to pull model '$model'."
            exit 1
        }
    } else {
        Write-Host "Model '$model' already present." -ForegroundColor Green
    }
}

# 6. Run the Spring Boot app
Write-Step "Starting Spring Boot application (./gradlew bootRun)..."
./gradlew.bat bootRun
