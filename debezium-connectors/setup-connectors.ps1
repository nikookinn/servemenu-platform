# PowerShell script to setup Debezium connectors for all services
# Run this after docker-compose is up and Debezium Connect is ready

$DEBEZIUM_URL = "http://localhost:8085"
$CONNECTORS_DIR = $PSScriptRoot

Write-Host "🚀 Setting up Debezium Connectors for 2025 Outbox Pattern" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan

# Function to check if Debezium Connect is ready
function Test-DebeziumReady {
    try {
        $response = Invoke-RestMethod -Uri "$DEBEZIUM_URL/" -Method Get -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

# Function to create or update a connector
function Deploy-Connector {
    param (
        [string]$ConnectorFile,
        [string]$ConnectorName
    )
    
    Write-Host "`n📋 Processing: $ConnectorName" -ForegroundColor Yellow
    
    # Read connector configuration
    $config = Get-Content -Path $ConnectorFile -Raw
    
    # Check if connector exists
    try {
        $existing = Invoke-RestMethod -Uri "$DEBEZIUM_URL/connectors/$ConnectorName" -Method Get -ErrorAction Stop
        Write-Host "   ✓ Connector exists, updating..." -ForegroundColor Cyan
        
        # Update existing connector
        $response = Invoke-RestMethod -Uri "$DEBEZIUM_URL/connectors/$ConnectorName/config" `
            -Method Put `
            -Headers @{"Content-Type"="application/json"} `
            -Body ($config | ConvertFrom-Json).config | ConvertTo-Json -Depth 10
            
        Write-Host "   ✅ Updated successfully!" -ForegroundColor Green
    } catch {
        if ($_.Exception.Response.StatusCode -eq 404) {
            Write-Host "   ✓ Creating new connector..." -ForegroundColor Cyan
            
            # Create new connector
            $response = Invoke-RestMethod -Uri "$DEBEZIUM_URL/connectors" `
                -Method Post `
                -Headers @{"Content-Type"="application/json"} `
                -Body $config
                
            Write-Host "   ✅ Created successfully!" -ForegroundColor Green
        } else {
            Write-Host "   ❌ Error: $_" -ForegroundColor Red
        }
    }
}

# Wait for Debezium Connect to be ready
Write-Host "`n⏳ Waiting for Debezium Connect to be ready..." -ForegroundColor Yellow
$retries = 0
$maxRetries = 30

while (-not (Test-DebeziumReady) -and $retries -lt $maxRetries) {
    Start-Sleep -Seconds 2
    $retries++
    Write-Host "." -NoNewline
}

if ($retries -eq $maxRetries) {
    Write-Host "`n❌ Debezium Connect is not responding at $DEBEZIUM_URL" -ForegroundColor Red
    Write-Host "Please ensure docker-compose is running and Debezium Connect is healthy." -ForegroundColor Yellow
    exit 1
}

Write-Host "`n✅ Debezium Connect is ready!" -ForegroundColor Green

# Deploy all connectors
$connectorFiles = @(
    @{File="business-service-outbox.json"; Name="business-service-outbox-connector"},
    @{File="user-service-outbox.json"; Name="user-service-outbox-connector"},
    @{File="media-service-outbox.json"; Name="media-service-outbox-connector"}
)

foreach ($connector in $connectorFiles) {
    $filePath = Join-Path $CONNECTORS_DIR $connector.File
    if (Test-Path $filePath) {
        Deploy-Connector -ConnectorFile $filePath -ConnectorName $connector.Name
    } else {
        Write-Host "`n⚠️  File not found: $($connector.File)" -ForegroundColor Yellow
    }
}

# List all active connectors
Write-Host "`n📊 Active Connectors:" -ForegroundColor Cyan
try {
    $connectors = Invoke-RestMethod -Uri "$DEBEZIUM_URL/connectors" -Method Get
    foreach ($conn in $connectors) {
        $status = Invoke-RestMethod -Uri "$DEBEZIUM_URL/connectors/$conn/status" -Method Get
        $state = $status.connector.state
        $color = if ($state -eq "RUNNING") { "Green" } else { "Red" }
        Write-Host "   • $conn : $state" -ForegroundColor $color
    }
} catch {
    Write-Host "   ❌ Could not retrieve connector list" -ForegroundColor Red
}

Write-Host "`n✨ Debezium setup complete!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Monitor connectors at: http://localhost:9021 (Kafka UI)" -ForegroundColor Yellow
