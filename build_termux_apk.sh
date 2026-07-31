#!/usr/bin/env bash

# ==============================================================================
# Android AI Operator (AIOperator) — Termux Debug APK Generation Script
# ==============================================================================
#
# This script automates the process of building the debug APK directly on device
# inside the Termux environment.
#
# Prerequisites in Termux:
#   1. Packages: pkg install openjdk-17 x11-repo tur-repo gradle
#   2. Storage access: termux-setup-storage (optional, for copying APK)
# ==============================================================================

set -euo pipefail

# ANSI color codes for premium terminal formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${CYAN}    Android AI Operator (AIOperator) Build Tool      ${NC}"
echo -e "${BLUE}======================================================${NC}"
echo -e "Starting on-device Termux compilation workflow...\n"

# 1. Environment & Java Check
echo -e "${BLUE}[1/4] Checking environment & dependencies...${NC}"

if [ -f "/data/data/com.termux/files/usr/bin/termux-info" ]; then
    echo -e "${GREEN}✓ Termux environment detected.${NC}"
else
    echo -e "${YELLOW}! Non-Termux environment detected. Proceeding with standard build...${NC}"
fi

if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    # Handle Java 17+ or 1.8 formats
    if [ "$JAVA_VERSION" = "1" ] || [ "$JAVA_VERSION" = "17" ] || [ "$JAVA_VERSION" -ge 17 ] 2>/dev/null; then
        echo -e "${GREEN}✓ Java 17+ is installed and active ($JAVA_VERSION).${NC}"
    else
        echo -e "${RED}✗ Incompatible Java version detected ($JAVA_VERSION).${NC}"
        echo -e "${YELLOW}Please install OpenJDK 17 in Termux by running:${NC}"
        echo -e "  pkg install openjdk-17"
        exit 1
    fi
else
    echo -e "${RED}✗ Java is not installed.${NC}"
    echo -e "${YELLOW}Please install OpenJDK 17 in Termux by running:${NC}"
    echo -e "  pkg install openjdk-17"
    exit 1
fi

# 2. Gradlew Executable check
echo -e "\n${BLUE}[2/4] Setting up build permissions...${NC}"
if [ -f "./gradlew" ]; then
    chmod +x gradlew
    echo -e "${GREEN}✓ Set executable permissions for gradlew successfully.${NC}"
else
    echo -e "${RED}✗ gradlew (Gradle wrapper) not found at repository root.${NC}"
    exit 1
fi

# 3. Triggering Compilation
echo -e "\n${BLUE}[3/4] Compiling debug APK via Gradle wrapper...${NC}"
echo -e "${YELLOW}This may take a few minutes depending on device thermal throttling and resources.${NC}"
echo -e "${YELLOW}Running: ./gradlew assembleDebug${NC}\n"

if ./gradlew assembleDebug; then
    echo -e "\n${GREEN}✓ Gradle compilation succeeded!${NC}"
else
    echo -e "\n${RED}✗ Gradle build failed.${NC}"
    echo -e "${YELLOW}Tip: Ensure you have enough free RAM and storage space (at least 2-3GB).${NC}"
    echo -e "${YELLOW}To check free memory, run: free -h${NC}"
    exit 1
fi

# 4. APK Relocation
echo -e "\n${BLUE}[4/4] Locating and copying compiled APK...${NC}"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
TARGET_APK="ai-operator-debug.apk"

if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" "$TARGET_APK"
    echo -e "${GREEN}✓ Success! Debug APK copied to repo root: ${YELLOW}$TARGET_APK${NC}"
    echo -e "${BLUE}======================================================${NC}"
    echo -e "${GREEN}Installation Instructions in Termux:${NC}"
    echo -e "To install the APK on your device, execute:"
    echo -e "  ${CYAN}termux-open $TARGET_APK${NC}"
    echo -e "Or copy it to your shared internal storage:"
    echo -e "  ${CYAN}cp $TARGET_APK /sdcard/Download/${NC}"
    echo -e "${BLUE}======================================================${NC}"
else
    echo -e "${RED}✗ Expected APK not found at $APK_PATH.${NC}"
    exit 1
fi
