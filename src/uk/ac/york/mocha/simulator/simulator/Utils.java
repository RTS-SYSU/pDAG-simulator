package uk.ac.york.mocha.simulator.simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import uk.ac.york.mocha.simulator.entity.DAGtoPython;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.entity.RecencyProfileReal;
import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;

public class Utils {

	public static List<long[]> getETHistroy(Node n, List<Node> hist) {
		List<long[]> etHist = new ArrayList<>();
		List<Node> sameNodes = new ArrayList<>();

		for (Node ln : hist) {
			if (ln.getDagID() == n.getDagID() && ln.getId() == n.getId())
				sameNodes.add(ln);
		}

		sameNodes.sort((c1, c2) -> -Integer.compare(c1.getDagInstNo(), c2.getDagInstNo()));

		for (int i = 0; i < SystemParameters.etHist_length; i++) {
			if (sameNodes.size() == i)
				break;

			long[] res = new long[2];
			res[0] = sameNodes.get(i).expectedET;
			res[1] = sameNodes.get(i).expectedCache;
			etHist.add(res);
		}

		return etHist;
	}

	public static List<List<Node>> getAllocHistoryByLevel2Cache(List<List<Node>> allocHis) {

		List<List<Node>> level2 = new ArrayList<>();
		for (int i = 0; i < allocHis.size() / SystemParameters.Level2CoreNum; i++) {
			level2.add(new ArrayList<>());
		}

		for (int i = 0; i < allocHis.size(); i++) {
			int cluster = i / SystemParameters.Level2CoreNum;

			level2.get(cluster).addAll(allocHis.get(i));
		}

		return level2;
	}

	public static void assignPriorityOur(List<DirectedAcyclicGraph> dags) {
		for (DirectedAcyclicGraph dag : dags) {
			Pair<Long, List<int[]>> res = null;

			dag.hard = true;

			res = DAGtoPython.pharseDAGForPython(dag, 8);
			List<int[]> prio = res.getSecond();

			for (Node n : dag.getFlatNodes()) {
				int id = n.getId();

				for (int i = 0; i < prio.size(); i++) {
					if (prio.get(i)[0] - 1 == id) {
						n.priority = prio.get(i)[1];
						break;
					}
				}
			}
		}
	}

	/*
	 * Order nodes by 1) its DAG priority and 2) DAG instance number.
	 */
	public static int compareDAG(List<DirectedAcyclicGraph> dags, int id1, int inst1, int id2, int inst2) {

		DirectedAcyclicGraph dag1 = Utils.getDagByIndex(dags, id1, inst1);
		DirectedAcyclicGraph dag2 = Utils.getDagByIndex(dags, id2, inst2);

		if (dag1.getSchedParameters().getPriority() > dag2.getSchedParameters().getPriority()) {
			return -1;
		} else if (dag1.getSchedParameters().getPriority() < dag2.getSchedParameters().getPriority()) {
			return 1;
		} else {
			return Long.compare(inst1, inst2);
		}
	}

	public static int compareNodeByPriorityAndSensitivity(List<DirectedAcyclicGraph> dags, Node c1, Node c2) {

		DirectedAcyclicGraph dag1 = Utils.getDagByIndex(dags, c1.getDagID(), c1.getDagInstNo());
		DirectedAcyclicGraph dag2 = Utils.getDagByIndex(dags, c2.getDagID(), c2.getDagInstNo());

		if (dag1.getSchedParameters().getPriority() > dag2.getSchedParameters().getPriority()) {
			return -1;
		} else if (dag1.getSchedParameters().getPriority() < dag2.getSchedParameters().getPriority()) {
			return 1;
		} else {

			int ins = Long.compare(c1.getDagInstNo(), c2.getDagInstNo());

			if (ins != 0)
				return ins;
			else
				return compareNodeBySensitivity(c1, c2);
		}
	}

