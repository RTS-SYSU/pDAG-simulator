package uk.ac.york.mocha.simulator.entity;

public class InfoCap {
	public long best_core = -1;
	public long best_response_time = Long.MAX_VALUE;
	public long best_inter = Long.MAX_VALUE;
	public long best_intra = Long.MAX_VALUE;

	public InfoCap(long best_core, long best_response_time, long best_inter, long best_intra) {
		this.best_core = best_core;
		this.best_response_time = best_response_time;
		this.best_inter = best_inter;
		this.best_intra = best_intra;
	}
}