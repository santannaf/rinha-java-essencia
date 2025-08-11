#!/usr/bin/env bash


docker rm -f $(docker ps -a -q) && docker system prune -f && \


docker compose -f ./payment-processor/docker-compose.yml up -d && \


docker compose up -d

