package uk.ac.york.mocha.simulator.generator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DAGtoPython;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.entity.RecencyProfile;
import uk.ac.york.mocha.simulator.entity.RecencyProfileReal;
import uk.ac.york.mocha.simulator.parameters.SchedulingParameters;
import uk.ac.york.mocha.simulator.parameters.StructuralParameters;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class SystemGenerator {

	private boolean isHarmonic; // distribution of periods

	private int total_tasks;
	private double totalUtil;

	private Random ran;

	private boolean print;
	private int cores;
	private boolean takeAllUtil;
	private List<Double> assignedUtils;

	private boolean randomC;

	public int periodToUitl = 2;

	public CacheHierarchy cache;

	public Random NodeCond = new Random(56);

	DecimalFormat df = new DecimalFormat("#.###");

	public SystemGenerator(int total_partitions, int totalTasks, boolean isHarmonic, boolean takeAllUtil,
			List<Double> assignedUtils, Random rng, boolean randomC, boolean print, List<int[]> level2) {

		this.totalUtil = SystemParameters.utilPerTask * (double) totalTasks;
		this.takeAllUtil = takeAllUtil;
		this.total_tasks = totalTasks;
		this.isHarmonic = isHarmonic;
		this.print = print;
		this.ran = rng;
		this.cores = total_partitions;

		this.assignedUtils = assignedUtils;

		this.randomC = randomC;

		if (level2 == null)
			cache = new CacheHierarchy(total_partitions, SystemParameters.cacheLevel, SystemParameters.Level2CoreNum);
		else
			cache = new CacheHierarchy(total_partitions, SystemParameters.cacheLevel, level2);
	}

	public SystemGenerator(int total_partitions, int totalTasks, boolean isHarmonic, boolean takeAllUtil,
			List<Double> assignedUtils, int seed, boolean randomC, boolean print) {

		this(total_partitions, totalTasks, isHarmonic, takeAllUtil, assignedUtils, new Random(seed), randomC, print,
				null);
	}

	public SystemGenerator(int total_partitions, int totalTasks, boolean isHarmonic, boolean takeAllUtil,
			List<Double> assignedUtils, Random rng, boolean randomC, boolean print) {

		this(total_partitions, totalTasks, isHarmonic, takeAllUtil, assignedUtils, rng, randomC, print, null);
	}

	public Pair<List<DirectedAcyclicGraph>, CacheHierarchy> generatedDAGInstancesInOneHP(int forceInstanceNum,
			int hyperPeriodNum, List<Long> periods, boolean hard) {

		if (periods != null && periods.size() != total_tasks) {
			System.err
					.println("SystemGenerator.generatedDAGInstancesInOneHP(): # of given periods != # of total tasks");
			System.exit(-1);
		}

		if (print)
			System.out.println(
					"----------------------------------- Scheduling parameters -----------------------------------");

		List<DirectedAcyclicGraph> dagTasks = null;
		boolean schedulable = false;

		Pair<Long, List<int[]>> res = null;

		if (hard) {
			while (!schedulable) {
				dagTasks = generateSporadicDAGs(periods, hard);

				/**
				 * Set offline scheduling and allocation for the hard task
				 */

				DirectedAcyclicGraph dag = dagTasks.get(0);
				dag.hard = hard;

				res = DAGtoPython.pharseDAGForPython(dag, cores);

				long blocking = 0;

				if (dagTasks.size() > 1) {
					List<Node> blockingNodes = new ArrayList<>();

					for (int i = 1; i < dagTasks.size(); i++) {
						blockingNodes.addAll(dagTasks.get(i).getFlatNodes());
					}

					blockingNodes.sort((c1, c2) -> -Long.compare(c1.getWCET(), c2.getWCET()));

					for (int i = 0; i < SystemParameters.fan_in; i++) {
						blocking += blockingNodes.get(i).getWCET();
					}
				}

				long response_time = res.getFirst() + blocking;
				if (response_time > dag.getSchedParameters().getDeadline())
					schedulable = false;
				else
					schedulable = true;
			}

			List<int[]> prio = res.getSecond();

			/*
			 * Set priority for hard real-time nodes
			 */
			for (Node n : dagTasks.get(0).getFlatNodes()) {
				int id = n.getId();

				for (int i = 0; i < prio.size(); i++) {
					if (prio.get(i)[0] - 1 == id) {
						n.priority = prio.get(i)[1];
						break;
					}
				}
			}

			// /*
			// * Set allocation for hard real-time nodes
			// */
			// List<Node> hardNodes = new
			// ArrayList<>(dagTasks.get(0).getFlatNodes());
			// hardNodes.sort((c1, c2) -> -Integer.compare(c1.priority,
			// c2.priority));
			//
			// int currentCore = 0;
			// for (int i = 0; i < hardNodes.size(); i++) {
			// Node n = hardNodes.get(i);
			// n.offline_partition = currentCore;
			//
			// if (currentCore == 7)
			// currentCore = 0;
			// else
			// currentCore++;
			// }
		} else {
			dagTasks = generateSporadicDAGs(periods, hard);
		}

		for (DirectedAcyclicGraph d : dagTasks)
			d.findPath(true);

		/************************************************************************************/

		List<DirectedAcyclicGraph> dags = new ArrayList<>();

		if (hyperPeriodNum > 0) {

			long hyperPeriod = Utils.getHyperPeriod(
					dagTasks.stream().map(c -> c.getSchedParameters().getPeriod()).collect(Collectors.toList()));

			long totoalDuration = hyperPeriod * hyperPeriodNum;

			for (DirectedAcyclicGraph dag : dagTasks) {
				long instances = totoalDuration / dag.getSchedParameters().getPeriod();

				dag.totalInstNum = instances > forceInstanceNum ? instances : forceInstanceNum;

				if (dag.totalInstNum <= 0) {
					System.err.println(
							"SystemGenerator.generatedDAGInstancesInOneHP(): DAG instances is less or equal to 0!");
					System.exit(-1);
				}

				dags.addAll(dag.getInstances(instances));
			}

		} else if (forceInstanceNum > 0) {

			// long duration = dagTasks.get(total_tasks -
			// 1).getSchedParameters().getPeriod() * forceInstanceNum;

			for (DirectedAcyclicGraph dag : dagTasks) {
				dag.totalInstNum = forceInstanceNum; // (long)
														// Math.ceil((double)
														// duration / (double)
														// dag.getSchedParameters().getPeriod());
				dags.addAll(dag.getInstances(dag.totalInstNum));
			}

		} else {
			long hyperPeriod = Utils.getHyperPeriod(
					dagTasks.stream().map(c -> c.getSchedParameters().getPeriod()).collect(Collectors.toList()));

			for (DirectedAcyclicGraph dag : dagTasks) {
				int instances = (int) (hyperPeriod / dag.getSchedParameters().getPeriod());
				assert (instances > 0);

				dags.addAll(dag.getInstances(instances));
			}
		}

		if (print) {
			System.out.println("Number of instances");
			for (DirectedAcyclicGraph d : dagTasks)
				System.out.print(d.totalInstNum + "    ");
			System.out.println();

			if (dags == null || dags.size() == 0) {
				System.out.println("SystemGenerator.generatedDAGInstancesInOneHP()");
				System.exit(-1);
			}
		}
		return new Pair<List<DirectedAcyclicGraph>, CacheHierarchy>(dags, cache);
	}

	public List<DirectedAcyclicGraph> generatedForSteven(List<Long> wcet, List<Long> periods, List<Integer> priority,
			List<RecencyProfileReal> crps, CacheHierarchy cache, int instanceNum, List<Integer> instances) {

		if (periods != null && periods.size() != total_tasks) {
			System.err
					.println("SystemGenerator.generatedDAGInstancesInOneHP(): # of given periods != # of total tasks");
			System.exit(-1);
		}

		if (print)
			System.out.println(
					"----------------------------------- Scheduling parameters -----------------------------------");

		List<DirectedAcyclicGraph> dagTemplates = new ArrayList<>();

		for (int i = 0; i < total_tasks; i++) {
			double util = Double.parseDouble(df.format((double) wcet.get(i) / (double) periods.get(i)));

			SchedulingParameters sched_param = new SchedulingParameters(priority.get(i), periods.get(i), periods.get(i),
					wcet.get(i), util, -1);
			StructuralParameters dag_param = new StructuralParameters(1, 1, 1, 1, 0, ran);

			List<RecencyProfile> crp = new ArrayList<>();
			crp.add(crps.get(i));
			DirectedAcyclicGraph dag = new DirectedAcyclicGraph(sched_param, dag_param, crp, cache, i, ran, false,
					true);

			dag.getFlatNodes().get(0).setWCET((long) Math.round(crps.get(i).WCET));

			dagTemplates.add(dag);
		}

		List<DirectedAcyclicGraph> dags = new ArrayList<>();
		for (int i = 0; i < dagTemplates.size(); i++) {
			DirectedAcyclicGraph d = dagTemplates.get(i);

			if (instances != null)
				d.totalInstNum = instances.get(i);
			else
				d.totalInstNum = instanceNum;

			dags.addAll(d.getInstances(d.totalInstNum));

		}

		return dags;
	}

	private List<DirectedAcyclicGraph> generateSporadicDAGs(List<Long> periods, boolean hard) {

		List<DirectedAcyclicGraph> dags = new ArrayList<>();
		List<SchedulingParameters> schedParam = generateSchedParam(periods);

		switch (periodToUitl) {
			case 1:
				for (SchedulingParameters sp : schedParam) {
					sp.setPeriod(sp.getPeriod() * 2);
				}

				break;

			case 2:
				/* we do nothing here */

				break;

			case 3:

				for (SchedulingParameters sp : schedParam) {
					sp.setPeriod((long) Math.floor((double) sp.getPeriod() / (double) 1.5));
				}

				break;

			case 4:

				for (SchedulingParameters sp : schedParam) {
					sp.setPeriod((long) Math.floor((double) sp.getPeriod() / (double) 2));
				}

				break;

			default:
				break;
		}

		/*
		 * Assign scheduling parameters to each DAG
		 */
		for (int i = 0; i < total_tasks; i++) {
			StructuralParameters dag_param = new StructuralParameters(SystemParameters.maxLayer,
					SystemParameters.minLayer, SystemParameters.maxParal, SystemParameters.minParal,
					SystemParameters.connectProb, ran);
			DirectedAcyclicGraph dagTask = null;

			if (i == 0 && hard) {
				dagTask = new DirectedAcyclicGraph(schedParam.get(i), dag_param, cache, i, ran, true);
			} else {
				dagTask = new DirectedAcyclicGraph(schedParam.get(i), dag_param, cache, i, ran, false);
			}

			dags.add(dagTask);
		}

		generateWCETs(dags);
		ToCond(dags);


		return dags;
	}

	/*
	 * transfer normal node to conditional node
	 */
	private void ToCond(List<DirectedAcyclicGraph> dags) {
		for (DirectedAcyclicGraph d : dags) {
			List<Node> flatNodes = d.getFlatNodes();
			if(SystemParameters.FixCondNum){
				int m = d.getFlatNodes().size();
				for(Integer nodeId : pickNumbers(m)){
					Node n = flatNodes.get(nodeId);
					n.isCond = true;
					n.generateCondPath();
					generateCondStruc(n, d.sched_param.getPeriod());
				}
			}
			else{
				for (Node n : flatNodes) {
					if (n.getChildren().size() == 0 || n.getParent().size() == 0) {
						/*
						 * skip the sink and source
						 */
						continue;
					}
					double p = NodeCond.nextDouble();
					if (p < SystemParameters.pCond) {
						n.isCond = true;
						n.generateCondPath();
						generateCondStruc(n, d.sched_param.getPeriod());
					}
				}
			}
		}
	}

	/*
	* for randomly choose conditional node
	* */
	public static List<Integer> pickNumbers(int limit) {
//		if (SystemParameters.MAX_CondNum >= limit) {
//			throw new IllegalArgumentException("k must be less than limit");
//		}

		long seed = 1000;
//		Random rn = new Random(seed);
//		int noc = rn.nextInt(SystemParameters.MAX_CondNum) + 1;
		int noc = SystemParameters.MAX_CondNum;// exactly three


		// 防止bug，在入口处会加一层判断
		int newLimit = Math.min(limit - 2, noc);

		List<Integer> numbers = new ArrayList<>();
		for (int i = 1; i < limit - 1; i++) {
			numbers.add(i);
		}

		// long seed = 1000; // 固定的种子
		Collections.shuffle(numbers, new Random(seed));
		return numbers.subList(0, newLimit);
	}

	/*
	 * generate conditional structure for conditional node
	 */
	private void generateCondStruc(Node n, long period) {
		/*
		 * use the max length of path to update the node's WCET
		 */
		Random r = new Random(56);
		long W = n.getWCET();
		long max_wcet = 0;
		double jobUtil = (double) W / (double) period;
		if (jobUtil == 0)
			System.exit(-1);

		for (int i = 0; i < n.nCond; i++) {
			// SystemParameters.utilforTask = (r.nextDouble() * 0.5 + 0.75) * jobUtil;
			SystemParameters.utilforTask = jobUtil;
			n.utils.add(SystemParameters.utilforTask);
			PathGenerator gen = new PathGenerator(SystemParameters.coreNum, 1, true, true, null, 1000 + i, true,
					false);
			Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(1, -1, n.getWCET(),
					null, false);

//			int debug = 0;
//			for(Node ele : sys.getFirst().get(0).getFlatNodes()) debug += ele.WCET;
			long cur_wcet = 0;
			for (Node m : sys.getFirst().get(0).getFlatNodes()) {
				if (m.isCritical)
					cur_wcet += m.getWCET();
			}
			max_wcet = Math.max(max_wcet, cur_wcet);

			// longestpath的length赋给node属性
			long length = 0;
			for(Node crit : sys.getFirst().get(0).longestPath) length += crit.WCET;
			n.correspondLen.add(length);
			n.idx2len.put(i, length);
			n.idx2prob.put(i, n.prob.get(i));
		}
		// 用最大branch的最大length作为WCET
		n.setWCET(max_wcet);
	}

	private void generateWCETs(List<DirectedAcyclicGraph> dags) {

		if (print) {
			System.out.println("Assigned and generated WCET (in us):");
			System.out.println("-----------------------------------------------------------------------------");
		}

		for (DirectedAcyclicGraph d : dags) {

			long totalWCET = d.getSchedParameters().getWCET();
			List<Node> node = d.getFlatNodes();

			long[] c = new long[d.getNodeNum()];
			long sumC = 0;
			long sum = 0;

			for (int i = 0; i < c.length; i++) {
				c[i] = randomC ? ran.nextInt(100) : 100;
				sumC += c[i];
			}

			double ratio = (double) sumC / (double) totalWCET;

			for (int i = 0; i < c.length; i++) {
				long cNode = (long) Math.ceil((double) c[i] / ratio);
				sum += cNode;
				if (cNode == 0)
					cNode = 1;
				node.get(i).setWCET(cNode);
			}

			d.getSchedParameters().setWCET(sum);
			if (print)
				System.out.printf("|    DAG_%2d   |   Assigned WCET: %10d   |   Actual WCET: %10d   |\n", d.id,
						totalWCET, sum);

		}

		if (print)
			System.out.println("-----------------------------------------------------------------------------");
	}

	public static void generateWCET(DirectedAcyclicGraph d, Random rng, boolean randomC, boolean print) {

		if (print) {
			System.out.println("Assigned and generated WCET (in us):");
			System.out.println("-----------------------------------------------------------------------------");
		}

		long totalWCET = d.getSchedParameters().getWCET();
		List<Node> node = d.getFlatNodes();

		long[] c = new long[d.getNodeNum()];
		long sumC = 0;
		long sum = 0;

		for (int i = 0; i < c.length; i++) {
			c[i] = randomC ? rng.nextInt(100) : 100;
			sumC += c[i];
		}

		double ratio = (double) sumC / (double) totalWCET;

		for (int i = 0; i < c.length; i++) {
			long cNode = (long) Math.ceil((double) c[i] / ratio);
			sum += cNode;
			if (cNode == 0)
				cNode = 1;
			node.get(i).setWCET(cNode);
		}

		d.getSchedParameters().setWCET(sum);
		if (print) {
			System.out.printf("|    DAG_%2d   |   Assigned WCET: %10d   |   Actual WCET: %10d   |\n", d.id, totalWCET,
					sum);
			for (Node n : d.getFlatNodes())
				System.out.println(n.toString());
		}

		if (print)
			System.out.println("-----------------------------------------------------------------------------");
	}

	public static void generateWCETs(List<DirectedAcyclicGraph> dags, Random rng, boolean randomC, boolean print) {

		if (print) {
			System.out.println("Assigned and generated WCET (in us):");
			System.out.println("-----------------------------------------------------------------------------");
		}

		for (DirectedAcyclicGraph d : dags) {

			long totalWCET = d.getSchedParameters().getWCET();
			List<Node> node = d.getFlatNodes();

			long[] c = new long[d.getNodeNum()];
			long sumC = 0;
			long sum = 0;

			for (int i = 0; i < c.length; i++) {
				c[i] = randomC ? rng.nextInt(100) : 100;
				sumC += c[i];
			}

			double ratio = (double) sumC / (double) totalWCET;

			for (int i = 0; i < c.length; i++) {
				long cNode = (long) Math.ceil((double) c[i] / ratio);
				sum += cNode;
				if (cNode == 0)
					cNode = 1;
				node.get(i).setWCET(cNode);
			}

			d.getSchedParameters().setWCET(sum);
			if (print) {
				System.out.printf("|    DAG_%2d   |   Assigned WCET: %10d   |   Actual WCET: %10d   |\n", d.id,
						totalWCET, sum);

			}

		}

		if (print)
			System.out.println("-----------------------------------------------------------------------------");
	}

	/*
	 * generate scheduling parameters for DAGs
	 */
	private List<SchedulingParameters> generateSchedParam(List<Long> periodsT) {
		/*
		 * generates uniformly distributed periods
		 */
		List<Long> periods;
		if (periodsT == null) {
			periods = new ArrayList<>(total_tasks);
			while (true) {

				if (isHarmonic) {

					/* harmonic period, same periods are not allowed */
					List<Long> harmonicPeriods = new ArrayList<>();

					for (long i = SystemParameters.MIN_PERIOD; i <= SystemParameters.MAX_PERIOD; ++i) {
						if (SystemParameters.MAX_PERIOD % i == 0) {
							harmonicPeriods.add(i * 1000);
						}
					}

					if (harmonicPeriods.size() <= total_tasks) {
						System.err.println("not enough harmonic periods");
						System.exit(-1);
					}

					long period = harmonicPeriods.get(ran.nextInt(harmonicPeriods.size()));
					if (!periods.contains(period))
						periods.add(period);

				} else {
					/* log Uniform distrubtion */
					double a1 = Math.log(SystemParameters.minT);
					double a2 = Math.log(SystemParameters.maxT + 1);
					double scaled = ran.nextDouble() * (a2 - a1);
					double shifted = scaled + a1;
					double exp = Math.exp(shifted);

					int result = (int) exp;
					result = Math.max(SystemParameters.minT, result);
					result = Math.min(SystemParameters.maxT, result);

					long period = result * 1000;
					if (!periods.contains(period))
						periods.add(period);
				}

				if (periods.size() >= total_tasks)
					break;
			}

		} else {
			periods = new ArrayList<>(periodsT);
		}
		periods.sort((p1, p2) -> Double.compare(p1, p2));

		if (print) {
			System.out.print("task periods & deadline (in us): ");
			for (int i = 0; i < periods.size(); i++) {
				long p = periods.get(i);
				System.out.print(p + "   ");
			}
			System.out.println();
		}

		/*
		 * generate utils by UUifastDiscard
		 */
		List<Double> utils = null;
		if (this.assignedUtils == null) {
			UUnifastDiscard unifastDiscard = new UUnifastDiscard(totalUtil, total_tasks, 1000, cores, takeAllUtil, ran);
			while (true) {
				utils = unifastDiscard.getUtils();

				double tt = 0;
				for (int i = 0; i < utils.size(); i++) {
					tt += utils.get(i);
				}

				if (utils != null)
					if (utils.size() == total_tasks && tt <= totalUtil)
						break;
			}
		} else {
			if (this.assignedUtils.size() != total_tasks) {
				System.err.println(
						"SystemGenerator.generateSchedParam(): pre-assigned utilisations does not match task number");
				System.exit(-1);
			}

			utils = new ArrayList<>(this.assignedUtils);
		}

		if (print) {
			System.out.print("task utils: ");
			double tt = 0;
			for (int i = 0; i < utils.size(); i++) {
				tt += utils.get(i);
				System.out.print(utils.get(i) + "   ");
			}
			System.out.println();
			System.out.println("total uitls: " + tt);
		}

		/*
		 * generate priority by DMPO
		 */
		List<Integer> priorities = new ArrayList<>();
		for (int i = 0; i < total_tasks; i++) {

			if (i == 0)
				priorities.add(SystemParameters.MAX_PRIORITY - (i + 1) * 2);
			else {
				priorities.add(SystemParameters.MAX_PRIORITY - (i + 1) * 2);
				// priorities.add(10); // TODO: Now we have equal priority for
				// non real-time DAGs.
			}
		}

		priorities.sort((p1, p2) -> -Integer.compare(p1, p2));

		if (print) {
			System.out.print("task priority: ");
			for (int i = 0; i < priorities.size(); i++) {
				long p = priorities.get(i);
				System.out.print(p + "   ");
			}
			System.out.println();
		}

		/*
		 * generate scheduling parameters
		 */
		List<SchedulingParameters> schedParams = new ArrayList<>();
		for (int i = 0; i < total_tasks; i++) {
			long WCET = (long) Math.ceil((double) periods.get(i) * utils.get(i));

			SchedulingParameters param = new SchedulingParameters(priorities.get(i), periods.get(i), periods.get(i),
					WCET, utils.get(i), -1);

			schedParams.add(param);
		}

		return schedParams;
	}

	// public static void main(String args[]) {
	// SystemGenerator gen = new SystemGenerator(100, 1000, 32, 16, true, 1000);
	// gen.generatedDAGInstancesInOneHP(-1, -1, null);
	// }

}
