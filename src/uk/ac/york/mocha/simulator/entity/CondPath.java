package uk.ac.york.mocha.simulator.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CondPath implements Comparable<CondPath>{
    public long length;// total length of the path
    public List<Node> conds;// the cond nodes it contains
    public List<Node> regulars;// the regular nodes it contains
    public List<Node> completePath;
    public List<Integer> idx;// the branch each node choose
    public HashMap<Node, Integer> Node2Branch;
    public double globalProb;// probability of the path

    public CondPath(long length, List<Node> conds, List<Integer> idx, List<Node> regs, List<Node> completePath) {
        this.length = length;
        this.conds = conds;
        this.idx = idx.stream()
                .map(Integer::new)
                .collect(Collectors.toList());
        this.globalProb = 0;
        this.regulars = regs;
        this.completePath = completePath;
        this.Node2Branch = new HashMap<>();

        for (int i = 0; i < conds.size(); i ++){
            Node2Branch.put(conds.get(i), idx.get(i));
        }
    }

    public List<Node> getRegulars() {
        return regulars;
    }

    public void setRegulars(List<Node> regulars) {
        this.regulars = regulars;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public List<Node> getConds() {
        return conds;
    }

    public void setConds(List<Node> conds) {
        this.conds = conds;
    }

    public List<Integer> getIdx() {
        return idx;
    }

    public void setIdx(List<Integer> idx) {
        this.idx = idx;
    }

    public double getGlobalProb() {
        return globalProb;
    }

    public void setGlobalProb(double globalProb) {
        this.globalProb = globalProb;
    }

    @Override
    public int compareTo(CondPath other) {
        // 从大到小排序
        return Long.compare(other.length, this.length);
    }
}
