#!/usr/bin/env bash


./gradlew clean build && ./gradlew nativeCompile && \


docker buildx build --platform linux/amd64 -t thalessantanna/payments-revolts:0.0.1 --push .

