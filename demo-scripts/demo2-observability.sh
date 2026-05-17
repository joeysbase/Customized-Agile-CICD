#!/bin/bash
# Demo 2: Observability - Metrics, Logs, and Traces

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
echo -e "  Demo 2: Observability"
echo -e "  Metrics | Logs | Traces"
echo -e "========================================${NC}"
echo ""

echo -e "${YELLOW}Step 1: Run a pipeline to generate observability data${NC}"
echo "$ cicd run --name success"
echo ""
cd "$PROJECT_DIR" && cicd run --name success
echo ""

read -p "Press Enter to open Grafana dashboards..."
echo ""

echo -e "${CYAN}Opening Grafana at http://localhost:3000${NC}"
open "http://localhost:3000" 2>/dev/null

echo ""
echo -e "${YELLOW}Walk through the following dashboards:${NC}"
echo "  1. Pipeline Overview    -> run counts, success/failure distribution, duration trends"
echo "  2. Stage/Job Breakdown  -> per-stage and per-job durations"
echo "  3. Logs Viewer          -> structured logs filtered by pipeline/run"
echo "  4. Trace Explorer       -> click a trace to see full span hierarchy"
echo ""

read -p "Press Enter to finish demo..."
echo ""

echo -e "${GREEN}========================================"
echo -e "  Demo 2 Complete"
echo -e "========================================${NC}"
