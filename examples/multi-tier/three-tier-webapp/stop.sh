#!/bin/bash
# Stop the three-tier web application
# Stops in reverse dependency order

set -e

echo "Stopping Three-Tier Web Application..."
echo ""

# Stop in reverse order
jplatform stop nginx-web
echo "✓ Web tier stopped"

jplatform stop spring-app
echo "✓ Application tier stopped"

jplatform stop postgres-db
echo "✓ Database tier stopped"

echo ""
echo "All tiers stopped."
