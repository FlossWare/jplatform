#!/bin/bash
# Start the three-tier web application
# JPlatform automatically handles dependency ordering

set -e

echo "Starting Three-Tier Web Application..."
echo ""

# Start all tiers - JPlatform will start in dependency order:
# 1. postgres-db (no dependencies)
# 2. spring-app (depends on postgres-db)
# 3. nginx-web (depends on spring-app)

jplatform start postgres-db
echo "✓ Database tier starting..."
echo ""

jplatform start spring-app
echo "✓ Application tier starting (waiting for database)..."
echo ""

jplatform start nginx-web
echo "✓ Web tier starting (waiting for application)..."
echo ""

echo "===================================="
echo "Waiting for all tiers to be ready..."
sleep 10

echo ""
echo "Application Status:"
jplatform status

echo ""
echo "===================================="
echo "Application Ready!"
echo "===================================="
echo "  Web UI:  http://localhost:8080/"
echo "  API:     http://localhost:8080/api/"
echo "  Health:  http://localhost:8081/actuator/health"