	private static int compareNodeBySensitivity(Node c1, Node c2) {

		if (c1.sensitivity > c2.sensitivity)
			return -1;
		else if (c1.sensitivity < c2.sensitivity)
			return 1;
		else {
			return -Long.compare(c1.getWCET(), c2.getWCET());
		}

	}

	/*
	 * Order nodes by 1) its DAG priority and 2) its WCET.
	 */
	public static int compareNode(List<DirectedAcyclicGraph> dags, Node c1, Node c2) {

		DirectedAcyclicGraph dag1 = Utils.getDagByIndex(dags, c1.getDagID(), c1.getDagInstNo());
		DirectedAcyclicGraph dag2 = Utils.getDagByIndex(dags, c2.getDagID(), c2.getDagInstNo());

		if (dag1.getSchedParameters().getPriority() > dag2.getSchedParameters().getPriority()) {
			return -1;
		} else if (dag1.getSchedParameters().getPriority() < dag2.getSchedParameters().getPriority()) {
			return 1;
		} else {

			int ins = Long.compare(c1.getDagInstNo(), c2.getDagInstNo());

			if (ins != 0)
				return ins;
			else
				return -Long.compare(c1.getWCET(), c2.getWCET());

			// int c = -Long.compare(c1.getWCET(), c2.getWCET());
			//
			// if (c != 0)
			// return c;
			// else {
			// return Integer.compare(c1.getDagInstNo(), c2.getDagInstNo());
			// }

		}

	}

	public static int compareNodeByID(List<DirectedAcyclicGraph> dags, Node c1, Node c2) {

		return Integer.compare(c1.getId(), c2.getId());
	}

	public static int compareNodeWithPriority(List<DirectedAcyclicGraph> dags, Node c1, Node c2) {

		DirectedAcyclicGraph dag1 = Utils.getDagByIndex(dags, c1.getDagID(), c1.getDagInstNo());
		DirectedAcyclicGraph dag2 = Utils.getDagByIndex(dags, c2.getDagID(), c2.getDagInstNo());

		if (dag1.getSchedParameters().getPriority() > dag2.getSchedParameters().getPriority()) {
			return -1;
		} else if (dag1.getSchedParameters().getPriority() < dag2.getSchedParameters().getPriority()) {
			return 1;
		} else {
			int c = -1;

			if (dag1.id != dag2.id) {
				System.out.println(
						"Utils.compareNodeWithHard(): the IDs of DAG-1 and DAG-2 are not equal, but they have the same priority!");
				System.exit(-1);
			}

			c = Integer.compare(c1.getDagInstNo(), c1.getDagInstNo());

			if (c != 0)
				return c;
			else {
				return -Long.compare(c1.priority, c2.priority);
			}

		}

	}

	public static int compareNodeWithHard(List<DirectedAcyclicGraph> dags, Node c1, Node c2) {

		DirectedAcyclicGraph dag1 = Utils.getDagByIndex(dags, c1.getDagID(), c1.getDagInstNo());
		DirectedAcyclicGraph dag2 = Utils.getDagByIndex(dags, c2.getDagID(), c2.getDagInstNo());

		if (dag1.getSchedParameters().getPriority() > dag2.getSchedParameters().getPriority()) {
			return -1;
		} else if (dag1.getSchedParameters().getPriority() < dag2.getSchedParameters().getPriority()) {
			return 1;
		} else {
			int c = -1;

			if (dag1.hard) {
				if (dag1.id != dag2.id) {
					System.out.println(
							"Utils.compareNodeWithHard(): the IDs of DAG-1 and DAG-2 are not equal, but there should be only one DAG in the system!");
					System.exit(-1);
				}

				c = Integer.compare(c1.getDagInstNo(), c1.getDagInstNo());

				if (c != 0)
					return c;
				else {
					return -Long.compare(c1.priority, c2.priority);
				}

			} else {
				c = -Long.compare(c1.getWCET(), c2.getWCET());

				// if (c != 0)
				return c;
				// else {
				// return Integer.compare(c1.getDagInstNo(), c1.getDagInstNo());
				// }
			}

		}

	}

