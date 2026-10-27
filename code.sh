#!/bin/bash
# Gradle build/sync commands for ChatBotPro

# Sync project (refresh dependencies)
./gradlew --refresh-dependencies

# Clean and build debug
./gradlew clean :app:assembleDebug

# Build release
./gradlew :app:assembleRelease

# Run unit tests
./gradlew :app:test

# Run all checks
./gradlew check

# Run lint
./gradlew lint

# Generate all build outputs
./gradlew assemble
