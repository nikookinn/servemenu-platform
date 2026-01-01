#!/bin/bash

# Bash script to setup Debezium connectors for all services
# Run this after docker-compose is up and Debezium Connect is ready

DEBEZIUM_URL="http://localhost:8085"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Setting up Debezium Connectors for 2025 Outbox Pattern"
echo "================================================"

# Function to check if Debezium Connect is ready
check_debezium_ready() {
    curl -s -o /dev/null -w "%{http_code}" "$DEBEZIUM_URL/" | grep -q "200"
}

# Function to create or update a connector
deploy_connector() {
    local connector_file=$1
    local connector_name=$2
    
    echo -e "\n📋 Processing: $connector_name"
    
    # Check if connector exists
    if curl -s -o /dev/null -w "%{http_code}" "$DEBEZIUM_URL/connectors/$connector_name" | grep -q "200"; then
        echo "   ✓ Connector exists, updating..."
        
        # Extract config from JSON file and update
        config=$(cat "$connector_file" | jq '.config')
        curl -X PUT \
            -H "Content-Type: application/json" \
            -d "$config" \
            "$DEBEZIUM_URL/connectors/$connector_name/config" \
            -s -o /dev/null
            
        echo "   ✅ Updated successfully!"
    else
        echo "   ✓ Creating new connector..."
        
        # Create new connector
        curl -X POST \
            -H "Content-Type: application/json" \
            -d @"$connector_file" \
            "$DEBEZIUM_URL/connectors" \
            -s -o /dev/null
            
        echo "   ✅ Created successfully!"
    fi
}

# Wait for Debezium Connect to be ready
echo -e "\n⏳ Waiting for Debezium Connect to be ready..."
retries=0
max_retries=30

while ! check_debezium_ready && [ $retries -lt $max_retries ]; do
    sleep 2
    retries=$((retries + 1))
    echo -n "."
done

if [ $retries -eq $max_retries ]; then
    echo -e "\n❌ Debezium Connect is not responding at $DEBEZIUM_URL"
    echo "Please ensure docker-compose is running and Debezium Connect is healthy."
    exit 1
fi

echo -e "\n✅ Debezium Connect is ready!"

# Deploy all connectors
declare -a connectors=(
    "business-service-outbox.json:business-service-outbox-connector"
    "user-service-outbox.json:user-service-outbox-connector"
    "media-service-outbox.json:media-service-outbox-connector"
    "qr-service-outbox.json:qr-service-outbox-connector"
)

for connector in "${connectors[@]}"; do
    IFS=':' read -r file name <<< "$connector"
    file_path="$SCRIPT_DIR/$file"
    
    if [ -f "$file_path" ]; then
        deploy_connector "$file_path" "$name"
    else
        echo -e "\n⚠️  File not found: $file"
    fi
done

# List all active connectors
echo -e "\n📊 Active Connectors:"
connectors=$(curl -s "$DEBEZIUM_URL/connectors" | jq -r '.[]')

for conn in $connectors; do
    status=$(curl -s "$DEBEZIUM_URL/connectors/$conn/status" | jq -r '.connector.state')
    if [ "$status" == "RUNNING" ]; then
        echo "   • $conn : ✅ $status"
    else
        echo "   • $conn : ❌ $status"
    fi
done

echo -e "\n✨ Debezium setup complete!"
echo "================================================"
echo "Monitor connectors at: http://localhost:9021 (Kafka UI)"
