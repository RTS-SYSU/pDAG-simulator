package uk.ac.york.mocha.simulator.parameters;

import java.io.Serializable;
import java.util.Random;

public class StructuralParameters implements Serializable {

	private static final long serialVersionUID = -4517859999449660269L;

	/* DAGType */
	public enum NodeType { // TODO: to support NFJ DAGs
		NORMAL, NFJ
	}

	/* parameters for normal DAGs */
	private final int parallelism;
	private final double connect_prob;
	private final int layers;

	public final int parallelism_min;
	public final int parallelism_max;

	/* DAG structure Parameters */
	public StructuralParameters(int maxLayer, int minLayer, int maxParall, int minparall, double connect_prob,
			Random rng) {

		if (maxLayer - minLayer < 0) {
			System.err.println("maxLayer-minLayer < 0 !!");
			System.err.println("maxLayer: " + maxLayer + "    minLayer: " + minLayer);
			System.exit(-1);
		}

		if (maxParall - minparall < 0) {
			System.err.println("maxParal-minParal < 0 !!");
			System.err.println("maxParal: " + maxParall + "    minParal: " + minparall);
			System.exit(-1);
		}

		if (maxParall == minparall) {
			this.parallelism = maxParall;
			this.parallelism_max = maxParall;
			this.parallelism_min = minparall;
		} else {
			this.parallelism = rng.nextInt(maxParall - minparall) + minparall;
			this.parallelism_max = maxParall;
			this.parallelism_min = minparall;
		}

		this.connect_prob = connect_prob;

		if (maxLayer == minLayer)
			this.layers = maxLayer;
		else
			this.layers = rng.nextInt(maxLayer - minLayer) + minLayer;
	}

	public int getParallelism() {
		return parallelism;
	}

	public double getConnect_prob() {
		return connect_prob;
	}

	public int getLayers() {
		return layers;
	}

}
