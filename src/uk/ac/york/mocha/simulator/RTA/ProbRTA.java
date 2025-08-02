package uk.ac.york.mocha.simulator.RTA;

import org.python.indexer.ast.NPass;
import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.entity.LenwithProb;
import uk.ac.york.mocha.simulator.entity.Node;
import uk.ac.york.mocha.simulator.entity.CondPath;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class ProbRTA {
    public DirectedAcyclicGraph dag;

    // 所有的路径
    public List<List<Node>> allPaths;
    // 每条路径包含的cond nodes
    public List<List<Node>> condNodesForPath; 
    // 每条路径包含的regular nodes
    public List<List<Node>> regNodesForPath;
    // 每条路径non-critical部分的长度
    public List<Long> nonLen;

    public List<Node> regularNodes;
    public Map<Integer, Integer> regularIndex;
    public List<Node> condNodes;
    public Map<Integer, Integer> condIndex;

    public long thresholdLen;
    public List<Node> thresholdPath;

    public List<CondPath> filterPath;

    // 不能同时出现的路径集合数量/平均概率
    public List<List<Double>> notCoProb;


    public ProbRTA(DirectedAcyclicGraph dag){
        this.dag = dag;

        regularNodes = new ArrayList<>();
        condNodes = new ArrayList<>();
        regularIndex = new HashMap<>();
        condIndex = new HashMap<>();

        thresholdLen = 0;
        condNodesForPath = new ArrayList<>();
        regNodesForPath = new ArrayList<>();
        nonLen = new ArrayList<>();

        filterPath = new ArrayList<>();

        notCoProb = new ArrayList<>();
    }

    public void go() {
        splitNodes();
        allPaths = dag.allpaths;

        getPathCondRegNodes();
        findMaxMin();
        findAll();
        // 先从大到小排序
        Collections.sort(filterPath);
        extraProcess();
        extraProcess_fin();

        // calculateProb();
        cal_new();
        filterPath = filterPath.stream().filter(CondPath -> CondPath.getGlobalProb() != 0).collect(Collectors.toList());
        mergeCondPaths();

        // not coexist
        calNotCo();
//        if(SystemParameters.isRTA && !SystemParameters.EXPthree) transfer();

        // printInfo();

        // dbug();


        int debug = 0;
        // TODO: 把长度相同的概率合并一下
    }

    public void transfer(){
        for(CondPath i : filterPath) i.setLength((long) (i.getLength() + Math.ceil((dag.getSchedParameters().getWCET() - i.getLength()) / SystemParameters.coreNum)));
    }

    public void extraProcess() {
        // 消去cond-nodes及其branch完全相同，且总长度相同的
        int n = filterPath.size();
        int i = 0;
        while(i < n){
            CondPath curPath = filterPath.get(i);
            long curLen = curPath.length;
            if(curLen == -1){
                i ++;
                continue;
            }
            HashSet<Node> curSet = new HashSet<>(curPath.conds);
            int j = i + 1;
            while(j < n){
                CondPath pre = filterPath.get(j);
                if(pre.length == -1){
                    j ++;
                    continue;
                }
                if(curLen != pre.length){
                    break;
                }
                if(judgefast(pre, curPath, curSet)){
                    pre.setLength(-1L);
                }
                j ++;
            }
            i ++;
        }
        filterPath = filterPath.stream().filter(CondPath -> CondPath.length != -1).collect(Collectors.toList());
    }

    public void extraProcess_fin() {
        // 消去cond-nodes及其branch完全相同，且总长度相同的
        int n = filterPath.size();
        for(int i = 0; i < n; i ++){
            for(int j = 0; j < n; j ++){
                if(i == j) continue;
                CondPath pta = filterPath.get(i);
                CondPath ptb = filterPath.get(j);
                if(pta.length == -1 || ptb.length == -1) {
                    continue;
                }

                HashSet<Node> curSet = new HashSet<>(pta.conds);
                // 1\完全相同
                if(pta.conds.size() == ptb.conds.size()){
                    boolean flag = true;
                    for(int k = 0; k < ptb.conds.size(); k++){
                        Node cnd = ptb.conds.get(k);
                        if(!curSet.contains(cnd)){
                            // node不一样
                            flag = false;
                            break;
                        }
                        if(pta.Node2Branch.get(cnd) != ptb.Node2Branch.get(cnd)){
                            // 走了不一样的branch
                            flag = false;
                            break;
                        }
                    }

                    if(flag){
                        if(pta.length >= ptb.length) ptb.length = -1;
                        else pta.length = -1;
                        continue;
                    }
                }

                // 2\一个包含另一个
//                if(pta.conds.size() < ptb.conds.size()){
//                    continue;
//                }
                boolean flag = true;
                for(Node cnd : ptb.conds){
                    if(!curSet.contains(cnd) || !Objects.equals(pta.Node2Branch.get(cnd), ptb.Node2Branch.get(cnd))){
                        flag = false;
                        break;
                    }
                }
                if(!flag) continue;
                long bound = pta.length;
                HashSet<Node> bst = new HashSet<>(ptb.conds);
                for(Node cnd : pta.conds){
                    if(!bst.contains(cnd)){// distinct branches
                        bound = bound - cnd.correspondLen.get(pta.Node2Branch.get(cnd)) + Collections.min(cnd.correspondLen);
                    }
                }
                if(bound >= ptb.length) ptb.length = -1;
            }
        }

        filterPath = filterPath.stream().filter(CondPath -> CondPath.length != -1).collect(Collectors.toList());
    }

    public void calNotCo() {
        for(int i = 0; i < filterPath.size(); i ++){
            notCoProb.add(new ArrayList<>());
            processCoexist(i);
        }
    }

    public void processCoexist(int idx){
        CondPath curPath = filterPath.get(idx);
        HashSet<Node> curSet = new HashSet<>(curPath.conds);
        for(int i = 0; i < idx; i ++){
            CondPath prePath = filterPath.get(i);
            boolean judge = twoPaths(prePath, curPath, curSet);
            if(!judge){
                // 无法共存
                notCoProb.get(idx).add(1 - pathExist(prePath.conds, prePath.idx));
            }
        }
    }

    public Boolean twoPaths(CondPath prePath, CondPath curPath, HashSet<Node> curSet){
        double res = 1.0;
        for (Node preNode : prePath.conds){
            if(curSet.contains(preNode)){
                // branch是否相同 ? 公共节点continue : 冲突节点return 1
                if(Objects.equals(prePath.Node2Branch.get(preNode), curPath.Node2Branch.get(preNode))){
                    continue;
                }
                else{
                    return false;
                }
            }
        }

        // int debug = 0;
        return true;
    }

    public void mergeCondPaths() {
        if (filterPath.isEmpty()) {
            return;
        }

        List<CondPath> mergedPaths = new ArrayList<>();
        CondPath current = filterPath.get(0);

        for (int i = 1; i < filterPath.size(); i++) {
            CondPath next = filterPath.get(i);
            if (current.length == next.length) {
                // 如果长度相同，则合并概率
                current.setGlobalProb(current.getGlobalProb() + next.getGlobalProb());
            } else {
                // 如果长度不同，添加当前路径到结果列表，并移动到下一个
                mergedPaths.add(current);
                current = next;
            }
        }

        // 添加最后一个元素
        mergedPaths.add(current);

        filterPath = mergedPaths;
    }

    public List<CondPath> getFilterPath() {
        return filterPath;
    }

    public void dbug(){
        double sum = 0;
        for (CondPath pt : filterPath) sum += pt.getGlobalProb();
        System.out.println(sum);
    }

    public void printInfo(){
        File file = new File("testOut/test.txt");
        /*if(!file.exists()){
            boolean success = file.mkdirs();
        }*/
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (CondPath cur : filterPath){
                // TODO 再做一个视图的样例
                writer.write(String.valueOf("length = " + cur.length + ", prob = " + cur.globalProb));
                writer.newLine();

                // print the path
                writer.write("The path node: [ ");
                int idx = 0;
                for(Node tmp : cur.completePath){
                    if(!tmp.isCond){
                        writer.write(String.valueOf(tmp.getId() + ", "));
                    }
                    else{
                        DirectedAcyclicGraph subDAG = tmp.choosePathwithIndex(cur.Node2Branch.get(tmp));
                        for(Node subNode : subDAG.longestPath){
                            writer.write(String.valueOf(tmp.getId() + "." + cur.Node2Branch.get(tmp) + "." + subNode.getId() + ", "));
                        }
                    }
                }
                writer.write("]");
                writer.newLine();
                writer.newLine();
                // System.out.println("length = " + cur.length + ", prob = " + cur.globalProb);
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void cal_new(){
        int n = filterPath.size();
        double pre_prob = 0.0;
        for(int i = 0; i < n; i ++){
            CondPath curPath = filterPath.get(i);
            // 完成这一步之后，只要加上重复减掉的部分即可
            double probCur = pathExist(curPath.conds, curPath.idx) - pre_prob;

            for (int j = i - 1; j >= 0; j --){
                CondPath prePath = filterPath.get(j);
                if(Math.abs(prePath.globalProb) >1e-14){
                    if(prePath.length != curPath.length) {probCur += existAndNotExist(prePath, curPath);}
                    else {
                        if(judge(prePath, curPath) < 2){
                            probCur += existAndNotExist(prePath, curPath);
                        }
                        else probCur = 0.0;
                    }
                }
                // probCur += existAndNotExist(prePath, curPath);
            }

//            int debug = 0;
            // rule 1
            if (probCur < 0){
                curPath.setGlobalProb(0.0);
            }
            else{
                // rule 2
                double real = Math.min(probCur, 1.0 - pre_prob);
//                if(i == 1 && real != probCur){
//                    int debug = 0;
//                }
                curPath.setGlobalProb(real);
                pre_prob += real;
                if(pre_prob == 1) break;
            }
        }
    }

//                if (i == n - 1){
//                    // 删掉threshold
//                    filterPath.remove(n - 1);
//                    // 把倒数第二个path
//                    CondPath last = filterPath.get(n - 2);
//                    double newProb = 1 - pre_prob + last.getGlobalProb();
//                    if(newProb < 0){
//                        System.out.println("The last path probability is negative!!!");
//                        System.exit(0);
//                    }
//                    last.setGlobalProb(newProb);
//                    break;
//                }
//                else {
//                    curPath.setGlobalProb(0.0);
////                    System.out.println("The path probability is negative!!!");
////                    System.exit(0);
//                }

    /*
    * 为每条路径计算发生概率
    * 注意，输入是conditional nodes集合和idx
    * */
    public double pathExist(List<Node> conds, List<Integer> idx){
        double prob = 1;
        for (int i = 0; i < conds.size(); i ++){
            Node cur = conds.get(i);
            int branch = idx.get(i);
            prob *= cur.idx2prob.getOrDefault(branch, 0.0);
        }
        return prob;
    }

    /*
    * j(pre)路径发生和i(cur)路径不发生的概率 = j发生 * j发生前提下i不发生
    * 1、找到j发生的概率p1
    * 2、找到i的独立集合V2 --> p2 (ps: 如果存在冲突节点则p2为1
    * 3、prob = p1 * (1 - p2)
    * */
    public double existAndNotExist(CondPath prePath, CondPath curPath){
        // 1、找到j发生的概率p1
        double prob_1 = pathExist(prePath.conds, prePath.idx);

        // 2、找到i的独立集合V2 --> p2
        double prob_2 = 1.0;
        HashSet<Node> preSet = new HashSet<Node>(prePath.conds);
        for (Node curNode : curPath.conds){
            if(preSet.contains(curNode)){
                // branch是否相同 ? 公共节点continue : 冲突节点return 1
                if(Objects.equals(prePath.Node2Branch.get(curNode), curPath.Node2Branch.get(curNode))){
                    continue;
                }
                else{
                    prob_2 = 0.0;
                    break;
                }
            }
            else{
                // 独立集合
                prob_2 *= curNode.idx2prob.get(curPath.Node2Branch.get(curNode));
            }
        }

        // int debug = 0;
        return prob_1 * (1.0 - prob_2);
    }

//    public void calculateProb(){
//        // 先从大到小排序
//        Collections.sort(filterPath);
//        int n = filterPath.size();
//        for(int i = 0; i < n; i ++){
//            CondPath curPath = filterPath.get(i);
//            HashSet<Node> curSet = new HashSet<Node>(curPath.conds);
//            // from other and self
//            double prob_others = 1, prob_self = 1;
//
//            // 自身路径发生的概率
//            for(int index = 0; index < curPath.conds.size(); index ++){
//                Node cond_self = curPath.conds.get(index);
//                prob_self *= cond_self.idx2prob.get(curPath.idx.get(index));
//            }
//
//            // 比当前路径长的路径不发生的概率
//            for (int j = 0; j < i; j ++){
//                CondPath formerPath = filterPath.get(j);
//                List<Node> common = judge(formerPath, curPath, curSet);
//                /*
//                 * 1\判读是否能共存: 所有 cond nodes either 同branch or 不是公共
//                 * 2\能共存，则根据公式去计算新的概率*/
//                if(common != null){
//                    // 能共存
//                    double prob_common = 1;
//                    for (Node ele : common) prob_common *= ele.idx2prob.get(curPath.Node2Branch.get(ele));
//                    prob_others -= formerPath.getGlobalProb() / prob_common;
//                }
//            }
//            curPath.setGlobalProb(prob_self * prob_others);
//        }
//    }
//
    /*
    * 判断两个路径能否共存*/
    public int judge(CondPath pre, CondPath cur){
        // 判读是否能共存: 所有 cond nodes either 同branch or 不是公共
        // 2完全相同 1 部分相同 0 不可共存
        HashSet<Node> curSet = new HashSet<>(cur.conds);
        int res = 2;
        for (Node n : pre.conds){
            if (curSet.contains(n)){
                // 是公共，则判断是否同branch
                if(Objects.equals(pre.Node2Branch.get(n), cur.Node2Branch.get(n))){
                    continue;
                }
                else return 0;
            }
            else res = 1;
        }
        return res;
    }

    public boolean judgefast(CondPath pre, CondPath cur, HashSet<Node> curSet){
        // 判读是否能共存
        if(pre.conds.size() != cur.conds.size()) return false;
        for (Node n : pre.conds){
            if (curSet.contains(n)){
                // 是公共，则判断是否同branch
                if(Objects.equals(pre.Node2Branch.get(n), cur.Node2Branch.get(n))){
                    continue;
                }
                else return false;
            }
            else return false;
        }
        return true;
    }

    /*
     * 找threshold长度*/
    public void findMaxMin(){
        int n = allPaths.size();
        for(int i = 0; i < n; i ++){
            long curSum = nonLen.get(i);
            for(Node cur : condNodesForPath.get(i)) curSum += Collections.min(cur.correspondLen);
            if(curSum > thresholdLen){
                thresholdLen = curSum;
                thresholdPath = allPaths.get(i);
            }
        }
    }

    /*
    * 找出所有可能的路径*/
    public void findAll() {
        int n = allPaths.size();
        for(int i = 0; i < n; i ++){
            List<Integer> branches = new ArrayList<>();
            findPossiblePath(condNodesForPath.get(i), 0, nonLen.get(i), branches, i);
        }
    }

    // 回溯遍历路径
    public void findPossiblePath(List<Node> conds, int idx, long curSum, List<Integer> branches, int pathId){
        if(idx == conds.size()){
            if(curSum >= thresholdLen){
                filterPath.add(new CondPath(curSum, conds, branches, regNodesForPath.get(pathId), allPaths.get(pathId)));
            }
            return;
        }

        Node curNode = conds.get(idx);
        int n = curNode.nCond;
        for(int i = 0; i < n; i ++){
            branches.add(i);
            findPossiblePath(conds, idx + 1, curSum + curNode.correspondLen.get(i), branches, pathId);
            branches.remove(branches.size() - 1);
        }
    }

    // 对节点进行划分
    public void splitNodes(){
        for(Node n : dag.getFlatNodes()){
            int key = getKey(n);
            if(n.isCond) condIndex.put(key, 1);
            else regularIndex.put(key, 1);
        }
    }

    /*
    * 为每条路径找到cond / regular集合，方便后续处理*/
    public void getPathCondRegNodes(){
        for (List<Node> p : allPaths){
            List<Node> cNode = new ArrayList<>();
            List<Node> rNode = new ArrayList<>();
            long nonSum = 0;
            for(Node n : p){
                if(condIndex.containsKey(getKey(n))) cNode.add(n);
                else{
                    nonSum += n.WCET;
                    rNode.add(n);
                }
            }
            condNodesForPath.add(cNode);
            regNodesForPath.add(rNode);
            nonLen.add(nonSum);
        }
    }

    // 计算每个节点的key
    public int getKey(Node n){
        return (n.getDagInstNo() + 1) * (n.getId() + 1);
    }

    /*
    * 根据节点类型划分集合
    * */
    public void splitNodesSet(){
        for(Node n : dag.getFlatNodes()){
            if(n.isCond) condNodes.add(n);
            else regularNodes.add(n);
        }
    }
}
