#!/bin/bash
# Demo 1: Allow Failures Feature

PROJECT_DIR="/Users/terigong/Documents/Northeastern University/CS7580/f-team"
export CICD_SERVER_URL=http://localhost:8080

cicd() {
  java -jar "$PROJECT_DIR/client/build/libs/client-all.jar" "$@"
}

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}========================================"
echo -e "  Demo 1: Allow Failures Feature"
echo -e "========================================${NC}"
echo ""

echo -e "${YELLOW}Step 1: Verify the pipeline configuration${NC}"
echo "$ cicd verify .pipelines/allow-failure.yaml"
echo ""
cicd verify "$PROJECT_DIR/.pipelines/allow-failure.yaml"
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 2: Dry run - preview execution order${NC}"
echo "$ cicd dryrun .pipelines/allow-failure.yaml"
echo ""
cicd dryrun "$PROJECT_DIR/.pipelines/allow-failure.yaml"
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 3: Run pipeline with failures: true (coverage-report is allowed to fail)${NC}"
echo "$ cicd run --name allow-failure"
echo ""
cd "$PROJECT_DIR" && cicd run --name allow-failure
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 4: Report - inspect the test stage${NC}"
echo "$ cicd report --pipeline sprint6-allowed-failure-demo --run latest --stage test"
echo ""
LATEST_RUN=$(cd "$PROJECT_DIR" && cicd report --pipeline sprint6-allowed-failure-demo 2>/dev/null | grep "run-no:" | tail -1 | tr -d ' ' | cut -d: -f2)
cd "$PROJECT_DIR" && cicd report --pipeline sprint6-allowed-failure-demo --run "$LATEST_RUN" --stage test
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 5: Run pipeline with failures: false (critical-test blocks the pipeline)${NC}"
echo "$ cicd run --name block-failure"
echo ""
cd "$PROJECT_DIR" && cicd run --name block-failure
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 6: Report - compare with blocking failure${NC}"
echo "$ cicd report --pipeline sprint6-blocking-failure-demo --run latest --stage test"
echo ""
LATEST_RUN=$(cd "$PROJECT_DIR" && cicd report --pipeline sprint6-blocking-failure-demo 2>/dev/null | grep "run-no:" | tail -1 | tr -d ' ' | cut -d: -f2)
cd "$PROJECT_DIR" && cicd report --pipeline sprint6-blocking-failure-demo --run "$LATEST_RUN" --stage test
echo ""

echo -e "${GREEN}========================================"
echo -e "  Demo 1 Complete"
echo -e "========================================${NC}"
