#!/bin/bash
# Deploy the three-tier web application
# Usage: ./deploy.sh

set -e

echo "===================================="
echo "Three-Tier Web Application Deployment"
echo "===================================="
echo ""

# Check JPlatform is available
if ! command -v jplatform >/dev/null 2>&1; then
    echo "ERROR: jplatform command not found"
    echo "Install JPlatform first"
    exit 1
fi

# Deploy tiers in order
echo "Step 1: Deploying Database Tier (PostgreSQL VM)..."
jplatform deploy 1-database-tier.yaml
echo "✓ Database tier deployed"
echo ""

echo "Step 2: Deploying Application Tier (Spring Boot Java)..."
jplatform deploy 2-app-tier.yaml
echo "✓ Application tier deployed"
echo ""

echo "Step 3: Deploying Web Tier (NGINX Container)..."
jplatform deploy 3-web-tier.yaml
echo "✓ Web tier deployed"
echo ""

echo "===================================="
echo "Deployment Complete!"
echo "===================================="
echo ""
echo "Next steps:"
echo "  1. Start the stack:    ./start.sh"
echo "  2. Check status:       jplatform status"
echo "  3. Run tests:          ./test.sh"
echo "  4. View logs:          jplatform logs <tier-id>"
echo ""
echo "Deployed workloads:"
echo "  - postgres-db  (VM)"
echo "  - spring-app   (Java)"
echo "  - nginx-web    (Container)"
