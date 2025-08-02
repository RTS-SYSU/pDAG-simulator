package uk.ac.york.mocha.simulator.allocation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class OnlineCacheAwarePredictiability_WCET_Sensitivity_Compare extends AllocationMethods {

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

		readyNodes.stream().forEach(c -> c.partition = -1);

		readyNodes.sort((c1, c2) -> Utils.compareNodeByPriorityAndSensitivity(dags, c1, c2));

		List<Node> preEligible = new ArrayList<>();
		for (int i = 0; i < availableCores.size(); i++) {
			if (readyNodes.size() == i)
				break;
			preEligible.add(readyNodes.get(i));
		}

		List<Integer> availableP = new ArrayList<>(availableCores);

		List<List<Long>> speedUpTable = new ArrayList<>();
		List<List<Long>> cacheTable = new ArrayList<>();

		for (Node n : preEligible) {
			List<Long> ETdrop = new ArrayList<>();
			List<Long> cachedrop = new ArrayList<>();

			for (int i = 0; i < history_level1.size(); i++) {
				int proc = i;
				if (availableP.contains(proc)) {
					/*
					 * Speed up by ABSOLUTE value
					 */
					long WCET = n.getWCET();

					Pair<Pair<Long, Double>, Integer> ETWithCache = n.crp.computeET(-1, history_level1, history_level2,
							history_level3, n, proc, true, 0, 0, false);
					long realET = ETWithCache.getFirst().getFirst();
					long speedup = WCET - realET;

					ETdrop.add(speedup);

					cachedrop.add((long) ETWithCache.getSecond());
				}
			}

			speedUpTable.add(ETdrop);
			cacheTable.add(cachedrop);
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

			Pair<Integer, Integer> p = setPartition(speedUpTable, cacheTable, allocNodes, allocProcs, allocHistoryCut,
					allocHistory, preEligible, availableP, availableTimeAllProcs, currentTime, lcif, history_level1,
					history_level2, history_level3, etHist);

			Node n = preEligible.get(p.getFirst().intValue());

			n.partition = availableP.get(p.getSecond().intValue());

			allocNodes.add(p.getFirst().intValue());
			allocProcs.add(p.getSecond().intValue());

			localRunqueue.get(n.partition).add(n);
			allocHistory.get(n.partition).add(n);
		}

		for (int i = 0; i < readyNodes.size(); i++) {
			if (readyNodes.get(i).partition > -1) {
				readyNodes.remove(i);
				i--;
			}
		}

	}

	private Pair<Integer, Integer> setPartition(List<List<Long>> speedUpTable, List<List<Long>> cacheTable,
			List<Integer> allocNodes, List<Integer> allocProcs, List<List<Node>> allocHistory,
			List<List<Node>> fullAllocHistory, List<Node> preEligible, List<Integer> procs,
			long[] availableTimeAllProcs, long time, boolean lcif, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, List<Node> etHist) {

		int row = -1;
		int col = -1;

		for (int i = 0; i < speedUpTable.size(); i++) {
			if (!allocNodes.contains(i)) {
				/**
				 * The native version: speed-up the sensitive nodes as much as I can.
				 */
				long max = Long.MIN_VALUE;
				for (int j = 0; j < speedUpTable.get(i).size(); j++) {
					if (!allocProcs.contains(j)) {
						if (max < speedUpTable.get(i).get(j)) {
							max = speedUpTable.get(i).get(j);
							row = i;
							col = j;
						}
					}
				}

				/**
				 * The new version: maintain a stable speed-up.
				 */
//				long minDiff = Long.MAX_VALUE;
//				long standardET = 0;
//				long standardCache = 0;
//
//				Node n = preEligible.get(i);
//				List<long[]> etHistOneNode = Utils.getETHistroy(n, etHist);
//
////				if (etHistOneNode.size() >= SystemParameters.etHist_length) {
//				/**
//				 * max
//				 */
////					standardET = etHistOneNode.stream().mapToLong(a -> a).max().getAsLong();
//
//				/**
//				 * Average
//				 */
////					standardET = (long) Math.round(etHistOneNode.stream().mapToLong(a -> a).average().getAsDouble());
//
//				/**
//				 * Median
//				 */
//				double[] valuesET = new double[etHistOneNode.size()];
//				double[] valuesCache = new double[etHistOneNode.size()];
//				for (int k = 0; k < etHistOneNode.size(); k++) {
//					valuesET[k] = etHistOneNode.get(k)[0];
//					valuesCache[k] = etHistOneNode.get(k)[1];
//				}
//
//				Median med = new Median();
//				standardET = (long) Math.round(med.evaluate(valuesET));
//				standardCache = (long) Math.round(med.evaluate(valuesCache));
////				}
//
////				System.out.println("Node: " + n.getFullName() + ", reference ET: " + standardET);
////				System.out.println("Node: " + n.getFullName() + ", observed ET: " + Arrays
////						.toString(etHistOneNode.stream().map(c -> c[0]).collect(Collectors.toList()).toArray()));
////				System.out.println("Node: " + n.getFullName() + ", observed Cache: " + Arrays
////						.toString(etHistOneNode.stream().map(c -> c[1]).collect(Collectors.toList()).toArray()));
////				System.out.println(
////						"Node: " + n.getFullName() + ", Speed-up: " + Arrays.toString(speedUpTable.get(i).toArray()));
//
//				for (int j = 0; j < speedUpTable.get(i).size(); j++) {
//					if (!allocProcs.contains(j)) {
//						long expectedET = n.getWCET() - speedUpTable.get(i).get(j);
//
//						if (minDiff > Math.abs(standardET - expectedET)) {
//							minDiff = Math.abs(standardET - expectedET);
//							row = i;
//							col = j;
//						}
//					}
//				}

				break;
			}
		}

		if (row == -1 || col == -1) {
			System.err.println("SimpleCacheAware.getIndexOfMaximum(): Cannot find the max value!");

			System.exit(-1);
		}

		return new Pair<Integer, Integer>(row, col);
	}

}