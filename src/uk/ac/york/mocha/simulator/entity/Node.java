package uk.ac.york.mocha.simulator.entity;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.generator.SystemGenerator;
import uk.ac.york.mocha.simulator.generator.PathGenerator;
import uk.ac.york.mocha.simulator.generator.UUnifastDiscard;
import uk.ac.york.mocha.simulator.parameters.SchedulingParameters;
import uk.ac.york.mocha.simulator.parameters.StructuralParameters;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;

public class Node implements Serializable {

	private static final long serialVersionUID = -6902741116934537381L;

	/* NodeType */
	public enum NodeType {
		SOURCE, SINK, NORMAL, SOLO
	}

	/*
	 * Node identifiers: id - node ID; dagID - the ID of its DAG; daginstNo - the
	 * instance Number of its DAG.
	 */
	private final int id;
	private final int dagID;
	private int dagInstNo = Integer.MIN_VALUE;

	/*
	 * The layer of the node in the DAG strcuture
	 */
	private final int layer;

	/*
	 * The type of the node, can be SOURCE, SINK, or Normal
	 */
	public NodeType type;

	/*
	 * The worst-case execution time of the node
	 */
	public long WCET;
	// private long WCETinRange;

	public boolean isCond = false;
	public int nCond = 0;
	public List<Double> prob = new ArrayList<>();
	public List<Long> correspondLen = new ArrayList<>();
	// 其实是branch to prob / length
	public Map<Integer, Double> idx2prob = new HashMap<>();
	public Map<Integer, Long> idx2len = new HashMap<>();
	public List<Double> utils = new ArrayList<>();
	Random r;// to choose the path
	public boolean isSub = false;
	public int confather = -1;

	/*
	 * A list of successors/predecessors of the node.
	 */
	private List<Node> successors;
	private List<Node> predecessors;

	public List<List<Node>> allPaths = new ArrayList<>();
	public List<Long> allPathLength = new ArrayList<>();

	public int pathNum = 0;
	public long pathET;

	// public long globalMaxPathET = -1;
	// public long globalMaxPathNum = -1;

	public double[] weights = new double[6];

	public double sensitivity = -1;

	/*
	 * The priority of the node
	 */
	public int priority = -1;

	/*
	 * Is the node belongs to the critical path of the DAG.
	 */
	public boolean isCritical = false;

	/*
	 * Does the node has faults in execution time?
	 */
	public boolean hasFaults = false;

	/*
	 * Simulation parameters
	 */
	public long release = -1;
	public long start = -1;
	public long finishAt = -1;
	public boolean finish = false;

	public int partition = -1;
	public int affinity = -1;

	public int delayed = -1;
	public long expectedET = -1;
	public long expectedCache = -1;
	public long[] expectedETPerCore;
	public boolean isDelayed = false;

	public double variation = 0;

	public int fixed_order = -1;
	public int fixed_allocation = -1;

	public int repeat_fixed_order = -1;
	public int repeat_fixed_allocation = -1;

	/*
	 * The variability of the node
	 */
	public RecencyProfile crp;
	public RecencyProfile crp_synth;

	public CacheVariabilityProfile cvp;

	boolean isReal;

	Random rng;
	/*
	 * public CacheHierarchy cache = new CacheHierarchy(SystemParameters.coreNum,
	 * SystemParameters.cacheLevel,
	 * SystemParameters.Level2CoreNum);
	 */

	// public List<Long> speeds = new ArrayList<>();

	public Node(int layer, NodeType type, int id, int dagID, RecencyProfile crp, Random rng) {
		this(-1, layer, type, id, dagID, crp, false, rng);
	}

	public Node(long WCET, int layer, NodeType type, int id, int dagID, RecencyProfile crp, boolean real, Random rng) {
		this.WCET = WCET;
		this.layer = layer;
		this.type = type;

		this.id = id;
		this.dagID = dagID;

		this.successors = new ArrayList<>();
		this.predecessors = new ArrayList<>();

		this.rng = rng;

		// this.r = new Random(id);

		// this.crp = new CacheRecencyProfile(this, SystemParameters.coreNum,
		// 1000);

		this.crp = crp;
		this.crp_synth = new RecencyProfileSyn(crp.cache, crp.cache.coreNum, 1000);

		this.cvp = new CacheVariabilityProfile(this, SystemParameters.err_median,
				((double) this.rng.nextInt(SystemParameters.err_range + 1) / (double) 100) / 3.0, rng);

		this.isReal = real;
	}

