# Use a basic image that includes Java env
FROM openjdk:17-slim AS builder

# set work directory
WORKDIR /pDAG-simulator

# copy project files oto container
COPY lib/ ./lib/
COPY src/ ./src/
COPY scripts/ ./scripts/
COPY result/ ./result/

# install Python, pip, and install related dependencies
RUN apt-get update && apt-get install -y python3 python3-pip && pip3 install -r scripts/requirements.txt

# compile Java source code
RUN find ./src -name "*.java" > sources.txt && \
    mkdir -p bin && \
    javac -cp "lib/*" -d bin @sources.txt

# set default startup command: Run Java main class and Python script to generate experimental image
#CMD java -cp "lib/*:bin" Main && \
#    python3 scripts/deviation_analysis_plot.py && \
#    python3 scripts/computation_cost_analysis_plot.py && \
#    python3 scripts/design_solution_analysis_plot.py && \
#    ls -l /pDAG-simulator/result

#CMD ["java", "-cp", "lib/*:bin", "Main"]
CMD [ "tail", "-f", "/dev/null" ]