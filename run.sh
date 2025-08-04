#!/bin/bash

set -e

IMAGE_NAME="lzylearning/simulator:latest"
CONTAINER_NAME="simulator-instance"
RESULT_DIR_HOST="./result"
RESULT_DIR_CONTAINER="/pDAG-simulator/result"

check_container_running() {
  docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"
}

check_image_exists() {
  docker image inspect "$IMAGE_NAME" > /dev/null 2>&1
}

case "$1" in
  build)
    echo "🔨 Building Docker image..."
    docker build -t $IMAGE_NAME .
    ;;

  pull)
    echo "📡 Pulling Docker image..."
    docker pull $IMAGE_NAME
    ;;

  run)
    if ! check_image_exists; then
      echo "❌ Image not found. Please run: sudo ./run.sh build OR sudo ./run.sh pull"
      exit 1
    fi
    echo "🚀 Starting container..."
    docker run -dit --name $CONTAINER_NAME $IMAGE_NAME
    ;;

  deviation|cost|design|all)
    if ! check_container_running; then
      echo "❌ Container not running. Please start it with: sudo ./run.sh run"
      exit 1
    fi

    CMD="$*"
    echo "⚙️ Sending command to container: $CMD"
    # docker exec -i $CONTAINER_NAME sh -c "echo '$CMD' | /bin/bash -c 'cat > /dev/stdin' | java -cp 'lib/*:bin' Main"
    docker exec $CONTAINER_NAME java -cp "lib/*:bin" Main $*
    ;;

  draw)
    if ! check_container_running; then
      echo "❌ Container not running. Please start it with: sudo ./run.sh run"
      exit 1
    fi
    echo "🖼 Generating plots..."
    docker exec $CONTAINER_NAME python3 scripts/deviation_analysis_plot.py
    docker exec $CONTAINER_NAME python3 scripts/computation_cost_analysis_plot.py
    docker exec $CONTAINER_NAME python3 scripts/design_solution_analysis_plot.py
    docker cp $CONTAINER_NAME:$RESULT_DIR_CONTAINER/. $RESULT_DIR_HOST/
    echo "✅ Plots saved to $RESULT_DIR_HOST"
    ;;

  clean)
    echo "🧹 Cleaning up container..."
    docker rm -f $CONTAINER_NAME || true
    docker rmi -f $IMAGE_NAME || true
    ;;

  *)
    echo "Usage:"
    echo "    sudo ./run.sh build                     # Build docker image"
    echo "    sudo ./run.sh pull                      # Pull docker image"
    echo "    sudo ./run.sh run                       # Start container"
    echo "    sudo ./run.sh all                       # Run all experiments shown in the paper"
    echo "    sudo ./run.sh deviation psr 0.2 0.8     # Run DeviationAnalysis with specific params"
    echo "    sudo ./run.sh cost 2 10                 # Run ComputationCostAnalysis with specific params"
    echo "    sudo ./run.sh design 3 10               # Run DesignSolutionAnalysis with specific params"
    echo "    sudo ./run.sh draw                      # Generate plots and copy to result directory in host machine"
    echo "    sudo ./run.sh clean                     # Stop and remove container"
    ;;
esac

