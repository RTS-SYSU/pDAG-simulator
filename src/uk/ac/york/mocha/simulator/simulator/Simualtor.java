package uk.ac.york.mocha.simulator.simulator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.allocation.AllocationMethods;
import uk.ac.york.mocha.simulator.allocation.OnlineAndOffline;
import uk.ac.york.mocha.simulator.allocation.OnlineCacheAware;
import uk.ac.york.mocha.simulator.allocation.OnlineCacheAwareReverse;
import uk.ac.york.mocha.simulator.allocation.OnlineCacheAwareRobust_v2_1;
import uk.ac.york.mocha.simulator.allocation.OnlineCacheAwareWithOrdering;
import uk.ac.york.mocha.simulator.allocation.OnlineCacheAware_ForCompare;
import uk.ac.york.mocha.simulator.allocation.OnlineFFD;
import uk.ac.york.mocha.simulator.allocation.OnlineRandom;
import uk.ac.york.mocha.simulator.allocation.OnlineWFD;
import uk.ac.york.mocha.simulator.allocation.OnlineWFWithOrdering;
import uk.ac.york.mocha.simulator.allocation.SimpleAllocationConversing;
import uk.ac.york.mocha.simulator.allocation.onlineBFD;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.entity.Node.NodeType;
import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Allocation;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Hardware;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.SimuType;

/*
 * This is a Multiprocessor Non-preemptive Multi-DAG Simulator
 */

public class Simualtor {

	private SimuType type;
	private Hardware hardware;
	private Allocation alloc;

	/**********************************************************************
	 ************************ DAGs to be executed *************************
	 **********************************************************************/
	public List<DirectedAcyclicGraph> dags;
	public CacheHierarchy cache;

	/**********************************************************************
	 *********************** Current system time **************************
	 **********************************************************************/
	private long systemTime = 0;

	/**********************************************************************
	 ************************* Runtime queues *****************************
	 **********************************************************************/
	/* a sleeping queue for DAGs waiting to be RELEASED */
	List<DirectedAcyclicGraph> sleepingDAGs;

	/* a ready queue for DAGs waiting to be EXECUTED */
	List<DirectedAcyclicGraph> readyDAGs;

	/* a ready queue for the nodes waiting to be EXECUTED */
	List<Node> readyNodes;

	/* a run queue for EXECUTING nodes */
	Node[] currentExe;

	/* the next available time of each processor */
	long[] allProcs;

	/* a list recording all executed nodes on each processor */
	List<List<Node>> allocNodes;

	/* Control for enable/disable week cluster-level affinity */
	boolean lcif;

	/* Execution history by cache level */
	List<List<Node>> history_level1;
	List<List<Node>> history_level2;
	List<Node> history_level3;

	/* A list containg allocation history, for printing and debugging. */
	List<String[]> allocHistory;

	/* cache performance */
	long[] cachePerformance;
	int totalAccess = 0;

	DecimalFormat df = new DecimalFormat("#.###");

	public long totalMakespan = -1;

	/********************* Runtime queues *********************************/

	public Simualtor(SimuType type, Hardware hardware, Allocation alloc, RecencyType recency,
			List<DirectedAcyclicGraph> dags, CacheHierarchy cache, int procNum, int recencySeed, boolean lcif) {

		this.cache = cache;
		this.type = type;
		this.hardware = hardware;
		this.alloc = alloc;

		this.dags = new ArrayList<>(dags);

		this.sleepingDAGs = new ArrayList<>(dags);
		sleepingDAGs.sort((c1, c2) -> Long.compare(c1.releaseTime, c2.releaseTime));
		this.readyDAGs = new ArrayList<>();
		this.readyNodes = new ArrayList<>();
		this.currentExe = new Node[procNum];
		this.allProcs = new long[procNum];
		this.lcif = lcif;

		this.history_level1 = new ArrayList<>();
		this.history_level2 = new ArrayList<>();
		this.history_level3 = new ArrayList<>();

		this.allocNodes = new ArrayList<>();
		this.allocHistory = new ArrayList<>();

		for (int i = 0; i < procNum; i++) {
			List<Node> oneProc = new ArrayList<>();
			this.history_level1.add(oneProc);

			List<Node> oneProcAlloc = new ArrayList<>();
			this.allocNodes.add(oneProcAlloc);
		}

		for (int i = 0; i < cache.level2.size(); i++) {
			List<Node> oneCluster = new ArrayList<>();
			this.history_level2.add(oneCluster);
		}

		this.cachePerformance = new long[4];

	}

	public Pair<List<DirectedAcyclicGraph>, double[]> simulate(boolean printSim) {
		return simulate(printSim, 0);
	}

