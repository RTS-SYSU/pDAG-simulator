package uk.ac.york.mocha.simulator.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class OnlineCacheAwareWithOrdering extends AllocationMethods {

	@Override
	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> availableProcs, long[] availableTimeAllProcs, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<List<Node>> allocHistory, long currentTime,
			boolean affinity, List<Node> etHist) {

		/*
		 * Entry for debugging a single node
		 */
		// for (Node n : readyNodes) {
		// if (n.getDagID() == 0 && n.getDagInstNo() == 0 && n.getId() == 0) {
		// break;
		// }
		// }

		if (readyNodes.size() == 0 || availableProcs.size() == 0)
			return;

		readyNodes.stream().forEach(c -> c.partition = -1);

		/*
		 * Sort ready nodes list by FPS+WF, take first procNum nodes to allocate.
		 */
		readyNodes.sort((c1, c2) -> Utils.compareNodeWithPriority(dags, c1, c2));

		/**
		 * Allocation for hard real-time tasks
		 */
		// List<Integer> allocProcsHard = new ArrayList<>();
		// List<Node> allocNodesHard = new ArrayList<>();
		// List<Node> hardAsSoft = new ArrayList<>();
		//
		// for (int i = 0; i < readyNodes.size(); i++) {
		// Node n = readyNodes.get(i);
		//
		// if (n.priority > -1) {
		//
		// int offlineAlloc = n.offline_partition;
		// hardAsSoft.add(n);
		// if (availableProcs.contains(offlineAlloc) &&
		// !allocProcsHard.contains(offlineAlloc)) {
		// n.partition = offlineAlloc;
		//
		// allocProcsHard.add(offlineAlloc);
		// allocNodesHard.add(n);
		// } else { //if (!availableProcs.contains(offlineAlloc) &&
		// availableProcs.size() > 0)
		// Pair<Long, Integer> res = table.computeET(-1, history_level1,
		// history_level2, history_level3, n,
		// offlineAlloc, true, recency_fault, 0);
		//// long et_diff_wait = n.getWCET() - res.getFirst();
		// int cache = res.getSecond();
		//
		// if (cache != 1) {
		// hardAsSoft.add(n);
		// }
		//
		//// List<Integer> free_proc_update = new ArrayList<>();
		//// for(int j=0; j<availableProcs.size();j++) {
		//// if(!allocNodesHard.contains(availableProcs.get(j))) {
		//// free_proc_update.add(availableProcs.get(j));
		//// }
		//// }
		////
		//// List<Long> et_conserving = new ArrayList<>();
		//// for(int j=0; j< free_proc_update.size(); j++) {
		////
		//// }
		// }
		//
		// }
		//
		// }

		/**
		 * Allocation for soft real-time tasks
		 */
		// List<Node> softNodes = readyNodes.stream().filter(c -> c.priority ==
		// -1).collect(Collectors.toList());
		// List<Node> restNodes = new ArrayList<>();
		// restNodes.addAll(hardAsSoft);
		// restNodes.addAll(softNodes);
		//
		// List<Integer> softProcs = new ArrayList<>();
		// for (int i = 0; i < availableProcs.size(); i++) {
		// int proc = availableProcs.get(i);
		// if (!allocProcsHard.contains(proc)) {
		// softProcs.add(proc);
		// }
		// }

		List<Node> preEligible = new ArrayList<>();
		for (int i = 0; i < availableProcs.size(); i++) {
			if (readyNodes.size() == i)
				break;
			preEligible.add(readyNodes.get(i));
		}

		List<Integer> availableP = new ArrayList<>(availableProcs);

		List<List<Long>> speedUpTable = new ArrayList<>();

		for (Node n : preEligible) {
			List<Long> ETdrop = new ArrayList<>();

			for (int i = 0; i < history_level1.size(); i++) {
				int proc = i;
				if (availableP.contains(proc)) {
					/*
					 * Option 1: Speed up by ABSOLUTE vaue
					 */
					long WCET = n.getWCET();
					long realET = n.crp
							.computeET(-1, history_level1, history_level2, history_level3, n, proc, true, 0,0, false)
							.getFirst().getFirst();
					long speedup = WCET - realET;

					/*
					 * Option 2: Speed up by RELATIVE vaue
					 */
					// double speedup = ((double) (n.getWCET() -
					// table.computeET(history_level1,
					// history_level2, history_level3, n, proc, true))) /
					// (double) n.getWCET();

					/*
					 * Option 3: Cache-aware ET
					 */
					// long speedup = table.computeET(history_level1,
					// history_level2,
					// history_level3, n, proc, true);

					ETdrop.add(speedup);
				}
			}

			speedUpTable.add(ETdrop);
		}

		List<Integer> allocProcs = new ArrayList<>();
		List<Integer> allocNodes = new ArrayList<>();

		List<List<Node>> historyCut = new ArrayList<>();
		for (int i = 0; i < history_level1.size(); i++) {
			if (availableP.contains(i))
				historyCut.add(history_level1.get(i));
		}

		List<List<Node>> allocHistoryCut = new ArrayList<>();
		for (int i = 0; i < allocHistory.size(); i++) {
			if (availableP.contains(i))
				allocHistoryCut.add(allocHistory.get(i));
		}

		for (int k = 0; k < availableP.size(); k++) {
			if (k >= preEligible.size())
				break;

			Pair<Integer, Integer> p = setPartition(speedUpTable, allocNodes, allocProcs, allocHistoryCut, allocHistory,
					preEligible, availableP, availableTimeAllProcs, currentTime, affinity, history_level1,
					history_level2, history_level3);

			Node n = preEligible.get(p.getFirst().intValue());

			n.partition = availableP.get(p.getSecond().intValue());
//			n.expectedET = n.getWCET() - speedUpTable.get(p.getFirst()).get(p.getSecond());

			allocNodes.add(p.getFirst().intValue());
			allocProcs.add(p.getSecond().intValue());

		}

	}

	private Pair<Integer, Integer> setPartition(List<List<Long>> speedUpTable, List<Integer> allocNodes,
			List<Integer> allocProcs, List<List<Node>> allocHistory, List<List<Node>> fullAllocHistory,
			List<Node> preEligible, List<Integer> procs, long[] availableTimeAllProcs, long time, boolean affinity,
			List<List<Node>> history_level1, List<List<Node>> history_level2, List<Node> history_level3) {

		int row = -1;
		int col = -1;
		long max = Long.MIN_VALUE;

		for (int i = 0; i < speedUpTable.size(); i++) {
			if (!allocNodes.contains(i)) {
				for (int j = 0; j < speedUpTable.get(i).size(); j++) {
					if (!allocProcs.contains(j)) {
						if (max < speedUpTable.get(i).get(j)) {
							max = speedUpTable.get(i).get(j);
							row = i;
							col = j;
						}
					}

				}
			}
		}

		if (affinity) {
			Node n = preEligible.get(row);
			List<Integer> freeProcIndex = new ArrayList<>();
			List<Integer> freeProc = new ArrayList<>();
			List<Integer> freeCluster = new ArrayList<>();

			/**
			 * Find all available cores that can have the same speed up
			 */
			for (int i = 0; i < procs.size(); i++) {
				if (!allocProcs.contains(i) && speedUpTable.get(row).get(i) == max) {
					freeProcIndex.add(i);

					int proc = procs.get(i);
					freeProc.add(proc);

					int c = proc / 4;
					if (!freeCluster.contains(c))
						freeCluster.add(c);
				}
			}

			if (freeProcIndex.size() > 1) {

				/*
				 * Search in history for same node & DAG allocation
				 */
				List<List<Node>> NodeHis = new ArrayList<>();
				List<Long> impacts = new ArrayList<>();

				for (int i = 0; i < freeProcIndex.size(); i++) {
					NodeHis.add(new ArrayList<>());
					impacts.add((long) 0);
				}

				for (int i = 0; i < freeProcIndex.size(); i++) {
					int procIndex = freeProcIndex.get(i);
					long et_n = n.getWCET() - speedUpTable.get(row).get(procIndex);

					List<Node> nodesInProc = allocHistory.get(procIndex);

					/*
					 * Get the nodes that can hit level two cache in each free core.
					 */
					long Nodenum = 0;
					for (int j = nodesInProc.size() - 1; j >= 0; j--) {
						Nodenum += nodesInProc.get(j).finishAt - nodesInProc.get(j).start;

						if (Nodenum >= SystemParameters.v4) {
							break;
						}

						NodeHis.get(i).add(nodesInProc.get(j));
					}

					List<Node> affectedNodes = NodeHis.get(i);
					long affectedTime = 0;

					for (Node affected : affectedNodes) {
						long affectedTimeOneNode = affected.crp.computeET(-1, history_level1, history_level2,
								history_level3, affected, affected.partition, true, et_n, 0,false).getFirst().getFirst()
								- affected.crp.computeET(-1, history_level1, history_level2, history_level3, affected,
										affected.partition, true, 0,0, false).getFirst().getFirst();

						affectedTime += affectedTimeOneNode < 0 ? 0 : affectedTimeOneNode;

						if (affectedTime < 0) {
							System.err.println("CacheAwareAlloc.setPartition(): the affected time is less than 0!");
							System.exit(-1);
						}
					}

					impacts.set(i, affectedTime);
				}

				long minExecutionTime = Collections.min(impacts);
				int minETIndex = impacts.indexOf(minExecutionTime);

				col = freeProcIndex.get(minETIndex);

			}

		}

		if (row == -1 || col == -1) {
			System.err.println("SimpleCacheAware.getIndexOfMaximum(): Cannot find the max value!");

			System.exit(-1);
		}

		return new Pair<Integer, Integer>(row, col);
	}

}
