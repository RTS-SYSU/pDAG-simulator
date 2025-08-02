package uk.ac.york.mocha.simulator.allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;

public class OnlineCacheAwareReverse extends AllocationMethods {

	@Override
	public void allocate(List<DirectedAcyclicGraph> dags, List<Node> readyNodes, List<List<Node>> localRunqueue,
			List<Integer> availableProcs, long[] availableTimeAllProcs, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<List<Node>> allocHistory, long currentTime,
			boolean lcif, List<Node> etHist) {

		/*
		 * Entry for debugging a single node
		 */
		for (Node n : readyNodes) {
			if (n.getDagID() == 0 && n.getDagInstNo() == 1 && n.getId() == 0) {
				break;
			}
		}

		if (readyNodes.size() == 0 || availableProcs.size() == 0)
			return;

		readyNodes.stream().forEach(c -> c.partition = -1);

		/*
		 * Sort ready nodes list by FPS+WF, take first procNum nodes to allocate.
		 */
//		readyNodes.sort((c1, c2) -> Utils.compareNode(dags, c1, c2));

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
					 * Option 1: Speed up by ABSOLUTE value
					 */
					long WCET = n.getWCET();
					long realET = n.crp
							.computeET(-1, history_level1, history_level2, history_level3, n, proc, true, 0, 0,false)
							.getFirst().getFirst();
					long speedup = WCET - realET;

					/*
					 * Option 2: Speed up by RELATIVE value
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
					preEligible, availableP, availableTimeAllProcs, currentTime, lcif, history_level1, history_level2,
					history_level3);

			Node n = preEligible.get(p.getFirst().intValue());

			n.partition = availableP.get(p.getSecond().intValue());

			allocNodes.add(p.getFirst().intValue());
			allocProcs.add(p.getSecond().intValue());

		}

	}

//	private List<List<Long>> computeSpeedUp(List<Node> readyNodes, List<Integer> cores, RecencyProfile table,
//			long[] coreTime, long systemTime, List<List<Node>> localRunqueue, List<List<Node>> history_level1,
//			List<List<Node>> history_level2, List<Node> history_level3) {
//
//		List<List<Long>> speedUpTable = new ArrayList<>();
//
//		for (int i = 0; i < readyNodes.size(); i++) {
//
//			Node n = readyNodes.get(i);
//
//			List<Long> ETdrop = new ArrayList<>();
//
//			for (int core = 0; core < cores.size(); core++) {
//
//				/*
//				 * Option 1: Speed up by ABSOLUTE value
//				 */
//				long WCET = n.getWCET();
//				long realET = table
//						.computeET(-1, history_level1, history_level2, history_level3, n, core, true, 0, false)
//						.getFirst();
//				long speedup = WCET - realET;
//
//				long waitforCore = coreTime[core] > systemTime ? coreTime[core] - systemTime : 0;
//				long waitforReady = localRunqueue.get(core).stream().mapToLong(x -> x.expectedET).sum();
//
////				for (int i = 0; i < speedUpTable.size(); i++) {
////				long speedUp = speedUpTable.get(i).get(n.partition) - n.expectedET;
////				speedUpTable.get(i).set(n.partition, speedUp);
////			}
//
//				speedup = speedup - waitforCore - waitforReady;
//
//				ETdrop.add(speedup);
//
//			}
//
//			speedUpTable.add(ETdrop);
//		}
//
//		return speedUpTable;
//	}

	private Pair<Integer, Integer> setPartition(List<List<Long>> speedUpTable, List<Integer> allocNodes,
			List<Integer> allocProcs, List<List<Node>> allocHistory, List<List<Node>> fullAllocHistory,
			List<Node> preEligible, List<Integer> procs, long[] availableTimeAllProcs, long time, boolean lcif,
			List<List<Node>> history_level1, List<List<Node>> history_level2, List<Node> history_level3) {

		int row = -1;
		int col = -1;
		long min = Long.MAX_VALUE;

		for (int i = 0; i < speedUpTable.size(); i++) {
			if (!allocNodes.contains(i)) {
				for (int j = 0; j < speedUpTable.get(i).size(); j++) {
					if (!allocProcs.contains(j)) {
						if (min > speedUpTable.get(i).get(j)) {
							min = speedUpTable.get(i).get(j);
							row = i;
							col = j;
						}
					}

				}
			}
		}

		if (lcif) {
			Node n = preEligible.get(row);
			List<Integer> freeProcIndex = new ArrayList<>();
			List<Integer> freeProc = new ArrayList<>();
			List<Integer> freeCluster = new ArrayList<>();

			/**
			 * Find all available cores that can have the same speed up
			 */
			for (int i = 0; i < procs.size(); i++) {
				if (!allocProcs.contains(i) && speedUpTable.get(row).get(i) == min) {
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

			// if (freeProcIndex.size() > 1) {
			// /*
			// * Search in history for same node & DAG allocation
			// */
			// List<Long> NodeHis = new ArrayList<>();
			//
			// for (int i = 0; i < freeProcIndex.size(); i++)
			// NodeHis.add((long) 0);
			//
			// for (int i = 0; i < freeProcIndex.size(); i++) {
			// int procIndex = freeProcIndex.get(i);
			//
			// List<Node> nodesInProc = allocHistory.get(procIndex);
			//
			// long Nodenum = 0;
			// for (Node nh : nodesInProc)
			// Nodenum += nh.finishAt - nh.start;
			//
			// NodeHis.set(i, Nodenum);
			// }
			//
			// long minExecutionTime = Collections.min(NodeHis);
			// int minETIndex = NodeHis.indexOf(minExecutionTime);
			//
			// col = freeProcIndex.get(minETIndex);
			// }

		}

		if (row == -1 || col == -1) {
			System.err.println("SimpleCacheAware.getIndexOfMaximum(): Cannot find the max value!");

			System.exit(-1);
		}

		return new Pair<Integer, Integer>(row, col);
	}

}

/****************************************************************************************************************************
 ******************************************************* OLD VERSIONS *******************************************************
 ****************************************************************************************************************************/

/******************************************************************************
 * v0.2 ***********************************************************************
 * 
 * @Override public void getEligibileNode(List<DirectedAcyclicGraph> dags,
 *           List<Node> readyNodes, List<Integer> availableProcs,
 *           List<List<Node>> history, Recency table) {
 *
 *           if (readyNodes.size() == 0 || availableProcs.size() == 0) return;
 *
 *           readyNodes.stream().forEach(c -> c.partition = -1);
 *
 *           readyNodes.sort((c1, c2) -> compareNode(dags, c1, c2));
 *
 *           List<Node> preEligible = new ArrayList<>();
 *
 *           for (Node n : readyNodes) { if (n.getDagID() == 1 &&
 *           n.getDagInstNo() == 2 && n.getId() == 5) {
 *           System.out.println("check"); break; } }
 *
 *           for (int i = 0; i < availableProcs.size(); i++) { if (i >=
 *           readyNodes.size()) break; preEligible.add(readyNodes.get(i)); }
 *
 *           List<Integer> availableP = new ArrayList<>(availableProcs);
 *
 *           for (Node n : preEligible) { List<Long> ETdrop = new ArrayList<>();
 *
 *           for (int i = 0; i < availableP.size(); i++) { int proc =
 *           availableP.get(i);
 *
 *           ETdrop.add(n.getWCET() - table.computeET(history, n, proc, true));
 *           }
 *
 *           int procIndex = getIndexOfMaximum(ETdrop); n.partition =
 *           availableP.get(procIndex); availableP.remove(procIndex); } }
 *
 *           private int getIndexOfMaximum(List<Long> l) { int index = -1; long
 *           max = Long.MIN_VALUE;
 *
 *
 *           for (int i = 0; i < l.size(); i++) { if (max < l.get(i)) { max =
 *           l.get(i); index = i; } }
 *
 *           if (index == -1) System.out.println();
 *
 *           return index; }
 *
 ***********************************************************************************/

/******************************************************************************
 * v0.1 ***********************************************************************
 * 
 * @Override public void getEligibileNode(List<DirectedAcyclicGraph> dags,
 *           List<Node> readyNodes, List<Integer> availableProcs,
 *           List<List<Node>> history, Recency table) {
 * 
 *           if (readyNodes.size() == 0) return;
 * 
 *           readyNodes.stream().forEach(c -> c.partition = -1);
 * 
 * 
 *           sort ready nodes list by FPS+WF, take first procNum nodes to
 *           execute.
 * 
 *           readyNodes.sort((c1, c2) -> compareNode(dags, c1, c2));
 * 
 *           if (availableProcs.size() == 1 || readyNodes.size() == 1) {
 *           readyNodes.get(0).partition = availableProcs.get(0); }
 * 
 *           List<Node> preEligible = new ArrayList<>();
 * 
 *           for (int i = 0; i < availableProcs.size(); i++) { if (i >=
 *           readyNodes.size()) break; preEligible.add(readyNodes.get(i)); }
 * 
 *           List<Node> eligibile = new ArrayList<>();
 * 
 *           for (int i = 0; i < availableProcs.size(); i++) { if
 *           (preEligible.size() == 0) break;
 * 
 *           int proc = availableProcs.get(i);
 * 
 *           List<Long> ETdrop = preEligible.stream().map(n -> (n.getWCET() -
 *           table.computeET(history, n, proc, true)))
 *           .collect(Collectors.toList());
 * 
 *           int maxIndex = getIndexOfMaximum(ETdrop);
 * 
 *           preEligible.get(maxIndex).partition = availableProcs.get(i);
 * 
 *           eligibile.add(preEligible.get(maxIndex));
 *           preEligible.remove(maxIndex);
 * 
 * 
 *           }
 * 
 * 
 *           }
 ***********************************************************************************/