	public Pair<List<DirectedAcyclicGraph>, double[]> simulate(boolean printSim, int onlyCritical) {

		/*
		 * Reset Run-time parameters of DAGs and their nodes
		 */
		for (DirectedAcyclicGraph dag : dags) {
			dag.reset();
		}

		boolean cacheAware = false;
		AllocationMethods allocM = null;

		switch (alloc) {
			case SIMPLE:
				allocM = new SimpleAllocationConversing();
				break;
			case RANDOM:
				allocM = new OnlineRandom();
				break;
			case BEST_FIT:
				allocM = new onlineBFD();
				break;
			case WORST_FIT:
				allocM = new OnlineWFD();
				break;
			case FIRST_FIT:
				allocM = new OnlineFFD();
				break;
			case CACHE_AWARE:
				allocM = new OnlineCacheAware();
				break;
			case CACHE_AWARE_RESERVE:
				allocM = new OnlineCacheAwareReverse();
				break;
			case CACHE_AWARE_ROBUST_v2_1:
				allocM = new OnlineCacheAwareRobust_v2_1();
				break;
			case OFFLINE_CACHE_AWARE:
				allocM = new OnlineAndOffline();
				break;
			case CACHE_AWARE_OUR:
				Utils.assignPriorityOur(dags);
				allocM = new OnlineCacheAwareWithOrdering();
				break;
			case WORST_FIT_OUR:
				Utils.assignPriorityOur(dags);
				allocM = new OnlineWFWithOrdering();
				break;
			case CACHE_AWARE_COMPARE:
				allocM = new OnlineCacheAware_ForCompare();
				break;
			default:
				System.err.println("The simualtion method is NOT supported ! ");
				System.exit(-1);
				return null;
		}

		switch (hardware) {
			case PROC:
				cacheAware = false;
				break;
			case PROC_CACHE:
				cacheAware = true;
			default:
				break;
		}

		debug_output_begin(printSim);

		while (sleepingDAGs.size() > 0 || readyDAGs.size() > 0) {

			/*
			 * Update the maintained list by time
			 */
			UpdateSystemStatus();

			/*
			 * Get all available processors at the current time
			 */
			List<Integer> availableProc = getAvailableCores();

			/*
			 * Execute ready nodes on available processors
			 */
			ExecuteReadyNodes(availableProc, allocM, cacheAware, onlyCritical, printSim);

			if (sleepingDAGs.size() == 0 && readyDAGs.size() == 0)
				totalMakespan = systemTime;

			/*
			 * advance to next time unit.
			 */
			advance();
		}

		debug_output_end(printSim);

		/*
		 * Summarise finish time of each DAG instance
		 */
		// List<Long> duration = dags.stream().map(c -> (c.finishTime -
		// c.startTime)).collect(Collectors.toList());
		// List<Long> makespan = dags.stream().map(c ->
		// c.getFlatNodes().stream().mapToLong(c1 -> c1.finishAt -
		// c1.start).sum()).collect(Collectors.toList());
		// List<Long> finish = dags.stream().map(c ->
		// (c.finishTime)).collect(Collectors.toList());
		//
		// List<List<Long>> res = new ArrayList<>();
		// res.add(duration);
		// res.add(makespan);
		// res.add(finish);

		List<DirectedAcyclicGraph> result = new ArrayList<>();

		for (DirectedAcyclicGraph d : dags) {
			result.add(d.deepCopy());
		}

		double[] cachePerf = new double[cachePerformance.length];
		for (int i = 0; i < cachePerformance.length; i++) {
			double d = (double) cachePerformance[i] / (double) totalAccess;
			cachePerf[i] = Double.parseDouble(df.format(d));
		}

		return new Pair<>(result, cachePerf);

	}

