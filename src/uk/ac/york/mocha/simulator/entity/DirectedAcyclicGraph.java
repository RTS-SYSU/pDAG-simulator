package uk.ac.york.mocha.simulator.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import uk.ac.york.mocha.simulator.entity.Node.NodeType;
import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.parameters.SchedulingParameters;
import uk.ac.york.mocha.simulator.parameters.StructuralParameters;

public class DirectedAcyclicGraph implements Serializable {

	private static final long serialVersionUID = -4076503208112904549L;

	public final int id;
	public int instanceNo = -1;
	public long totalInstNum = -1;
	public final String name;

	/* Scheduling parameters of Sporadic DAG task */
	public final SchedulingParameters sched_param;

	/* Structural parameters of Sporadic DAG task */
	private final StructuralParameters dag_param;

	/* DAG parameters */
	private Node source;
	private Node sink;
	private int nodeNum;
	private int layers;

	/* Self-defined list structure */
	private List<Node> flatNodes;
	private List<List<Node>> layeredNodes;
	private List<ImmutablePair<Node, Node>> edges;

	/* Third party graph structure */
	private Graph<Node, DefaultEdge> graph;

	public List<Node> longestPath;
	private List<Node> shortestPath;

	private Random rng;
	public boolean instantiated = false;

	/*
	 * Run-time parameters
	 */
	public long releaseTime = Long.MAX_VALUE;
	public long startTime = Long.MAX_VALUE;
	public long finishTime = Long.MAX_VALUE;
	public List<Node> allocNodes = new ArrayList<>();

	public long variation = 0;

	/*
	 * Offline parameters
	 */
	public boolean hard = false;

	// DecimalFormat df = new DecimalFormat("#.###");

	CacheHierarchy cache;
	RecencyProfile globalCRP;
	List<RecencyProfile> crps;

	public DirectedAcyclicGraph(SchedulingParameters sched_param, StructuralParameters dag_param, CacheHierarchy cache, int id, Random rng,
			boolean hard) {
		this(sched_param, dag_param, null, cache, id, rng, hard, false);
	}

	public DirectedAcyclicGraph(SchedulingParameters sched_param, StructuralParameters dag_param, List<RecencyProfile> crps,
			CacheHierarchy cache, int id, Random rng, boolean hard, boolean real) {

		this.id = id;
		this.name = "DAG " + id;

		this.rng = rng;

		this.sched_param = sched_param;
		this.dag_param = dag_param;

		this.layeredNodes = new ArrayList<>();
		this.edges = new ArrayList<>();

		this.layers = dag_param.getLayers();

		this.graph = new DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge.class);

		this.longestPath = new ArrayList<>();
		this.shortestPath = new ArrayList<>();

		this.crps = crps;

		if (crps == null)
			this.globalCRP = new RecencyProfileSyn(cache, id, 100);

