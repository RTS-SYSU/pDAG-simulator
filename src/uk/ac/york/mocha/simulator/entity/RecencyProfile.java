package uk.ac.york.mocha.simulator.entity;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;

public abstract class RecencyProfile  implements Serializable {

	
	private static final long serialVersionUID = -6902741116934537381L;
	
	public CacheHierarchy cache;
	public RecencyType type;

	public RecencyProfile(CacheHierarchy cache, RecencyType type) {
		this.cache = cache;
		this.type = type;
	}

	public abstract Pair<Pair<Long, Double>, Integer> computeET(long time, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, Node n, int proc, boolean cacheAware,
			long additionalTime, long variation, boolean error);

	public long getTimeofLastIndex(List<Node> nodes, Node n, double bound) {

		long time = 0;
		
		

		for (int i = nodes.size() - 1; i >= 0; i--) {
			if (nodes.get(i).getDagID() == n.getDagID() && nodes.get(i).getId() == n.getId()) {
//				time = time + nodes.get(i).expectedET;
				return time;
			} else {
				time = time + nodes.get(i).expectedET;
				if (time > bound)
					return time;
			}
		}

		return -1;
	}

	public int getNodeLastIndex(List<Node> nodes, Node n) {

		for (int i = nodes.size() - 1; i >= 0; i--) {
			if (nodes.get(i).getDagID() == n.getDagID() && nodes.get(i).getId() == n.getId())
				return i;
		}

		return -1;
	}

	public int compareNodeForRecency(Node n1, Node n2, Node current) {

		int compare = -Long.compare(n1.finishAt, n2.finishAt);

		if (compare == 0) {
			if (n1.getFullName().equals(current.getFullName()))
				compare = -1;
			if (n2.getFullName().equals(current.getFullName()))
				compare = 1;
		}

		return compare;
	}
	
	public RecencyType getType() {
		return type;
	}

}
