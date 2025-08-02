package uk.ac.york.mocha.simulator.allocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.entity.InfoCap;
import uk.ac.york.mocha.simulator.generator.SystemGenerator;

public class TPDSHe {

	public static void main(String args[]) {
		int coreNum = 8;
		for (int i = 0; i < 10000; i++) {
			SystemGenerator gen = new SystemGenerator(8, 1, true, true, null, i, true, false);
			List<DirectedAcyclicGraph> dags = gen.generatedDAGInstancesInOneHP(1, -1, null, true).getFirst();

			DirectedAcyclicGraph dag = dags.get(0);

			for (int j = 0; j < dag.getFlatNodes().size(); j++) {
				dag.getFlatNodes().get(j).priority = dag.getFlatNodes().size() - j;
			}

			List<InfoCap> response_time = new TPDSHe().getResponseTime(dags, coreNum);

			System.out.println("he: " + response_time.get(0).best_response_time);
			System.out.println("------------------------------------------------------------------------------------");
		}
	}

	public List<InfoCap> getResponseTime(List<DirectedAcyclicGraph> dags, int coreNum) {

		String nodesInfo = "[";
		String WCETInfo = "[";
		String periodInfo = "[";
		String coreNumInfo = coreNum + "";

		for (int i = 0; i < dags.size(); i++) {
			DirectedAcyclicGraph d = dags.get(i);
			List<String> info = getDAGInfo(d);

			nodesInfo += info.get(0);
			WCETInfo += info.get(1);
			periodInfo += info.get(2);

			if (i != dags.size() - 1) {
				nodesInfo += ",";
				WCETInfo += ",";
				periodInfo += ",";
			}

		}

		nodesInfo += "]";
		WCETInfo += "]";
		periodInfo += "]";

		List<Long> response_time = new ArrayList<>();

		try {
			/*
			 * The final parameter is override priority passed to Python indicating the
			 * analysis will use the priority passed from Java. 0 - use RTSS priority with
			 * CPC model. 1 - use priority passed from java space.
			 */
			Process process = Runtime.getRuntime()
					.exec("python rta_tpds.py " + nodesInfo + " " + WCETInfo + " " + periodInfo + " " + coreNumInfo);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			// Read the output from the command
			// System.out.println("Here is the standard output of the command:\n");
			String out = null;
			String makespanS = "";
			while ((out = stdInput.readLine()) != null) {
				// System.out.println(out);
				makespanS += out + "";
			}

			String makespanSPre = makespanS.replaceAll("\\[", "").replaceAll("\\]", "").trim();
			String[] responset_time_string = makespanSPre.split(",");

			for (String s : responset_time_string) {
				Double d = Double.parseDouble(s);
				Long dl = (long) Math.ceil(d);
				response_time.add(dl);
			}

			// Read any errors from the attempted command
			// System.out.println("Here is the standard error of the command (if any):\n");
			String error = null;
			while ((error = stdError.readLine()) != null) {
				System.out.println(error);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		/**
		 * Here we carry on the calcualtion from Python world and keep going.
		 */
		/*
		 * Here we add inter-task interference to the response time.
		 */
		List<Long> delay_by_lower = new ArrayList<>();

		for (int i = 0; i < dags.size() - 1; i++) {
			long maxWorkload = Long.MIN_VALUE;

			for (int j = i + 1; j < dags.size(); j++) {

				long workload = dags.get(j).getFlatNodes().stream().mapToLong(c -> c.getWCET()).sum();
				if (workload > maxWorkload) {
					maxWorkload = workload;
				}
			}

			long maxDelay = (long) Math.ceil((double) maxWorkload / (double) coreNum);
			response_time.set(i, response_time.get(i) + maxDelay);
			delay_by_lower.add(maxDelay);

			// delay_by_lower.add((long) 0 );
		}
		delay_by_lower.add((long) 0);

		List<Long> delay_by_higher = new ArrayList<>();
		delay_by_higher.add((long) 0);

		for (int i = 1; i < dags.size(); i++) {

			long old_response = response_time.get(i);
			long new_response = 0;
			long inter = 0;

			while (old_response != new_response) {
				old_response = new_response;

				long total = 0;
				for (int j = i - 1; j >= 0; j--) {
					total += dags.get(j).getFlatNodes().stream().mapToLong(c -> c.getWCET()).sum() * (long) Math
							.ceil((double) old_response / (double) dags.get(j).getSchedParameters().getPeriod());
				}

				inter = (long) Math.ceil((double) total / (double) coreNum);

				new_response = response_time.get(i) + inter;

				if (new_response > dags.get(i).getSchedParameters().getPeriod()) {
					break;
				}
			}
			response_time.set(i, new_response);
			delay_by_higher.add(inter);
		}

		List<Long> allDelay = new ArrayList<>();
		for (int i = 0; i < dags.size(); i++) {
			allDelay.add(delay_by_higher.get(i) + delay_by_lower.get(i));
		}

		List<InfoCap> caps = new ArrayList<>();
		for (int i = 0; i < dags.size(); i++) {
			InfoCap cap = new InfoCap(coreNum, response_time.get(i), allDelay.get(i),
					response_time.get(i) - allDelay.get(i));
			caps.add(cap);
		}

		return caps;
	}

	private static List<String> getDAGInfo(DirectedAcyclicGraph dag) {
		List<String> info = new ArrayList<>();

		List<Node> nodes = dag.getFlatNodes();

		List<NodeByTemplate> nodesByTemplate = nodes.stream().map(c -> nodeTemplate(c)).collect(Collectors.toList());

		String G = "{";
		String C = "{";
		String T = "";

		for (int i = 0; i < nodesByTemplate.size(); i++) {
			String childrenS = nodesByTemplate.get(i).childrenS;
			String wcetS = nodesByTemplate.get(i).wcetS;

			G += childrenS;
			C += wcetS;

			if (i != nodesByTemplate.size() - 1) {
				G += ",";
				C += ",";
			}
		}

		G += "}";
		C += "}";
		T = dag.getSchedParameters().getPeriod() + "";

		info.add(G);
		info.add(C);
		info.add(T);

		return info;
	}

	public static Pair<Long, List<int[]>> pharseDAGForPython(DirectedAcyclicGraph dag, int coreNum) {

		List<Node> nodes = dag.getFlatNodes();

		nodes.sort((c1, c2) -> Integer.compare(c1.getId(), c2.getId()));

		List<NodeByTemplate> nodesByTemplate = nodes.stream().map(c -> nodeTemplate(c)).collect(Collectors.toList());

		String G = "{";
		String C = "{";
		String P = "{";

		for (int i = 0; i < nodesByTemplate.size(); i++) {
			String childrenS = nodesByTemplate.get(i).childrenS;
			String wcetS = nodesByTemplate.get(i).wcetS;
			String priorityS = nodesByTemplate.get(i).priorityS;

			G += childrenS;
			C += wcetS;
			P += priorityS;

			if (i != nodesByTemplate.size() - 1) {
				G += ",";
				C += ",";
				P += ",";
			}
		}

		G += "}";
		C += "}";
		P += "}";

		String core = coreNum + "";

		// System.out.println(G);
		// System.out.println(C);
		// System.out.println(P);
		// System.out.println(core);

		long makespan = -1;
		List<int[]> node_priority = new ArrayList<>();

		try {
			/*
			 * The final parameter is override priority passed to Python indicating the
			 * analysis will use the priority passed from Java. 0 - use RTSS priority with
			 * CPC model. 1 - use priority passed from java space.
			 */
			Process process = Runtime.getRuntime()
					.exec("python3 rta.py " + G + " " + C + " " + P + " " + core + " " + 0);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			// Read the output from the command
			// System.out.println("Here is the standard output of the command:\n");
			String s = null;
			String makespanS = "";
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
				makespanS += s + "";
			}

			String[] out = makespanS.split("\n");

			String priorityS = out[0];
			List<String> priorityA = Arrays.asList(priorityS.split(","));
			String makespan_string = priorityA.get(priorityA.size() - 1);
			// priorityA.remove(priorityA.size() - 1);

			ArrayList<String> priority_list = new ArrayList<>();
			for (int i = 0; i < priorityA.size() - 1; i++) {
				priority_list.add(priorityA.get(i));
			}

			for (String pair : priority_list) {
				String[] pair_array = pair.split(":");
				int id = Integer.parseInt(pair_array[0]);
				int priority = Integer.parseInt(pair_array[1]);

				int[] pair_list = new int[2];
				pair_list[0] = id;
				pair_list[1] = priority;

				node_priority.add(pair_list);
			}

			makespan = Long.parseLong(makespan_string);

			// Read any errors from the attempted command
			// System.out.println("Here is the standard error of the command (if any):\n");
			String error = null;
			while ((error = stdError.readLine()) != null) {
				System.out.println(error);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (makespan <= 0) {
			System.err.println("DAGtoPython.pharseDAGForPython(): makespan <= 0.");
			System.exit(-1);
		}

		return new Pair<>(makespan, node_priority);
	}

	private static NodeByTemplate nodeTemplate(Node n) {

		int id = n.getId() + 1;

		List<Integer> children = n.getChildren().stream().map(c -> c.getId()).collect(Collectors.toList());

		String childrenS = id + ":" + "[";

		for (int i = 0; i < children.size(); i++) {
			childrenS += children.get(i) + 1;

			if (i != children.size() - 1)
				childrenS += ",";
		}
		childrenS += "]";

		String wcetS = id + ":" + n.getWCET();

		String priorityS = id + ":" + n.priority;

		NodeByTemplate nodeTemp = new NodeByTemplate(childrenS, wcetS, priorityS);

		return nodeTemp;
	}
}

class NodeByTemplate {
	String childrenS;
	String wcetS;
	String priorityS;

	public NodeByTemplate(String childrenS, String wcetS, String priorityS) {
		this.childrenS = childrenS;
		this.wcetS = wcetS;
		this.priorityS = priorityS;
	}
}