	@Override
	public String toString() {
		return "Node " + dagID + "_" + dagInstNo + "_" + id + ", C:" + WCET + ", Fixed Order: " + fixed_order
				+ ", Fixed Alloc: " + fixed_allocation + ", has Fault: " + hasFaults;
		// return "Node " + dagID + "_" + id + ", C:" + WCET + ", in: " +
		// predecessors.size() + ", out: "
		// + successors.size() + ", in+out: " + predecessors.size() + successors.size()
		// + ", pathNum: " + pathNum;
		// return "Node " + dagID + "_" + dagInstNo + "_" + id + ", C:" + WCET + ", P:"
		// + partition + ", A:" + affinity;
	}

	public String getFullName() {
		return "N " + dagID + "_" + dagInstNo + "_" + id;
	}

	public String getShortName() {
		return "N_" + id;
	}

	public String getExeInfo() {
		return "Node " + dagID + "-" + dagInstNo + "_" + id + ": " + WCET + ", starts: " + start + ", finish: "
				+ finishAt + ", duration: " + (finishAt - start) + ", partition: " + partition + ", affinity: "
				+ affinity;
	}

	public void printExeInfo(String prefix) {
		System.out.printf(prefix
				+ " Node %2d_%2d_%2d    ---    WCET: %5d, starts: %5d, finishes: %5d, duration: %5d, partition: %2d, affinity: %2d\n",
				dagID, dagInstNo, id, WCET, start, finishAt, (finishAt - start), partition, affinity);
	}

	/*
	 * Return the WORST-CASE execution time of the node
	 */
	public long getWCET() {
		return WCET;
	}

	public void setWCET(long wCET) {
		WCET = wCET;
	}

	public int getLayer() {
		return layer;
	}

	public NodeType getType() {
		return type;
	}

	public void addChildren(Node n) {
		successors.add(n);
	}

	public List<Node> getChildren() {
		return successors;
	}

	public List<Node> getParent() {
		return predecessors;
	}

	public void removeParent(Node ToDelect) {
		predecessors.remove(ToDelect);
	}

	public void addParent(Node n) {
		predecessors.add(n);
	}

	public int getDagID() {
		return dagID;
	}

	public int getId() {
		return id;
	}

	public int getDagInstNo() {
		return dagInstNo;
	}

	public void setDagInstNo(int dagInstNo) {
		this.dagInstNo = dagInstNo;
	}

	/*
	* 生成branches的数量
	* */
	public void generateCondPath() {
//		long seed = 1000;
//		Random rn = new Random(seed);
//		nCond = rn.nextInt(SystemParameters.MAX_Cond - 1) + 2;
		nCond = 3;

		// 同时生成对应的概率
		generateProb();
	}

	/*
	* 为每个branch生成概率
	* */
	public void generateProb(){
		List<Double> points = new ArrayList<>();
		long seed = 1000; // 固定的种子
		Random random = new Random(seed);

		for (int i = 0; i < nCond - 1; i++) {
			points.add(random.nextDouble()); // 使用 Random 生成随机数
		}

		// 添加边界值
		points.add(0.0);
		points.add(1.0);

		// 排序
		Collections.sort(points);

		for (int i = 1; i < points.size(); i++) {
			double randomNumber = points.get(i) - points.get(i - 1);
			prob.add(randomNumber);
		}
	}

	public DirectedAcyclicGraph choosePath() {
		if (r == null) {
			r = new Random(getDagInstNo() * getId());
		}
		double choice = r.nextDouble();
		double sum = 0;
		int seedPath = 0;
		for(double i : prob){
			sum += i;
			if(sum >= choice) break;
			else seedPath ++;
		}
		seedPath = Math.min(seedPath, nCond - 1);// the selected path

		return choosePathwithIndex(seedPath);
	}

	public DirectedAcyclicGraph choosePathwithIndex(int index) {
		int seedPath = index;

		SystemParameters.utilforTask = utils.get(seedPath);
		if (SystemParameters.utilforTask == 0)
			System.exit(-1);

		PathGenerator gen = new PathGenerator(SystemParameters.coreNum, 1, true, true, null, 1000 + seedPath, true,
				false);
		Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(1, -1, -1,
				null, false);
		for (Node tmp : sys.getFirst().get(0).getFlatNodes())
			tmp.isSub = true;
		return sys.getFirst().get(0);
	}

	// public Node deepCopy() {
	// Node copy = new Node(this.WCET, this.layer, this.type, this.id, this.dagID,
	// this.rng);
	// copy.release = release;
	// copy.start = start;
	// copy.finish = finish;
	// copy.finishAt = finishAt;
	// copy.partition = partition;
	// copy.affinity = affinity;
	// copy.delayed = delayed;
	// return copy;
	// }

}