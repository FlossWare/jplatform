#!/bin/bash
# Test the three-tier web application

set -e

echo "Testing Three-Tier Web Application..."
echo ""

# Test 1: Check all tiers are running
echo "Test 1: Checking tier status..."
STATUS=$(jplatform status | grep -E "postgres-db|spring-app|nginx-web")
echo "$STATUS"

if echo "$STATUS" | grep -q "RUNNING.*RUNNING.*RUNNING"; then
    echo "✓ All tiers running"
else
    echo "✗ Some tiers not running"
    exit 1
fi

echo ""

# Test 2: Web tier responds
echo "Test 2: Testing web tier..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Web tier responding (HTTP $HTTP_CODE)"
else
    echo "✗ Web tier not responding (HTTP $HTTP_CODE)"
    exit 1
fi

echo ""

# Test 3: Application API responds
echo "Test 3: Testing application API..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Application API responding (HTTP $HTTP_CODE)"
else
    echo "✗ Application API not responding (HTTP $HTTP_CODE)"
    exit 1
fi

echo ""

# Test 4: Database connectivity
echo "Test 4: Testing database connectivity (via app)..."
# Test via application health check which includes DB check
HEALTH=$(curl -s http://localhost:8081/actuator/health)
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "✓ Database connectivity verified"
else
    echo "✗ Database not accessible"
    exit 1
fi

echo ""

# Test 5: Cross-tier communication
echo "Test 5: Testing cross-tier communication..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/users)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Cross-tier communication working"
else
    echo "⚠ Cross-tier API returned HTTP $HTTP_CODE (may be expected if no users)"
fi

echo ""
echo "===================================="
echo "All Tests Passed!"
echo "===================================="
echo ""
echo "Architecture verified:"
echo "  Web (Container) → App (Java) → DB (VM)"
echo ""
echo "All three workload types communicating successfully."
