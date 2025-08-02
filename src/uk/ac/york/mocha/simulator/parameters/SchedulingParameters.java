package uk.ac.york.mocha.simulator.parameters;

import java.io.Serializable;

public class SchedulingParameters implements Serializable {

	private static final long serialVersionUID = -7156076584900026238L;

	private int partition;
	private int priority;

	private long period;
	private long deadline;
	private long WCET;
	private double util;

	/* DAG scheduling and ALLOCATION parameters */
	public SchedulingParameters(int p, long t, long d, long c, double util, int partition) {
		this.priority = p;
		this.period = t;
		this.WCET = c;
		this.deadline = d;
		this.partition = partition;
		this.util = util;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void setPeriod(long period) {
		this.period = period;
	
	}
	public long getPeriod() {
		return period;
	}

	public void setDeadline(long deadline) {
		this.deadline = deadline;
	}
	
	public long getDeadline() {
		return deadline;
	}

	public int getPartition() {
		return partition;
	}

	public void setPartition(int partition) {
		this.partition = partition;
	}

	public double getUtil() {
		return util;
	}

	public long getWCET() {
		return WCET;
	}
	
	public void setWCET(long WCET) {
		this.WCET = WCET;
	}
}
