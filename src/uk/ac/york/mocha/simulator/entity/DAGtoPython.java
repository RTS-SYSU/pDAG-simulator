package uk.ac.york.mocha.simulator.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.generator.SystemGenerator;

public class DAGtoPython {

	public static void main(String args[]) {

		for (int i = 0; i < 10000; i++) {
			SystemGenerator gen = new SystemGenerator(8, 1, true, true, null, i, true, false);
			List<DirectedAcyclicGraph> dags = gen.generatedDAGInstancesInOneHP(1, -1, null, true).getFirst();

			DirectedAcyclicGraph dag = dags.get(0);

			for (int j = 0; j < dag.getFlatNodes().size(); j++) {
				dag.getFlatNodes().get(j).priority = dag.getFlatNodes().size() - j;
			}

			Pair<Long, List<int[]>> tmp = pharseDAGForPython(dag, 8);

			System.out.println(i);
			System.out.println(tmp.getFirst());
		}
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

		/*
		 * System.out.println(G);
		 * System.out.println(C);
		 * System.out.println(P);
		 * System.out.println(core);
		 */

		long makespan = -1;
		List<int[]> node_priority = new ArrayList<>();

		try {
			/*
			 * The final parameter is override priority passed to Python indicating the
			 * analysis will use the priority passed from Java. 0 - use RTSS priority with
			 * CPC model. 1 - use priority passed from java space.
			 */
			Process process = Runtime.getRuntime()
					.exec("python rta.py " + G + " " + C + " " + P + " " + core + " " + 0);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			// Read the output from the command
			// System.out.println("Here is the standard output of the command:\n");
			String s = null;
			String makespanS = "";
			while ((s = stdInput.readLine()) != null) {
				// System.out.println(s);
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
