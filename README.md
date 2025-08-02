# DAG Execution Timing Simulator



## 🐳 Installing Docker (on Linux)

### System Requirements

- 64-bit Linux with kernel 3.10+
- Root access or a user with `sudo` privileges

### Method 1: Install via Official Repository (Ubuntu/Debian)

```sh
sudo apt update
sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt install -y docker-ce docker-ce-cli containerd.io
```

### Method 2: Quick Install via Script

```sh
sudo curl -fsSL https://get.docker.com | sudo sh
```

### Start Docker Service

```sh
sudo systemctl start docker
```





## 🚀 Automated Experiment Execution with Docker

This project uses Docker to automatically run a DAG-based timing simulation and generate experimental results and plots. The execution process includes:

1. **Checking for an existing image locally**
2. **If not found**, it tries to **pull from Docker Hub**
3. **If pull fails**, it **builds the image locally**
4. **Runs a container to execute experiments**
5. **Copies the results (PDFs, logs, etc.) to the host**
6. **Cleans up the container**

Run the experiment with:

```sh
sudo ./run.sh
```

### Run Modes

You can pass an optional argument to choose how the image is prepared:

- `build`: Always build locally
- `pull`: Always pull from Docker Hub
- `auto` *(default)*: Check local → Try pull → Then build

```sh
sudo ./run.sh build
```





## 🛠 Script Logic Explained

### Step 1: Check or prepare image

```bash
if image_exists_locally; then
  echo "✅ The image exists locally."
else
  case "$MODE" in
    build)
      build_image
      ;;
    pull)
      pull_image || { echo "❌ Pull failed. Exiting."; exit 1; }
      ;;
    auto)
      echo "🔍 Trying to pull image..."
      if pull_image; then
        echo "✅ Pull succeeded."
      else
        echo "⚠️ Pull failed. Building locally..."
        build_image
      fi
      ;;
    *)
      echo "❌ Invalid mode. Use 'build', 'pull', or 'auto'."
      exit 1
      ;;
  esac
fi
```

### Step 2: Run the container (executes Java + Python)

```bash
echo "🚀 Run the container and generate experimental figures..."
docker run --name $CONTAINER_NAME $IMAGE_NAME
```

### Step 3: Copy result and clean up

```bash
echo "📦 Copy the result to $RESULT_DIR_HOST"
mkdir -p $RESULT_DIR_HOST
docker cp $CONTAINER_NAME:$RESULT_DIR_CONTAINER/. $RESULT_DIR_HOST/

echo "🧹 Clean up the container..."
docker rm $CONTAINER_NAME
```





## 📦 Project Overview

This project is a Java-based **DAG execution timing simulation framework**. It consists of:

- A DAG task framework (nodes, dependencies, etc.)
- A simulator framework
- A hardware framework (caches, history tables, etc.)

All p-DAG experiments are under the package:
 `uk.ac.york.mocha.simulator.experiments_pDAG`

### Key Experiment Classes

1. **`AllExprs`** – Contains all experiments, including those not shown in the paper. For clarity, the experiments published in the paper are separated into the following three classes.
2. **`DeviationAnalysis`** – Implements **Experiment 1**. Runs 500 trials under different `psr`, `parallelism`, and `number of conditional structures`. Tracks deviation and analysis time.
3. **`ComputationCostAnalysis`** – Implements **Experiment 2**. Measures analysis time difference between the proposed method and enumeration-based method under different conditional structure counts.
4. **`DesignSolutionAnalysis`** – Implements **Experiment 3**. Measures the **minimum required number of cores** for different acceptance thresholds across three approaches.



## 📊 Output & Visualization

- **Experiment results** are saved in the `result/` directory.
- Then, the Python scripts under `scripts/` will **generate plots as PDFs**.
- The generated PDFs and result logs will be **copied to your host machine**.

> ⚠️ *Note*: The figures may look different from those in the paper, because the publication figures were manually styled. However, the **data used is identical**.



## ⏳ Warn Remainder

TODO: 加个时间预估

Due to the scale and number of conditional structures tested, the full experiment can take **over a day** to complete. Please be patient and ensure your machine has sufficient resources.



## 📬 Contact

Project Maintainers:

- **Shuai Zhao** — alice@example.com  
- **Yiyang Gao** — [gaoyy26@mail2.sysu.edu.cn]()
- **Zhiyang Lin** — [linzhy78@mail2.sysu.edu.cn]()