	/*
	 * Compute the hyperperiod of input DAGs. NOTE: The simulation covers a complete
	 * hyperperiod.
	 */
	public static long getHyperPeriod(List<Long> periods) {

		if (periods == null)
			return 0;

		List<Long> period_copy = new ArrayList<>(periods);
		long lcm = 1;
		int divisor = 2;

		while (true) {
			int counter = 0;
			boolean divisible = false;

			for (int i = 0; i < period_copy.size(); i++) {

				if (period_copy.get(i) == 1) {
					counter++;
				}

				if (period_copy.get(i) % divisor == 0) {
					divisible = true;
					period_copy.set(i, period_copy.get(i) / divisor);
				}
			}

			if (divisible) {
				lcm = lcm * divisor;
			} else {
				divisor++;
			}

			if (counter == period_copy.size()) {
				return lcm;
			}
		}
	}

	public static DirectedAcyclicGraph getDagByIndex(List<DirectedAcyclicGraph> dags, int id, int instanceID) {
		for (DirectedAcyclicGraph dag : dags)
			if (dag.id == id && dag.instanceNo == instanceID)
				return dag;

		return null;
	}

	public static List<DirectedAcyclicGraph> deepCopy(List<DirectedAcyclicGraph> dags) {

		List<DirectedAcyclicGraph> dp = new ArrayList<>();

		for (DirectedAcyclicGraph d : dags)
			dp.add(d.deepCopy());

		return dp;
	}

	public static void writeResult(String path, String file, String result) {
		writeResult(path, file, result, false);
	}

	public static void writeResult(String path, String file, String result, boolean append) {

		File theDir = new File(path);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(path + "/" + file), append));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}

	public static void writeResult(String filename, String result, boolean append) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(filename), append));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}

	public static void writeResult(String filename, String result) {
		writeResult(filename, result, false);
	}

	public static void main(String args[]) {
//		List<RecencyProfileReal> crps = readJson("crp/profile.tacle.crp.json");

	}

	public static List<RecencyProfileReal> readJson(String file, List<String> taskNames, CacheHierarchy cache) {
		JSONParser parser = new JSONParser();

		List<RecencyProfileReal> crps = new ArrayList<>();

		try {
			Object obj = parser.parse(new FileReader(file));
			JSONObject jsonObj = (JSONObject) obj;

			@SuppressWarnings("unchecked")
			Set<String> names = jsonObj.keySet();

			List<String> nameList = new ArrayList<String>();
			if (taskNames == null) {
				for (String x : names)
					nameList.add(x);
			} else {
				nameList = taskNames;
			}

			int id = 0;
			for (String key : nameList) {
				JSONObject job = (JSONObject) jsonObj.get(key);

				double WCET = ((Number) job.get("median_timing")).doubleValue();

//				double highWaterMark = ((Number) job.get("hwm")).doubleValue();
				double medianET = ((Number) job.get("median_timing")).doubleValue();

				JSONArray breaks_j = (JSONArray) job.get("breaks");
				JSONArray slopes_j = (JSONArray) job.get("slopes");
				JSONArray intercepts_j = (JSONArray) job.get("intercepts");

				double[] breaks = new double[breaks_j.size()];
				double[] slopes = new double[slopes_j.size()];
				double[] intercepts = new double[intercepts_j.size()];

				for (int i = 0; i < breaks.length; i++) {
					breaks[i] = ((Number) breaks_j.get(i)).doubleValue();
					slopes[i] = ((Number) slopes_j.get(i)).doubleValue();
					intercepts[i] = ((Number) intercepts_j.get(i)).doubleValue();
				}

				id++;

				RecencyProfileReal crp = new RecencyProfileReal(cache, RecencyType.REAL, key, id, -1, WCET, medianET,
						breaks, slopes, intercepts);
				crps.add(crp);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return crps;
	}

}
