package uk.ac.york.mocha.simulator.entity;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;

public class RecencyProfileReal extends RecencyProfile implements Serializable {

	private static final long serialVersionUID = -6902741116934537381L;

	String job_name;
	int job_id;

	public double highWaterMark;
	public double WCET;
	public double medainET;

	double[] breaks;
	double[] slopes;
	double[] intercepts;

	public RecencyProfileReal(CacheHierarchy cache, RecencyType type, String job_name, int job_id, double highWaterMark,
			double WCET, double medainET, double[] breaks, double[] slopes, double[] intercepts) {

		super(cache, type);

		this.job_name = job_name;
		this.job_id = job_id;

		this.highWaterMark = highWaterMark;
		this.WCET = WCET;
		this.medainET = medainET;

		this.breaks = breaks;
		this.slopes = slopes;
		this.intercepts = intercepts;

	}

	public String toString() {
		return "Name:" + job_name + ", id: " + job_id + ", WCET: " + WCET;
	}

	public Pair<Pair<Long, Double>, Integer> computeET(long time, List<List<Node>> history_level1,
			List<List<Node>> history_level2, List<Node> history_level3, Node n, int proc, boolean cacheAware,
			long additionalTime, long variation, boolean error) {

		Pair<Long, Integer> res = computeET(time, history_level1, history_level2, history_level3, n, proc, cacheAware,
				additionalTime);

		if (n != null && error) {
			double err = n.cvp.rng.nextDouble() * 0.1;
//			System.out.println(err);
			// double err = n.cvp.getMedian();

			if (err < -1 || err > 1) {
				System.out.println("Error Value: " + err);
				System.exit(-1);
			}

			// System.out.println(n.getFullName() + " : err: " + err);

			long ETwithErr = (long) Math.ceil((double) res.getFirst() * (1.0 + err));

			return new Pair<Pair<Long, Double>, Integer>(new Pair<Long, Double>(ETwithErr, (double) 0), res.getSecond());
		} else
			return new Pair<Pair<Long, Double>, Integer>(new Pair<Long, Double>(res.getFirst(), (double) 0), res.getSecond());

	}

	private Pair<Long, Integer> computeET(long time, List<List<Node>> history_level1, List<List<Node>> history_level2,
			List<Node> history_level3, Node n, int proc, boolean cacheAware, long additionalTime) {
		/**
		 * Compute recency distance at each cache level
		 */
		long ET = n.getWCET();
		if (!cacheAware)
			return new Pair<Long, Integer>(ET, 4);

		// switch (this.type) {
		// case TIME_DEFAULT:

		/**************************************************************************
		 ************************* level 1 recency distance ************************
		 ***************************************************************************/
		long leve1Time = time != -1 ? time
				: getTimeofLastIndex(history_level1.get(proc), n, breaks[0]) + additionalTime;

		if (0 <= leve1Time && leve1Time <= breaks[0]) {
			double speedUp = getET(leve1Time, 0);
			return new Pair<Long, Integer>((long) Math.ceil((double) n.getWCET() * speedUp), 1);
		}

		/**************************************************************************
		 ************************* level 2 recency distance ************************
		 ***************************************************************************/
		int clusterID = cache.getLevel2ClusterID(proc);
		long level2Time = time != -1 ? time
				: getTimeofLastIndex(history_level2.get(clusterID), n, breaks[1]) + additionalTime;
		if (breaks[0] <= level2Time && level2Time < breaks[1]) {
			double speedUp = getET(level2Time, 1);
			return new Pair<Long, Integer>((long) Math.ceil((double) n.getWCET() * speedUp), 2);
		}

		/**************************************************************************
		 ************************* level 3 recency distance ************************
		 ***************************************************************************/
		long level3Time = time != -1 ? time : getTimeofLastIndex(history_level3, n, breaks[2]) + additionalTime;
		if (breaks[0] <= level3Time && level3Time < breaks[2]) {
			double speedUp = getET(level3Time, 2);
			return new Pair<Long, Integer>((long) Math.ceil((double) n.getWCET() * speedUp), 3);
		}

		return new Pair<Long, Integer>(ET, 4);

	}

	/**
	 * Calculate the speed-up by reading the CRP table Percentage of execution time
	 * speedup
	 */
	public double getET(double rd, int breakpoint) {

		// Find linear piece where rd fits
		// int c = 1;
		// while (c - 1 < breaks.length && rd < breaks[c - 1]) {
		// ++c;
		// }

		// Fit rd to the linear pc
		double crp = slopes[breakpoint] * rd + intercepts[breakpoint];

		// Normalize crp in interval [CPR_MIN, 1.0]
		if (crp > 1.0) {
			return 1.0;
		}
		if (crp <= 0.01) {
			return 0.01;
		}

		return crp;
	}

}
