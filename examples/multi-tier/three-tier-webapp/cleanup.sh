#!/bin/bash
# Remove all three-tier application workloads

set -e

echo "Cleaning up Three-Tier Web Application..."
echo ""

read -p "This will remove all workloads. Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
fi

# Stop and undeploy all tiers
echo "Stopping tiers..."
./stop.sh

echo ""
echo "Undeploying tiers..."
jplatform undeploy nginx-web
jplatform undeploy spring-app
jplatform undeploy postgres-db

echo ""
echo "✓ All tiers removed"
echo ""
echo "Note: Disk images and data volumes were NOT deleted"
echo "To remove VM disk: sudo rm /var/lib/jplatform/vms/postgres-db.qcow2"
echo "To remove app data: sudo rm -rf /var/lib/jplatform/apps/spring-app"
echo "To remove web data: sudo rm -rf /var/lib/jplatform/apps/nginx-web"
