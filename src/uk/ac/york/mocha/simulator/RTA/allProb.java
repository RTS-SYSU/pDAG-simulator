package uk.ac.york.mocha.simulator.RTA;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.entity.LenwithProb;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;

import java.util.*;
import java.util.stream.Collectors;

public class allProb {
    public DirectedAcyclicGraph dag;

    public List<Node> condNodes;
    public Map<Integer, Integer> condIndex;

    public List<LenwithProb> Paths;

    public Map<Long, Double> allLen2Prob;


    public allProb(DirectedAcyclicGraph dag){
        this.dag = dag;

        condNodes = new ArrayList<>();
        condIndex = new HashMap<>();

        Paths = new ArrayList<>();

        allLen2Prob = new HashMap<>();
    }

    public void go() {
        // main process
        splitNodes();
        findPaths(0, 1.0);
        processPaths();
//        if(SystemParameters.isRTA && !SystemParameters.EXPthree) transfer();


        // dbug();
        int debug = 0;
        // TODO: 把长度相同的概率合并一下
    }

    public void transfer(){
        for(LenwithProb i : Paths) i.setLength((long) (i.getLength() + Math.ceil((dag.getSchedParameters().getWCET() - i.getLength()) / SystemParameters.coreNum)));
    }

    public List<LenwithProb> getPaths() {
        return Paths;
    }

    public void processPaths() {
        allLen2Prob.forEach((key, value) -> {
            Paths.add(new LenwithProb(value, key));
        });
        Collections.sort(Paths);
    }

    public void findPaths(int curIndex, double prob){
        // 1、拿到conditional nodes集合
        // 2、依次选取一个branch（回溯），记录概率，然后计算lengths
        // 3、加入集合维护
        if(curIndex == condNodes.size()){
            dag.findPath(true);
            long length = dag.bestDistance;
            allLen2Prob.put(length, allLen2Prob.getOrDefault(length, 0.0) + prob);
            return;
        }

        Node curNode = condNodes.get(curIndex);
        for (int i = 0; i < curNode.nCond; i ++){
            double branchProb = curNode.idx2prob.getOrDefault(i, 1.0);
            long branchLen = curNode.idx2len.getOrDefault(i, 0L);

            prob *= branchProb;
            curNode.setWCET(branchLen);
            findPaths(curIndex + 1, prob);
            prob /= branchProb;
        }
    }

    public void splitNodes(){
        for(Node n : dag.getFlatNodes()){
            int key = getKey(n);
            if(n.isCond){
                condIndex.put(key, 1);
                condNodes.add(n);
            }
        }
    }

    public int getKey(Node n){
        return (n.getDagInstNo() + 1) * (n.getId() + 1);
    }
}
