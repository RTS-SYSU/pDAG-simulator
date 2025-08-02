package uk.ac.york.mocha.simulator.allocation;

import java.util.List;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;

public class SimpleAllocationConversing extends AllocationMethods {

	@Override
	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> availableProcs, long[] procs, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<List<Node>> allocHistory,
			 long currentTime, boolean affinity, List<Node> etHist) {

		if (readyNodes.size() == 0 || availableProcs.size() == 0)
			return;

		/*
		 * Entry for debugging a single node
		 */
		for (Node n : readyNodes) {
			if (n.getDagID() == 0 && n.getDagInstNo() == 6 && n.getId() == 7) {
				break;
			}
		}

		readyNodes.stream().forEach(c -> c.partition = -1);
		

		for (int i = 0; i < availableProcs.size(); i++) {
			if (i >= readyNodes.size())
				break;

			readyNodes.get(i).partition = availableProcs.get(i);
		}
		
	}

}
