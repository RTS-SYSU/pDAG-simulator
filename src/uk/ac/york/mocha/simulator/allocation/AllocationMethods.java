package uk.ac.york.mocha.simulator.allocation;

import java.util.List;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;

public abstract class AllocationMethods {

//	protected abstract int compareNode(List<DirectedAcyclicGraph> dags, Node c1, Node c2);

	public abstract void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes,
			List<List<Node>> localRunqueue, List<Integer> availableCores, long[] coreTimes,
			List<List<Node>> history_level1, List<List<Node>> history_level2, List<Node> history_level3,
			List<List<Node>> allocNodes, long sysTime, boolean lcif, List<Node> etHist);

}
