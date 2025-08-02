package uk.ac.york.mocha.simulator.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class OnlineCacheAwareRobust_v2_1 extends AllocationMethods {

//	@Override
//	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue, List<Integer> cores,
//			long[] coreTime, List<List<Node>> history_level1, List<List<Node>> history_level2, List<Node> history_level3,
//			List<List<Node>> allocHistory, RecencyProfile table, long systemTime, boolean lcif) {
//
//		List<Integer> availableCores = new ArrayList<>();
//		for (int i = 0; i < cores.size(); i++) {
//			if (localRunqueue.get(i).size() == 0 && coreTime[i] <= systemTime)
//				availableCores.add(i);
//		}
//
//		List<String> dagID = new ArrayList<>();
//		for (Node n : readyNodes) {
//			if (!dagID.contains(n.getDagID() + "_" + n.getDagInstNo()))
//				dagID.add(n.getDagID() + "_" + n.getDagInstNo());
//		}
//
//		dagID.sort((c1, c2) -> Utils.compareDAG(dags, Integer.parseInt(c1.split("_")[0]), Integer.parseInt(c1.split("_")[1]),
//				Integer.parseInt(c2.split("_")[0]), Integer.parseInt(c2.split("_")[1])));
//
//		List<List<Node>> nodesByDAG = new ArrayList<>();
//		for (int i = 0; i < dagID.size(); i++) {
//			nodesByDAG.add(new ArrayList<>());
//		}
//
//		/// **** ///
//	}

	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> cores, long[] coreTime, List<List<Node>> history_level1, List<List<Node>> history_level2,
			List<Node> history_level3, List<List<Node>> allocHistory, long systemTime, boolean lcif, List<Node> etHist) {

		if (readyNodes.size() == 0)
			return;

		readyNodes.stream().forEach(c -> c.partition = -1);

		/**************************************************************************************************
		 ************************************ Initialize the dispatcher ***********************************
		 *************************************************************************************************/
		/* Sort ready nodes list by DAG priority and then node WCET */
		readyNodes.sort((c1, c2) -> Utils.compareNode(dags, c1, c2));

		// for (List<Node> l : localRunqueue) {
		// readyNodes.addAll(l);
		// l.clear();
		// }

		for (Node n : readyNodes)
			n.expectedETPerCore = new long[cores.size()];

		List<Integer> allocProcs = new ArrayList<>();
		List<Integer> allocNodes = new ArrayList<>();

		// List<List<Node>> level1 = new ArrayList<>();
		// for(List<Node> l : allocHistory)
		// level1.add(new ArrayList<>(l));
		//
		// List<List<Node>> level2 =
		// Utils.getAllocHistoryByLevel2Cache(allocHistory);
		// List<Node> level3 =
		// allocHistory.stream().flatMap(List::stream).collect(Collectors.toList());

		List<Integer> availableCores = new ArrayList<>();
		for (int i = 0; i < cores.size(); i++) {
			if (localRunqueue.get(i).size() == 0 && coreTime[i] <= systemTime)
				availableCores.add(i);
		}
		/**************************************************************************************************
		 **************************************************************************************************
		 **************************************************************************************************/

		while (readyNodes.size() != allocNodes.size()) {
			List<List<Long>> speedUpTable = computeSpeedUp(readyNodes, cores, coreTime, systemTime, localRunqueue,
					history_level1, history_level2, history_level3);

			Pair<Integer, Integer> p = setPartition(speedUpTable, allocNodes, allocProcs, readyNodes, cores, coreTime,
					systemTime, lcif, history_level1, history_level2, history_level3);

			Node n = readyNodes.get(p.getFirst().intValue());

			n.partition = cores.get(p.getSecond().intValue());
			n.expectedET = n.expectedETPerCore[n.partition];

			allocNodes.add(p.getFirst().intValue());
			allocProcs.add(p.getSecond().intValue());

			localRunqueue.get(n.partition).add(n);

			// level1.get(n.partition).add(n);
			// int clusterID = n.partition / SystemParameters.Level2CoreNum;
			// level2.get(clusterID).add(n);
			// level3.add(n);

		}

		readyNodes.clear();

	}

	private Pair<Integer, Integer> setPartition(List<List<Long>> speedUpTable, List<Integer> allocNodes,
			List<Integer> allocProcs, List<Node> readyNodes, List<Integer> cores, long[] coreTime, long systemTime,
			boolean lcif, List<List<Node>> history_level1, List<List<Node>> history_level2, List<Node> history_level3) {

		int row = -1;
		int col = -1;
		long max = Long.MIN_VALUE;

		for (int i = 0; i < speedUpTable.size(); i++) {
			if (!allocNodes.contains(i)) {
				for (int j = 0; j < speedUpTable.get(i).size(); j++) {
					if (max < speedUpTable.get(i).get(j)) {
						max = speedUpTable.get(i).get(j);
						row = i;
						col = j;
					}
				}
			}
		}

		if (lcif) {
			Node n = readyNodes.get(row);
			List<Integer> freeProcIndex = new ArrayList<>();
			List<Integer> freeProc = new ArrayList<>();
			List<Integer> freeCluster = new ArrayList<>();

			/**
			 * Find all available cores that can have the same speed up
			 */
			for (int i = 0; i < cores.size(); i++) {
				if (speedUpTable.get(row).get(i) == max) {
					freeProcIndex.add(i);

					int proc = cores.get(i);
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
					long et_n = n.expectedETPerCore[procIndex];

					List<Node> nodesInProc = history_level1.get(procIndex);

					/*
					 * Get the nodes that can hit level two cache in each free core.
					 */
					long Nodenum = 0;
					for (int j = nodesInProc.size() - 1; j >= 0; j--) {
						Nodenum += nodesInProc.get(j).expectedET;

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
										affected.partition, true, 0, 0,false).getFirst().getFirst();

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

	private List<List<Long>> computeSpeedUp(List<Node> readyNodes, List<Integer> cores, long[] coreTime,
			long systemTime, List<List<Node>> localRunqueue, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3) {

		List<List<Long>> speedUpTable = new ArrayList<>();

		for (int i = 0; i < readyNodes.size(); i++) {

			Node n = readyNodes.get(i);

			List<Long> ETdrop = new ArrayList<>();

			for (int core = 0; core < cores.size(); core++) {

				final int k = core;
				/*
				 * Option 1: Speed up by ABSOLUTE value
				 */
				long WCET = n.getWCET();
				long estimatedET = n.crp
						.computeET(-1, history_level1, history_level2, history_level3, n, core, true,
								localRunqueue.get(core).stream().mapToLong(c -> c.expectedETPerCore[k]).sum(),0, false)
						.getFirst().getFirst();
				long speedup = WCET - estimatedET;
				n.expectedETPerCore[core] = estimatedET;

				long waitforCore = coreTime[core] > systemTime ? coreTime[core] - systemTime : 0;
				long waitforReady = localRunqueue.get(core).stream().mapToLong(x -> x.expectedET).sum();

				speedup = speedup - waitforCore - waitforReady;

				ETdrop.add(speedup);

			}

			speedUpTable.add(ETdrop);
		}

		return speedUpTable;
	}

}