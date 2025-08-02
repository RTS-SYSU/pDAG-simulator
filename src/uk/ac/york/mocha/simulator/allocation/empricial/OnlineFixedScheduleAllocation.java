package uk.ac.york.mocha.simulator.allocation.empricial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.york.mocha.simulator.allocation.AllocationMethods;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;



public class OnlineFixedScheduleAllocation extends AllocationMethods {
	
	public static int execution_order_controller = 0;

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
		
		List<Integer> orders = readyNodes.stream().map(n -> n.fixed_order).collect(Collectors.toList());
		
		while(orders.contains(execution_order_controller)) {
			Node n = readyNodes.get(orders.indexOf(execution_order_controller));
			
			if(availableCores.contains(n.fixed_allocation)) {
				n.partition = n.fixed_allocation;
				
				int index_core = availableCores.indexOf(n.partition);
				availableCores.remove(index_core);
				
				localRunqueue.get(n.partition).add(n);
				allocHistory.get(n.partition).add(n);
				
				n.repeat_fixed_order = execution_order_controller;
				n.repeat_fixed_allocation = n.partition;
				

				execution_order_controller++;
				
				
			}
			else {
				break;
			}
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
