#!/bin/bash
set -e

cd ~/apps/vault-api

echo "Pulling latest code..."
git pull origin main

echo "Rebuilding and restarting containers..."
docker compose --env-file .env.properties down
docker compose --env-file .env.properties up -d --build

echo "Cleaning up old images..."
docker system prune -f

echo "Deployed at $(date)"