	/******************************************************************
	 ********** Choose the next node in the queue to execute **********
	 ******************************************************************/
	private void ExecuteReadyNodes(List<Integer> availableProc, AllocationMethods allocM, boolean cacheAware,
			int onlyCritical, boolean printSim) {

		for (Node n : readyNodes) {
			if (n.getDagID() == 0 && n.getDagInstNo() == 0 && n.getId() == 5) {
				break;
			}
		}

		/*
		 * get ready nodes to execute by the specified allocation method
		 */
		allocM.allocate(dags, readyNodes, null, availableProc, allProcs, history_level1, history_level2, history_level3,
				allocNodes, systemTime, lcif, null);

		///////////////// Debug Output //////////////////////
		String[] oneSched = new String[allProcs.length];
		for (int i = 0; i < oneSched.length; i++) {
			if (allProcs[i] > systemTime) {
				oneSched[i] = "*";
			} else {
				oneSched[i] = "-";
			}
		}
		boolean add = false;
		///////////////// Debug Output //////////////////////

		for (int i = 0; i < readyNodes.size(); i++) {

			if (readyNodes.get(i).partition == -1)
				continue;

			Node n = readyNodes.get(i);

			currentExe[n.partition] = n;
			allocNodes.get(n.partition).add(n);
			n.start = systemTime;

			Pair<Pair<Long, Double>, Integer> ETWithCache = n.crp.computeET(-1, history_level1, history_level2,
					history_level3, n, n.partition, cacheAware, 0, 0, n.hasFaults);

			long realET = ETWithCache.getFirst().getFirst();
			int cacheEffects = ETWithCache.getSecond();
			totalAccess++;

			n.expectedET = realET;

			// if(alloc.equals(Allocation.CACHE_AWARE) && realET !=
			// n.expectedET)
			// {
			// System.out.println("Simualtor.ExecuteReadyNodes(): realET does
			// not equals to expectedET");
			// System.exit(-1);
			// }

			cachePerformance[cacheEffects - 1] = cachePerformance[cacheEffects - 1] + 1;

			/*
			 * A DAG is started when its SOURCE node starts execution
			 */
			if (n.getType().equals(NodeType.SOURCE) || n.getType().equals(NodeType.SOLO)) {
				Utils.getDagByIndex(dags, n.getDagID(), n.getDagInstNo()).startTime = systemTime;
			}

			allProcs[n.partition] = n.finishAt = systemTime + realET;

			///////////////// Debug Output //////////////////////
			// oneSched[n.partition] = n.getDagID() + "_" + n.getDagInstNo() + "_" +
			// n.getId() + ":" + n.finishAt;
			oneSched[n.partition] = n.getDagID() + "_" + n.getId();
			add = true;
			///////////////// Debug Output //////////////////////

			readyNodes.remove(n);
			i--;
		}

		if (add) {
			// List<Node> readys = new ArrayList<>(readyNodes);
			allocHistory.add(oneSched);
			debug_print_allocation(printSim, oneSched);
		}

	}

	/*****************************************************************
	 *** Maintain and update run-time queues based on current time ***
	 *****************************************************************/
	public void UpdateSystemStatus() {
		/*
		 * Check 1) whether any executing node finishes at the current time; 2) any
		 * ready node can execute now; 3) any DAG has finished execution.
		 */
		for (int i = 0; i < currentExe.length; i++) {

			Node n = currentExe[i];
			if (n != null && n.finishAt <= systemTime) {
				currentExe[i] = null;
				n.finish = true;

				/* add the node to history for each cache level */
				history_level1.get(i).add(n);
				int clusterID = cache.getLevel2ClusterID(i);
				history_level2.get(clusterID).add(n);
				history_level3.add(n);

				DirectedAcyclicGraph d = Utils.getDagByIndex(dags, n.getDagID(), n.getDagInstNo());
				d.allocNodes.add(n);

				/*
				 * A node can execute if all its parents are finished
				 */
				for (Node child : n.getChildren()) {
					boolean isReady = true;
					for (Node parent : child.getParent()) {
						if (!parent.finish)
							isReady = false;
					}

					if (isReady) {
						if (child.isCond) {
							/*
							 * 1\ choose the path
							 * 2\ update sink -> suc
							 * 3\ add new source to readyqueue
							 */
							DirectedAcyclicGraph subgraph = child.choosePath();
							List<Node> subNodes = subgraph.getFlatNodes();

							int numOfSub = subNodes.size();
							Node sink = subNodes.get(numOfSub - 1);
							if (sink.getChildren().size() != 0) {
								System.out.println("error");
								System.exit(-1);
							}
							sink.type = NodeType.NORMAL;
							Node source = subNodes.get(0);
							if (source.getParent().size() != 0) {
								System.out.println("error");
								System.exit(-1);
							}
							source.type = NodeType.NORMAL;
							readyNodes.add(source);

							for (Node kid : child.getChildren()) {
								/*
								 * 1\ release n -> child
								 * 2\ add sink -> child
								 */
								sink.addChildren(kid);
								kid.removeParent(child);
								kid.addParent(sink);
							}

							// for debug
							child.finish = true;
							for (Node son : subNodes) {
								son.addParent(child);
								son.confather = child.getId();
								son.setDagInstNo(child.getDagInstNo());
							}
						} else {
							readyNodes.add(child);
						}
					}
				}

				/*
				 * A DAG is finished when its SINK node finishes execution
				 */
				if (n.getType().equals(NodeType.SINK) || n.getType().equals(NodeType.SOLO)) {
					Utils.getDagByIndex(dags, n.getDagID(), n.getDagInstNo()).finishTime = systemTime;
				}
			}
		}

		/*
		 * Checking whether the sleeping DAGs can be released now.
		 */
		for (int i = 0; i < sleepingDAGs.size(); i++) {
			DirectedAcyclicGraph dag = sleepingDAGs.get(i);
			if (dag.releaseTime <= systemTime) {

				sleepingDAGs.remove(dag);
				readyDAGs.add(dag);
				readyNodes.add(dag.getSource());
				i--;
			}
		}

		/*
		 * Check whether the ready DAGs are finished now.
		 */
		for (int i = 0; i < readyDAGs.size(); i++) {
			DirectedAcyclicGraph dag = readyDAGs.get(i);
			if (dag.finishTime <= systemTime) {
				readyDAGs.remove(dag);
				i--;
			}
		}

	}

