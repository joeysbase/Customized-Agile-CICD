#!/bin/bash
# Demo 3: Kubernetes & Helm Deployment

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
echo -e "  Demo 3: Kubernetes & Helm"
echo -e "========================================${NC}"
echo ""

echo -e "${YELLOW}Step 1: Show the Helm release${NC}"
echo "$ helm list"
echo ""
helm list
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 2: Show all deployed pods${NC}"
echo "$ kubectl get pods"
echo ""
kubectl get pods
echo ""

echo "$ kubectl get svc"
echo ""
kubectl get svc
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 3: Run a pipeline and watch K8s job pods in real time${NC}"
echo ""
echo -e "${CYAN}Watching pods in background... (each job runs as a K8s pod)${NC}"
kubectl get pods -w &
WATCH_PID=$!
sleep 1

echo ""
echo "$ cicd run --name success"
echo ""
cd "$PROJECT_DIR" && cicd run --name success

kill $WATCH_PID 2>/dev/null
wait $WATCH_PID 2>/dev/null
echo ""

read -p "Press Enter to continue..."
echo ""

echo -e "${YELLOW}Step 4: Show Helm chart values (configuration as code)${NC}"
echo "$ helm get values cicd --all"
echo ""
helm get values cicd --all
echo ""

echo -e "${GREEN}========================================"
echo -e "  Demo 3 Complete"
echo -e "========================================${NC}"
