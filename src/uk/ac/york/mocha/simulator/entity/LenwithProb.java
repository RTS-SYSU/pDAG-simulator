package uk.ac.york.mocha.simulator.entity;

public class LenwithProb implements Comparable<LenwithProb>{
    public double prob;
    public long length;

    public LenwithProb(double prob, long length) {
        this.prob = prob;
        this.length = length;
    }

    public double getProb() {
        return prob;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long l) {
        this.length = l;
    }

    @Override
    public int compareTo(LenwithProb other) {
        // 从大到小排序
        return Long.compare(other.length, this.length);
    }
}
