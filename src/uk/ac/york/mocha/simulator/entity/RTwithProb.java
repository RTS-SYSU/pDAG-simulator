package uk.ac.york.mocha.simulator.entity;

public class RTwithProb {
    public double prob;
    public long length;

    public RTwithProb(double prob, long length) {
        this.prob = prob;
        this.length = length;
    }

    public double getProb() {
        return prob;
    }

    public long getLength() {
        return length;
    }

//    @Override
//    public int compareTo(RTwithProb other) {
//        // 从大到小排序
//        return Long.compare(other.length, this.length);
//    }
}