		if (real)
			constructDAGForSteven();
		else
			constructDAG();

	}

	public void reset() {

		// TODO
		// Random rng = new Random(1000);

		this.startTime = Long.MAX_VALUE;
		this.finishTime = Long.MAX_VALUE;
		this.allocNodes.clear();

		for (Node n : flatNodes) {
			n.release = -1;
			n.start = -1;
			n.finish = false;
			n.isDelayed = false;

			n.finishAt = -1;
			n.partition = -1;
			n.affinity = -1;
			n.delayed = -1;
			n.expectedET = -1;
			n.rng = n.cvp.rng = rng;

		}

	}

	/*****************************************************************
	 ******* Get Mutliple instances of one sporadic DAG task ********* NOTE: This
	 * method can only be invoked once! ***********
	 *****************************************************************/
	public List<DirectedAcyclicGraph> getInstances(long instanceNum) {

		/*
		 * Check whether it is the FIRST call
		 */
		if (instantiated) {
			System.err.println("Error: DirectedAcyclicGraph.getInstances() can only be invoked once! ");
			System.exit(-1);
			return null;
		} else
			instantiated = true;

		/*
		 * Deep copy the DAG
		 */
		List<DirectedAcyclicGraph> instances = new ArrayList<>();
		for (int i = 0; i < instanceNum; i++) {
			DirectedAcyclicGraph ins = deepCopy();
			ins.instanceNo = i;
			ins.releaseTime = i * sched_param.getPeriod();

			ins.instantiated = true;

			for (Node n : ins.flatNodes)
				n.setDagInstNo(i);

			instances.add(ins);
		}

		return instances;
	}

	/******************************************************************
	 ******************** Generate DAG structure ********************** This method does not depend
	 * on external library! *********
	 ******************************************************************/
	public void constructDAG() {
		if (layeredNodes.size() > 0) {
			System.err.println("The DAG task has already being generated!");
			return;
		}

		int nodeCounter = 1;

		/*
		 * Helper lists for constructing edges
		 */
		List<Node> parents = new ArrayList<>(); // potential parent nodes for a
												// layer
		List<Node> childless = new ArrayList<>(); // nodes without successors
		List<Node> orphans = new ArrayList<>(); // nodes without predecessors

		/*
		 * initialize source and sink node
		 */
		this.source = new Node(0, NodeType.SOURCE, 0, id, globalCRP, rng);
		// this.graph.addVertex(source);

		List<Node> firstLayer = new ArrayList<>();
		firstLayer.add(source);
		this.layeredNodes.add(firstLayer);

		parents = firstLayer;
		childless.addAll(parents);

		/*
		 * constrcut the DAG layer by layer
		 * 
		 */

		// System.out.println(layers);

		for (int l = 1; l < layers - 1; l++) {

			/*
			 * generate nodes for this layer
			 */
			int nodeNum = rng.nextInt(dag_param.parallelism_max - dag_param.parallelism_min) + dag_param.parallelism_min;
			// int nodeNum = rng.nextInt(dag_param.getParallelism()) + 1;
			// System.out.println(nodeNum);
			List<Node> nodePerLayer = new ArrayList<>();

			for (int k = 0; k < nodeNum; k++) {
				Node n = new Node(l, NodeType.NORMAL, nodeCounter, id, globalCRP, rng);

				nodePerLayer.add(n);
				nodeCounter++;
			}

			this.layeredNodes.add(nodePerLayer);
			orphans.addAll(nodePerLayer);

			/*
			 * generate edges for these nodes
			 */
			for (Node n : nodePerLayer) {

				/*
				 * generate edges for these nodes
				 */
				for (Node parent : parents) {
					if (rng.nextDouble() < this.dag_param.getConnect_prob()) {
						/*
						 * connect two nodes
						 */
						ImmutablePair<Node, Node> e = new ImmutablePair<Node, Node>(parent, n);

						edges.add(e);

						parent.addChildren(n);
						n.addParent(parent);

						/*
						 * update the lists of orphan and childless nodes
						 */
						if (orphans.contains(n))
							orphans.remove(n);
						if (childless.contains(parent))
							childless.remove(parent);
					}
				}
			}

			/* update parents for the next layer */
			parents = nodePerLayer;
			childless.addAll(parents);

			/* connect orphans to the source node */
			for (Node n : orphans) {
				ImmutablePair<Node, Node> e = new ImmutablePair<Node, Node>(source, n);

				edges.add(e);
				source.addChildren(n);
				n.addParent(source);
			}
			orphans.clear();

		}

		/* generate the sink node */
		this.sink = new Node(layers + 1, NodeType.SINK, nodeCounter, id, globalCRP, rng); // TODO
		// add
		// links,
		// make
		// it
		// random.
		List<Node> lastLayer = new ArrayList<>();
		lastLayer.add(sink);
		this.layeredNodes.add(lastLayer);

		/*
		 * Link all childless nodes to the sink node
		 */
		for (Node n : childless) {
			ImmutablePair<Node, Node> e = new ImmutablePair<Node, Node>(n, sink);
			edges.add(e);
			n.addChildren(sink);
			sink.addParent(n);
		}
		childless.clear();

		this.flatNodes = layeredNodes.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
		flatNodes.sort((n1, n2) -> Integer.compare(n1.getId(), n2.getId()));
		this.nodeNum = flatNodes.size();

		for (Node n : flatNodes)
			this.graph.addVertex(n);

		for (ImmutablePair<Node, Node> e : edges)
			this.graph.addEdge(e.left, e.right);

	}

	/******************************************************************
	 ******************** Generate DAG structure ********************** This method does not depend
	 * on external library! *********
	 ******************************************************************/
	public void constructDAGForSteven() {

		this.source = new Node(0, NodeType.SOLO, 0, id, crps.get(0), rng);
		List<Node> firstLayer = new ArrayList<>();
		firstLayer.add(source);
		this.layeredNodes.add(firstLayer);

		this.flatNodes = layeredNodes.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
		flatNodes.sort((n1, n2) -> Integer.compare(n1.getId(), n2.getId()));
		this.nodeNum = flatNodes.size();

		for (Node n : flatNodes)
			this.graph.addVertex(n);

		for (ImmutablePair<Node, Node> e : edges)
			this.graph.addEdge(e.left, e.right);
	}

	public void constructNFJDAG(int fanInNum) {

		int nodeCounter = 1;

		/*
		 * Helper lists for constructing edges
		 */
		List<Node> parents = new ArrayList<>(); // potential parent nodes for a
												// layer
		List<Node> nodeCurrentLayer = new ArrayList<>();
		/*
		 * initialize source and sink node
		 */
		this.source = new Node(0, NodeType.SOURCE, 0, id, globalCRP, rng);

		List<Node> firstLayer = new ArrayList<>();
		firstLayer.add(source);
		this.layeredNodes.add(firstLayer);

		parents = firstLayer;

		/*
		 * constrcut the DAG layer by layer
		 */
		for (int l = 1; l < fanInNum * 2; l++) {

			if (l % 2 == 1) {
				/*
				 * generate nodes for fan-in
				 */
				int nodeNum = rng.nextInt(dag_param.parallelism_max - dag_param.parallelism_min) + dag_param.parallelism_min;
				List<Node> nodePerLayer = new ArrayList<>();

				for (int k = 0; k < nodeNum; k++) {
					Node n = new Node(l, NodeType.NORMAL, nodeCounter, id, globalCRP, rng);

					nodePerLayer.add(n);
					nodeCounter++;
				}

				this.layeredNodes.add(nodePerLayer);
				nodeCurrentLayer = nodePerLayer;
			} else {
				/*
				 * generate nodes for fan-out
				 */
				int nodeNum = 1;
				List<Node> nodePerLayer = new ArrayList<>();

				for (int k = 0; k < nodeNum; k++) {
					Node n = new Node(l, NodeType.NORMAL, nodeCounter, id, globalCRP, rng);

					nodePerLayer.add(n);
					nodeCounter++;
				}

				this.layeredNodes.add(nodePerLayer);
				nodeCurrentLayer = nodePerLayer;
			}

			for (Node n : nodeCurrentLayer) {
				for (Node parent : parents) {
					ImmutablePair<Node, Node> e = new ImmutablePair<Node, Node>(parent, n);

					edges.add(e);

					parent.addChildren(n);
					n.addParent(parent);
				}
			}

			/* update parents for the next layer */
			parents = nodeCurrentLayer;
		}

		/* generate the sink node */
		this.sink = new Node(fanInNum * 2 + 1, NodeType.SINK, nodeCounter, id, globalCRP, rng); // TODO
		// add
		// links,
		// make
		// it
		// random.
		List<Node> lastLayer = new ArrayList<>();
		lastLayer.add(sink);
		this.layeredNodes.add(lastLayer);

		/*
		 * Link all childless nodes to the sink node
		 */
		for (Node n : parents) {
			ImmutablePair<Node, Node> e = new ImmutablePair<Node, Node>(n, sink);
			edges.add(e);
			n.addChildren(sink);
			sink.addParent(n);
		}

		this.flatNodes = layeredNodes.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
		flatNodes.sort((n1, n2) -> Integer.compare(n1.getId(), n2.getId()));
		this.nodeNum = flatNodes.size();

		for (Node n : flatNodes)
			this.graph.addVertex(n);

		for (ImmutablePair<Node, Node> e : edges)
			this.graph.addEdge(e.left, e.right);
	}

	int[] path; // used to store temporary path
	int[] bestPath; // used to store temporary path

	int length = 0; // length of the path
	int bestLength = 0; // length of the longest path

	long distance = 0; // distance of the path -- the length gyy need
	public long bestDistance; // distance of the longest path

	int[] visited; // used to mark a node as visited

	public List<List<Node>> allpaths = new ArrayList<>();

	public void findPath(boolean longest) {
		distance = 0;
		length = 0;
		bestLength = 0;
		longestPath = new ArrayList<>();

		visited = new int[flatNodes.size()];

		path = new int[flatNodes.size()];
		bestPath = new int[flatNodes.size()];

		for (int i = 0; i < flatNodes.size(); i++) {
			path[i] = -1;
			bestPath[i] = -1;
		}

		/*
		 * compute a longest path from begin to end
		 */
		bestDistance = longest ? Long.MIN_VALUE : Long.MAX_VALUE;
		dsf(source, longest);
		if (bestDistance != Long.MIN_VALUE && bestDistance != Long.MAX_VALUE) {
			for (Integer id : bestPath) {
				if (id != -1) {
					if (longest)
						longestPath.add(flatNodes.get(id));
					else
						shortestPath.add(flatNodes.get(id));
				}

			}

			// System.out.println("Critical Path: "
			// + (longest ? Arrays.toString(longestPath.toArray()) :
			// Arrays.toString(shortestPath.toArray())));
			// System.out.println("distance: " + bestDistance + " edges: " +
			// bestLength);
		}

		else {
			System.err.println("Did not find critical path");
			System.exit(-1);
		}

		if (longest) {
			for (Node n : longestPath)
				n.isCritical = true;
		}

		int maxpathNum = -1;
		long maxPathET = -1;
		long maxET = -1;
		long maxInOutDegree = -1;
		long maxInDegree = -1;
		long matOutDegree = -1;

		for (Node n : flatNodes) {
			int count = 0;
			for (int i = 0; i < allpaths.size(); i++) {
				if (allpaths.get(i).contains(n))
					count++;
			}
			n.pathNum = count;

			if (maxpathNum < count)
				maxpathNum = count;

			if (maxET < n.getWCET())
				maxET = n.getWCET();

			if (maxInDegree < n.getParent().size())
				maxInDegree = n.getParent().size();

			if (matOutDegree < n.getChildren().size())
				matOutDegree = n.getChildren().size();

			if (maxInOutDegree < n.getParent().size() + n.getChildren().size())
				maxInOutDegree = n.getParent().size() + n.getChildren().size();
		}

		for (Node n : flatNodes) {
			for (List<Node> p : allpaths) {
				if (p.contains(n)) {
					n.allPaths.add(p);
					n.allPathLength.add(p.stream().mapToLong(n1 -> n1.getWCET()).sum());
				}
			}
			long localMaxPathET = n.allPathLength.stream().mapToLong(c -> c).max().getAsLong();
			n.pathET = localMaxPathET;

			if (maxPathET < localMaxPathET)
				maxPathET = localMaxPathET;
		}

		for (Node n : flatNodes) {
			// n.globalMaxPathET = maxPathET;
			// n.globalMaxPathNum = maxpathNum;

			n.weights[0] = Double.parseDouble(df.format((double) n.getWCET() / (double) maxET));
			n.weights[1] = Double.parseDouble(df.format((double) n.pathET / (double) maxPathET));
			n.weights[2] = Double.parseDouble(df.format((double) n.getParent().size() / (double) maxInDegree));
			n.weights[3] = Double.parseDouble(df.format((double) n.getChildren().size() / (double) matOutDegree));
			n.weights[4] = Double
					.parseDouble(df.format((double) (n.getParent().size() + n.getChildren().size()) / (double) maxInOutDegree));
			n.weights[5] = Double.parseDouble(df.format((double) n.pathNum / (double) maxpathNum));
			
		}

	}

	private void dsf(Node current, boolean longest) {
		visited[current.getId()] = 1;
		path[length++] = current.getId();

		if (current.getId() == sink.getId()) {
			distance += current.getWCET();

			// System.out.println("Path: " +
			// Arrays.toString(Arrays.copyOfRange(path, 0, length)));

			List<Node> onepath = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				onepath.add(flatNodes.get(path[i]));
			}
			allpaths.add(onepath);

			if ((longest && distance > bestDistance) || (!longest && distance < bestDistance)) {
				// System.out.println("previous longest path: " +
				// Arrays.toString(bestPath) + " length: " + bestDistance);

				for (int i = 0; i < bestPath.length; i++)
					bestPath[i] = -1;

				for (int i = 0; i < length; i++)
					bestPath[i] = path[i];

				bestLength = length;
				bestDistance = distance;

				// System.out.println("previous longest path: " +
				// Arrays.toString(bestPath) + " length: " + bestDistance);

			}
			distance -= current.getWCET();

		} else {
			List<Node> succ = current.getChildren();

			for (int i = 0; i < succ.size(); i++) {
				Node suc = succ.get(i);

				if (visited[suc.getId()] == 0) {
					distance += current.getWCET();
					dsf(suc, longest);
					distance -= current.getWCET();
				}
			}
		}

		visited[current.getId()] = 0;
		length--;
	}

	DecimalFormat df = new DecimalFormat("#.###");

	public double[] getDelayAndETinAvg() {
		double[] res = new double[2];
		long delay = 0;
		long et = 0;

		for (Node n : flatNodes) {
			delay += n.start - n.release;
			et += n.finishAt - n.start;
		}

		res[0] = Double.parseDouble(df.format((double) delay / (double) flatNodes.size()));
		res[1] = Double.parseDouble(df.format((double) et / (double) flatNodes.size()));

		return res;
	}

	public List<List<Long>> getAllDelayAndETs() {

		List<Long> delays = flatNodes.stream().map(n -> (n.start - n.release)).collect(Collectors.toList());
		List<Long> ets = flatNodes.stream().map(n -> (n.finishAt - n.start)).collect(Collectors.toList());

		List<List<Long>> res = new ArrayList<>();
		res.add(delays);
		res.add(ets);
		return res;
	}

	public Node getSource() {
		return source;
	}

	public Node getSink() {
		return sink;
	}

	public SchedulingParameters getSchedParameters() {
		return sched_param;
	}

	public StructuralParameters getDAGParameters() {
		return dag_param;
	}

	public int getNodeNum() {
		return nodeNum;
	}

	public List<Node> getFlatNodes() {
		return flatNodes;
	}

	public String getName() {
		return name + "-" + instanceNo;
	}

	/*****************************************************************
	 ************** A good way to deep copy an object ****************
	 *****************************************************************/
	public synchronized DirectedAcyclicGraph deepCopy() {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);

			DirectedAcyclicGraph dag = (DirectedAcyclicGraph) ois.readObject();

			oos.flush();
			baos.flush();

			baos.close();
			oos.close();
			bais.close();
			ois.close();

			return dag;
		} catch (EOFException eof) {
			eof.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String toString() {
		// String out =
		// "************************************************************************************************\n";
		// String out = getName() + ", makespan: " + (finishTime - startTime) +
		// ": \n";
		String out = getName() + "\n";

		// out += "Nodes by layer: \n";
		//
		// for (List<Node> nodesPerLayer : this.layeredNodes) {
		// for (Node n : nodesPerLayer) {
		// out += n.toString() + "; ";
		// }
		// out += "\n";
		// }
		// out += "\n";
		//
		// out += "Edges by Nodes: \n";
		//
		// for (List<Node> nodesPerLayer : this.layeredNodes) {
		// for (Node n : nodesPerLayer) {
		// List<ImmutablePair<Node, Node>> childrenEdge = new ArrayList<>();
		// for (ImmutablePair<Node, Node> edge : edges) {
		// if (edge.left.toString().equals(n.toString()))
		// childrenEdge.add(edge);
		// }
		//
		// childrenEdge.sort((c1, c2) -> Utils.compareNodeByID(null, c1.right,
		// c2.right));
		//
		// out += n.getShortName() + " -> ";
		// for (ImmutablePair<Node, Node> edge : childrenEdge) {
		// out += edge.right.getShortName() + ", ";
		// }
		// out += "\n";
		// }
		//
		// }
		//
		// out += "\nlongest path:\n";
		// for (int i = 0; i < longestPath.size(); i++) {
		// out += longestPath.get(i).getShortName();
		//
		// if (i != longestPath.size() - 1)
		// out += " -> ";
		// }
		//
		// out +=
		// "\n************************************************************************************************\n";

		return out;

		// return getName();
	}

	public String printExeInfo() {
		String out = "************************************************************************************************\n";
		out += getName() + " starts at " + source.start + ", finishes at " + finishTime + " \n";
		out += "Nodes by layer: \n";

		for (List<Node> nodesPerLayer : this.layeredNodes) {
			for (Node n : nodesPerLayer) {
				out += n.getExeInfo() + "    \n";
			}
			out += "\n";
		}
		out += "\n";

		out += "************************************************************************************************\n";

		return out;
	}

	// public static void main(String args[]) {
	//
	// for (int i = 0; i < 999999; i++) {
	// System.out.println("********************* " + i + "
	// *********************");
	// SystemGenerator gen = new SystemGenerator(SystemParameters.coreNum, 8,
	// false, true, null, i, true,
	// SystemParameters.printGen);
	// gen.generatedDAGInstancesInOneHP(1, -1, null, false);
	//
	// }
	//
	// // for (int i = 0; i < dags.size(); i++) {
	// //
	// System.out.println("*****************************************************");
	// // System.out.println("********************** DAG " + i + "
	// // **********************");
	// //
	// System.out.println("*****************************************************");
	// //
	// // DirectedAcyclicGraph dag = dags.get(i);
	// // System.out.println(dag.toString());
	// //
	// //
	// // System.out.println("\n\nDONE\n\n\n\n");
	// // }
	//
	// // for (int i = 0; i < 1; i++) {
	// // int minLayer = 50;
	// // int maxLayer = 80;
	// // int parallelism = rng.nextInt(10)+2;
	// // double connectProb = 0.5;
	// //
	// // SchedulingParameters sched_param = new SchedulingParameters(10, 100,
	// // 100, 50000, 1, 0);
	// // StructuralParameters dag_param = new StructuralParameters(maxLayer,
	// // minLayer, parallelism, 1, connectProb,
	// // rng);
	// // DirectedAcyclicGraph dag = new DirectedAcyclicGraph(sched_param,
	// // dag_param, 0, rng.nextInt(), false);
	// //
	// // System.out.println(dag.toString());
	// //
	// //
	// System.out.println("\n\n------------------------------------------------------\n\n");
	// //
	// // List<DirectedAcyclicGraph> instances = dag.getInstances(10);
	// //
	// // for (DirectedAcyclicGraph d : instances)
	// // System.out.println(d.instanceNo + " " + d.startTime);
	// //
	// //
	// System.out.println("\n\n------------------------------------------------------\n\n");
	// // System.out.println("Finding longest path...");
	// // dag.findPath(true);
	// // }
	// //
	// // System.out.println("finished");
	//
	// }
}