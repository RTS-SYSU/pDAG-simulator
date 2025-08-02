package uk.ac.york.mocha.simulator.entity;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

public class CacheVariabilityProfile implements Serializable {

	private static final long serialVersionUID = -6902741116934537381L;

	static DecimalFormat df = new DecimalFormat("#.###");

	public double median;
	public double range;

	public Random rng;
	// int seed;

	Node n;

	public CacheVariabilityProfile(Node n, double median, double range, Random rng) {
		this.n = n;

		this.median = Double.parseDouble(df.format(median));
		this.range = Double.parseDouble(df.format(range));

		// this.seed = seed;
		this.rng = rng;
	}

	public double getMedian() {
		return median;
	}

	public double getRange() {
		return range;
	}

	public double getVary(int cacheLevel) {

//		double ran = (double) rng.nextInt(100) / (double) 100; // rng.nextGaussian();
//		double vary = median + (ran * range);
//
//		double out = Double.parseDouble(df.format(vary));
//
//		if (out >= 1)
//			out = 1;
//
//		if (out <= -1)
//			out = -1;

		double ran = -1;
		double out = -1;

		switch (cacheLevel) {
		case 1:
			ran = (double) rng.nextInt(10 + 1) / (double) 100; // rng.nextGaussian();
			out = Double.parseDouble(df.format(ran));
			break;
		case 2:
			ran = (double) rng.nextInt(20 + 1) / (double) 100; // rng.nextGaussian();
			out = Double.parseDouble(df.format(ran)) + 0.1;
			break;
		case 3:
			ran = (double) rng.nextInt(30 + 1) / (double) 100; // rng.nextGaussian();
			out = Double.parseDouble(df.format(ran)) + 0.2;
			break;
		case 4:
			ran = 0; //(double) rng.nextInt(10 + 1) / (double) 100; // rng.nextGaussian();
			out = Double.parseDouble(df.format(ran));
			break;
		default:
			break;
		}

		return out;
	}

	public static void main(String args[]) {

		CacheVariabilityProfile cvp = new CacheVariabilityProfile(null, 0, 1.0 / 3.0, new Random(1000));

		for (int i = 0; i < 10; i++) {
			System.out.println(cvp.getVary(0));
		}
	}

}
