package uk.ac.york.mocha.simulator.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class OnlineWFDNewSimu extends AllocationMethods {

	@Override
	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> cores, long[] availableTimeAllProcs, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<List<Node>> allocHistory, long currentTime,
			boolean lcif, List<Node> etHist) {

		List<Integer> availableCores = new ArrayList<>();
		for (int i = 0; i < cores.size(); i++) {
			if (localRunqueue.get(i).size() == 0 && availableTimeAllProcs[i] <= currentTime)
				availableCores.add(i);
		}
		
		if (readyNodes.size() == 0 || availableCores.size() == 0)
			return;

		readyNodes.stream().forEach(c -> c.partition = -1);

		/*
		 * Sort ready nodes list by FPS+WF, take first procNum nodes to allocate.
		 */
		readyNodes.sort((c1, c2) -> Utils.compareNode(dags, c1, c2));

		
		List<Integer> freeProc = new ArrayList<>(availableCores);
		
		for (int i = 0; i < availableCores.size(); i++) {
			if (i >= readyNodes.size())
				break;

			int core = getCoreIndexWithMinialWorkload(freeProc, history_level1);
			readyNodes.get(i).partition = core;
			freeProc.remove(freeProc.indexOf(core));
			
			localRunqueue.get(readyNodes.get(i).partition).add(readyNodes.get(i));
			allocHistory.get(readyNodes.get(i).partition).add(readyNodes.get(i));
		}
	
		for (int i = 0; i < readyNodes.size(); i++) {
			if (readyNodes.get(i).partition > -1) {
				readyNodes.remove(i);
				i--;
			}
		}

	}

	
	public int getCoreIndexWithMinialWorkload(List<Integer> freeProc, List<List<Node>> history_level1) {

		List<Long> accumaltedWorkload = new ArrayList<>();

		for (int i = 0; i < freeProc.size(); i++) {
			List<Node> nodeHis = history_level1.get(freeProc.get(i));

			long accumated = nodeHis.stream().mapToLong(c -> c.finishAt - c.start).sum();

			accumaltedWorkload.add(accumated);
		}

		long minWorkload = Collections.min(accumaltedWorkload);
		int coreIndex = accumaltedWorkload.indexOf(minWorkload);

		return freeProc.get(coreIndex);
	}

}
