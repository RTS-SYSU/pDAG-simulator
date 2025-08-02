package uk.ac.york.mocha.simulator.allocation;

import java.util.List;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;

public class SimpleAllocation extends AllocationMethods {

	@Override
	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> availableProcs, long[] procs, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<List<Node>> allocHistory, long currentTime,
			boolean affinity, List<Node> etHist) {

		if (readyNodes.size() == 0 || availableProcs.size() == 0)
			return;

		int coreIndex = 0;
		for (int i = 0; i < readyNodes.size(); i++) {
			readyNodes.get(i).partition = coreIndex;

			allocHistory.get(coreIndex).add(readyNodes.get(i));

			localRunqueue.get(coreIndex).add(readyNodes.get(i));

//			coreIndex++;
			if (coreIndex >= availableProcs.size())
				coreIndex = 0;

		}

		readyNodes.clear();
	}

}
