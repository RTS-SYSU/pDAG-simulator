package uk.ac.york.mocha.simulator.simulator;

public class Event {

	public enum EventType {
		DAG_START, DAG_FINISH, NODE_START, NODE_FINISH // , MIGRATION, PREEMPTION, BLOCK, SPINNING, SUSPENSION
	};

	public EventType type;
	public int allocation = -1;
	public int Level2CacheGroup = -1;
	public long time = -1;

	public Event(EventType type, int allocation, int level2group, int time) {
		this.type = type;
		this.allocation = allocation;
		this.Level2CacheGroup = level2group;
		this.time = time;
	}
}