	/****************************************************************
	 *** Advance to next time unit. Sometimes we jump, time flies ***
	 ****************************************************************/
	private void advance() {
		long oldTime = systemTime;
		/*
		 * We jump to the next available time if all cores are busy.
		 */
		boolean jump = true;
		long firstFinish = Long.MAX_VALUE;
		for (long i : allProcs) {
			firstFinish = firstFinish < i ? firstFinish : i;
			if (i <= systemTime)
				jump = false;
		}

		if (jump)
			systemTime = firstFinish;
		else {

			// if (readyNodes.size() > 0) {
			// System.out.println(
			// "Simualtor.advance(): timing error. It this not possible to still
			// have ready nodes wating here");
			// System.exit(-1);
			// }

			long earliestFinish = Long.MAX_VALUE;

			/*
			 * earliest finish of executing node.
			 */
			for (long i : allProcs) {
				if (i > systemTime)
					earliestFinish = earliestFinish < i ? earliestFinish : i;
			}

			/*
			 * earliest release of sleeping node
			 */
			long earliestRelease = sleepingDAGs.size() > 0 ? sleepingDAGs.get(0).releaseTime : Long.MAX_VALUE;

			systemTime = earliestFinish < earliestRelease ? earliestFinish : earliestRelease;
		}

		if (systemTime == oldTime) {
			System.out.println("Simualtor.advance(): timing error. The system time is not advanced! ");
			System.exit(-1);
		}

		if (systemTime == Long.MAX_VALUE && (sleepingDAGs.size() > 0 || readyNodes.size() > 0)) {
			System.out.println(
					"Simualtor.advance(): timing error. The system time is Long Max but there is still waiting nodes/DAGs ! ");
			System.exit(-1);
		}
	}

	private List<Integer> getAvailableCores() {
		List<Integer> available = new ArrayList<>();
		for (int i = 0; i < allProcs.length; i++) {
			if (allProcs[i] <= systemTime)
				available.add(i);
		}
		return available;
	}

	/******************************************************************
	 ***************** Debug Info and Results Summary *****************
	 ******************************************************************/
	private void debug_output_begin(boolean printSim) {
		if (printSim) {
			String res = "Simulation type: " + type.toString() + "    " + "Allocation: " + alloc;

			System.out.println("*****************************************************************");
			System.out.println(res);
			System.out.println("*****************************************************************");

			res += "\n\n";

			System.out.println(
					"\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Allocation Info <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

			System.out.printf("%10s    ", "sysTime");
			for (int i = 0; i < history_level1.size(); i++) {
				System.out.printf("%15s    ", "Core:" + i);
			}
			System.out.println();

		}
	}

	private void debug_print_allocation(boolean printSim, String[] oneSched) {
		if (printSim) {
			System.out.printf("%10s    ", systemTime);
			for (String s : oneSched) {
				System.out.printf("%15s    ", s);
			}
			System.out.println();

			// System.out.println("System Time: " + systemTime);
			// System.out.println("Sleeping DAGs: " +
			// Arrays.toString(sleepingDAGs.toArray()));
			// System.out.println("Ready DAGs: " +
			// Arrays.toString(sleepingDAGs.toArray()));
			// System.out.println("Ready Nodes: " +
			// Arrays.toString(readyNodes.toArray()));
			// System.out.println("Running Nodes: " +
			// Arrays.toString(currentExe));
		}

	}

	private void debug_output_end(boolean printSim) {
		if (printSim) {
			System.out.println(
					"\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Allocation Info <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

			System.out.println("\n\n\n");
			System.out.println(
					"--------------------------------------- DAG Execution Summary ---------------------------------------------");

			for (DirectedAcyclicGraph dag : dags) {
				System.out.printf(
						"---  DAG_" + dag.id + "_" + dag.instanceNo
								+ "    starts at t=%8d,   finishes at t=%8d,   makespan t=%8d. \n",
						dag.releaseTime, dag.finishTime, (dag.finishTime - dag.releaseTime));
			}

			System.out.println(
					"--------------------------------------- DAG Execution Summary ---------------------------------------------");
		}

	}

	// private String reprotSimulationResult() {
	// String res = "Simulation type: " + type.toString() + " " + "Allocation: " +
	// alloc;
	//
	// System.out.println("*****************************************************************");
	// System.out.println(res);
	// System.out.println("*****************************************************************");
	//
	// res += "\n\n";
	//
	// // System.out.println(
	// // "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Execution Trace
	// // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	// //
	// // List<List<Long>> finishTimes = new ArrayList<>();
	// //
	// // for (List<Node> nodes : history) {
	// // List<Long> finishPerProc = nodes.stream().map(c ->
	// // c.finishAt).collect(Collectors.toList());
	// // finishTimes.add(finishPerProc);
	// // }
	// //
	// // res += "Execuation Trace: \n\n";
	// //
	// // for (int i = 0; i < history.size(); i++) {
	// //
	// // if (i % 4 == 0) {
	// // res += "Level 2 Cache Group: " + i + "\n";
	// // System.out.println(">>> Level 2 Cache Group: " + i + ":");
	// // }
	// // res += " Processor: " + i + "\n";
	// // System.out.println(">>> Processor: " + i);
	// //
	// // for (Node n : history.get(i)) {
	// // res += " " + n.getExeInfo() + ", \n";
	// // n.printExeInfo(">>> ");
	// // }
	// //
	// // res += "\n";
	// // }
	// // System.out.println(
	// // ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Execution Trace End
	// // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
	//
	// System.out.println(
	// "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Allocation Info
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	//
	// for (int i = 0; i < history_level1.size(); i++) {
	// System.out.printf("%10s ", "Core:" + i);
	// }
	// System.out.println();
	//
	// for (String[] oneSched : allocHistory) {
	//
	// for (String s : oneSched) {
	// System.out.printf("%10s ", s);
	// }
	// System.out.println();
	// }
	//
	// // int maxSize = history.stream().mapToInt(c ->
	// // c.size()).max().getAsInt();
	// // for (int j = 0; j < maxSize; j++) {
	// // for (int i = 0; i < history.size(); i++) {
	// // try {
	// // if (history.get(i).get(j).getDagID() == 0)
	// // System.out.printf("%10s ", history.get(i).get(j).getFullName());
	// // else
	// // System.out.printf("%10s ", history.get(i).get(j).getFullName());
	// // } catch (Exception e) {
	// // System.out.printf("%10s ", "-");
	// // }
	// //
	// // }
	// // System.out.println();
	// // }
	//
	// System.out.println(
	// "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Allocation Info
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	//
	// System.out.println("--------------------------------------- DAG Execution
	// Summary ---------------------------------------------");
	//
	// res += "DAG Execution Summary: \n\n";
	//
	// for (DirectedAcyclicGraph dag : dags) {
	// res += "DAG_" + dag.id + "_" + dag.instanceNo + " finishes at dag.finishTime.
	// \n";
	// System.out.printf("--- DAG_" + dag.id + "_" + dag.instanceNo + " starts at
	// t=%8d, finishes at t=%8d, duration t=%8d. \n",
	// dag.releaseTime, dag.finishTime, (dag.finishTime - dag.releaseTime));
	// }
	//
	// System.out.println("--------------------------------------- DAG Execution
	// Summary ---------------------------------------------");
	//
	// return res;
	// }

	// public static void main(String args[]) {
	//
	// int cores = 8;
	//
	// SystemGenerator gen = new SystemGenerator(100, 1000, cores, 2, true,
	// 1000);
	// List<DirectedAcyclicGraph> dags = gen.generatedDAGInstancesInOneHP(-1,
	// -1, null);
	//
	// Simualtor sim = new Simualtor(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE,
	// Allocation.LOAD_BALANCE,
	// RecencyType.ORDER, dags, cores, 1000, true, true);
	// sim.simulate(true, true);
	//
	// Simualtor sim1 = new Simualtor(SimuType.CLOCK_LEVEL, Hardware.PROC_CACHE,
	// Allocation.CACHE_AWARE,
	// RecencyType.ORDER, dags, cores, 1000, true, true);
	// sim1.simulate(true, true);
	// System.out.println("finished");
	// }

}
