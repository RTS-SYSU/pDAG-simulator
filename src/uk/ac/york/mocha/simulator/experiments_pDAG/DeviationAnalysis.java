package uk.ac.york.mocha.simulator.experiments_pDAG;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.RTA.ProbRTA;
import uk.ac.york.mocha.simulator.RTA.allProb;
import uk.ac.york.mocha.simulator.allocation.TPDSHe;
import uk.ac.york.mocha.simulator.entity.*;
import uk.ac.york.mocha.simulator.generator.CacheHierarchy;
import uk.ac.york.mocha.simulator.generator.SystemGenerator;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.ExpName;
import uk.ac.york.mocha.simulator.simulator.Utils;
import uk.ac.york.mocha.simulator.utils.StaticFieldBackup;

import java.io.*;

public class DeviationAnalysis {

    static DecimalFormat df = new DecimalFormat("#.###");

    static int cores = 4;
    static int nos = 500;// 500 for time; 1000 for default
    static int intanceNum = 1;// 100


    static int startParal = 3;
    static int incrementParal = 1;
    static int endParal = 9;
    static int startrt = 20;
    static int incrementrt = 10;
    static int endrt = 80;
    static int currt = 40;

    static boolean print = false;
    static boolean RTA = true;
    static int startCond = 2;
    static int incrementCond = 1;
    static int endCond = 10;
    static int startAcp = 6;
    static int incrementAcp = 1;
    static int endAcp = 6;

    public static List<List<Double>> notPath = new ArrayList<>();

    public static List<List<Double>> timeOur = new ArrayList<>();
    public static List<List<Double>> timeAll = new ArrayList<>();
    public static List<List<Double>> timeHe = new ArrayList<>();
    public static List<List<Double>> timeZhao = new ArrayList<>();
    public static long GraRTA = 0;
    public static List<Long> OurRTA = new ArrayList<>();
    public static List<Long> ChenRTA = new ArrayList<>();
    public static List<Integer> timeIdx = new ArrayList<>();
    public static TPDSHe He19;
    public static String param;

    public static String resRootPath = "result/deviation_analysis";

    public static String secondResRootPath = "result/computation_cost_analysis";

    public static void main(String args[]) {
        run();
    }

    public static void run() {
        System.out.println("Start running deviation analysis");
        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }
        oneTaskWithFaults();

        backup.restore();
    }

    public static void runPsr(double startPsr, double endPsr){
        System.out.println("Start running deviation analysis -- varied psr");

        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }

        int hyperPeriodNum = -1;
        int seed = 1000;


        startrt = (int) (startPsr * 100);
        endrt = (int) (endPsr *100);

        param = "ratio";

        for (int i = startrt; i <= endrt; i = i + incrementrt) {
            currt = i;
            timeIdx.add(i);
            timeOur.add(new ArrayList<>());
            timeAll.add(new ArrayList<>());
            timeHe.add(new ArrayList<>());
            timeZhao.add(new ArrayList<>());
            Error_Avg_List.add(new ArrayList<>());
            System.out.println();
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_psr);
            Onewrite(i, param);
            OneWriteTime(i, param);
        }
        Multiwrite(param);
        backup.restore();
    }

    public static void runPara(int startPara, int endPara){
        System.out.println("Start running deviation analysis -- varied para");

        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }

        int hyperPeriodNum = -1;
        int seed = 1000;
        startParal = startPara;
        endParal = endPara;

        param= "para";

        for (int i = startParal; i <= endParal; i = i + incrementParal) {
            SystemParameters.maxParal = i;
            timeIdx.add(i);
            timeOur.add(new ArrayList<>());
            timeAll.add(new ArrayList<>());
            timeHe.add(new ArrayList<>());
            timeZhao.add(new ArrayList<>());
            Error_Avg_List.add(new ArrayList<>());
            System.out.println();
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_para);
            Onewrite(i, param);
            OneWriteTime(i, param);
        }
        Multiwrite(param);
        backup.restore();
    }

    public static void runCond(int custStartCond, int custEndCond){
        System.out.println("Start running deviation analysis -- varied cond");
        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }

        int hyperPeriodNum = -1;
        int seed = 1000;

        SystemParameters.utilPerTask = Double.parseDouble(df.format((double) 20 / (double) 10));
        startCond = custStartCond;
        endCond = custEndCond;

        param = "cond";

        for (int numberCond = startCond; numberCond <= endCond; numberCond += incrementCond) {
            SystemParameters.MAX_CondNum = numberCond;
            timeIdx.add(numberCond);
            timeOur.add(new ArrayList<>());
            timeAll.add(new ArrayList<>());
            timeHe.add(new ArrayList<>());
            timeZhao.add(new ArrayList<>());
            Error_Avg_List.add(new ArrayList<>());
            System.out.println();
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_cond);
            Onewrite(numberCond, param);
            OneWriteTime(numberCond, param);
            Multiwrite(param);
        }

        backup.restore();
    }

    public static void reset() {
        cores = 4;
        nos = 500;// 500 for time; 1000 for default
        intanceNum = 1;// 100
        startParal = 3;
        incrementParal = 1;
        endParal = 10;
        startrt = 10;
        incrementrt = 10;
        endrt = 80;
        currt = 40;

        print = false;
        RTA = true;
        startCond = 2;
        incrementCond = 1;
        endCond = 10;
        startAcp = 6;
        incrementAcp = 1;
        endAcp = 6;

        notPath = new ArrayList<>();
        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }

        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }
        timeOur = new ArrayList<>();
        timeAll = new ArrayList<>();
        timeHe = new ArrayList<>();
        timeZhao = new ArrayList<>();
        GraRTA = 0;
        OurRTA = new ArrayList<>();
        ChenRTA = new ArrayList<>();
        timeIdx = new ArrayList<>();
    }

    public static void oneTaskWithFaults() {
        int hyperPeriodNum = -1;
        int seed = 1000;
        String[] params = {"ratio", "para", "cond"};
        for (int k = 0; k < params.length; k++) {
            StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

            reset();
            param = params[k];
            switch (param) {
                case "ratio":
                    for (int i = startrt; i <= endrt; i = i + incrementrt) {
                        currt = i;
                        timeIdx.add(i);
                        timeOur.add(new ArrayList<>());
                        timeAll.add(new ArrayList<>());
                        timeHe.add(new ArrayList<>());
                        timeZhao.add(new ArrayList<>());
                        Error_Avg_List.add(new ArrayList<>());
                        System.out.println();
                        RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_psr);
                        Onewrite(i, param);
                        OneWriteTime(i, param);
                    }
                    Multiwrite(param);
                    break;
                case "para":
                    for (int i = startParal; i <= endParal; i = i + incrementParal) {
                        SystemParameters.maxParal = i;
                        timeIdx.add(i);
                        timeOur.add(new ArrayList<>());
                        timeAll.add(new ArrayList<>());
                        timeHe.add(new ArrayList<>());
                        timeZhao.add(new ArrayList<>());
                        Error_Avg_List.add(new ArrayList<>());
                        System.out.println();
                        RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_para);
                        Onewrite(i, param);
                        OneWriteTime(i, param);
                    }
                    Multiwrite(param);
                    break;
                default:
                    SystemParameters.utilPerTask = Double.parseDouble(df.format((double) 20 / (double) 10));
                    for (int numberCond = startCond; numberCond <= endCond; numberCond += incrementCond) {
                        SystemParameters.MAX_CondNum = numberCond;
                        timeIdx.add(numberCond);
                        timeOur.add(new ArrayList<>());
                        timeAll.add(new ArrayList<>());
                        timeHe.add(new ArrayList<>());
                        timeZhao.add(new ArrayList<>());
                        Error_Avg_List.add(new ArrayList<>());
                        System.out.println();
                        RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_cond);
                        Onewrite(numberCond, param);
                        OneWriteTime(numberCond, param);
                        Multiwrite(param);
                    }
            }

            backup.restore();
        }
    }

    public static void Onewrite(int param, String type) {
        Path path = Paths.get(resRootPath, "avg_" + type + "_" + String.valueOf(param) + ".txt");
        File file = new File(path.toString());
        BufferedWriter writer;
        List<Double> out = Error_Avg_List.get(Error_Avg_List.size() - 1);
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (Double ele : out) {
                writer.write(String.format("%.4f", ele) + ", ");
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void OneWriteTime(int param, String type) {
        Path path = Paths.get(secondResRootPath, "time_all_" + type + "_" + String.valueOf(param) + ".txt");
        File file = new File(path.toString());
        BufferedWriter writer;
        List<Double> res = timeAll.get(timeAll.size() - 1);
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("Chen:");
            writer.newLine();
            for (Double ele : res) {
                writer.write(String.valueOf(ele) + ", ");
            }
            writer.newLine();

            res = timeOur.get(timeOur.size() - 1);
            writer.write("Our:");
            writer.newLine();
            for (Double ele : res) {
                writer.write(String.valueOf(ele) + ", ");
            }
            writer.newLine();

            res = timeHe.get(timeHe.size() - 1);
            writer.write("He:");
            writer.newLine();
            for (Double ele : res) {
                writer.write(String.valueOf(ele) + ", ");
            }
            writer.newLine();

            res = timeZhao.get(timeZhao.size() - 1);
            writer.write("Zhao:");
            writer.newLine();
            for (Double ele : res) {
                writer.write(String.valueOf(ele) + ", ");
            }

            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void Multiwrite(String type) {
        Path path = Paths.get(resRootPath, "avg_Err_" + type + ".txt");
        File file = new File(path.toString());
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (List<Double> l : Error_Avg_List) {
                for (Double ele : l) writer.write(String.format("%.4f", ele) + ", ");
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        file = new File(Paths.get(secondResRootPath, "time_Chen_" + type + ".txt").toString());
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (List<Double> l : timeAll) {
                for (Double ele : l) {
                    writer.write(String.valueOf(ele) + ", ");
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        file = new File(Paths.get(secondResRootPath, "time_Our_" + type + ".txt").toString());
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (List<Double> l : timeOur) {
                for (Double ele : l) {
                    writer.write(String.valueOf(ele) + ", ");
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        file = new File(Paths.get(secondResRootPath, "time_He_" + type + ".txt").toString());
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (List<Double> l : timeHe) {
                for (Double ele : l) {
                    writer.write(String.valueOf(ele) + ", ");
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        file = new File(Paths.get(secondResRootPath, "time_Zhao_" + type + ".txt").toString());
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (List<Double> l : timeZhao) {
                for (Double ele : l) {
                    writer.write(String.valueOf(ele) + ", ");
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void RunOneGroup_new(int taskNum, int intanceNum, int hyperperiodNum, boolean takeAllUtil,
                                       List<List<Double>> util, int taskSeed, int tableSeed, List<List<Long>> periods, int NoS, boolean randomC,
                                       ExpName name) {

        int[] instanceNo = new int[taskNum];

        if (periods != null && hyperperiodNum > 0) {
            long totalHP = Utils.getHyperPeriod(periods.get(0)) * hyperperiodNum;

            for (int i = 0; i < periods.size(); i++) {
                int insNo = (int) (totalHP / periods.get(0).get(i));
                instanceNo[i] = insNo > intanceNum ? insNo : intanceNum;
            }
        } else if (intanceNum > 0) {
            for (int i = 0; i < instanceNo.length; i++)
                instanceNo[i] = intanceNum;
        } else {
//            System.out.println("Cannot get same instances number for randomly generated periods.");
        }

        long startTime = System.currentTimeMillis();
        int barLength = 40;

        int record = 0, i = 0;
        // exp one
        while (true) {
//            System.out.println(
//                    "Current psr: " + currt + " --- Current system number: " + (i + 1)
//                            + " --- Current collected tasks: " + record + " --- Current Parallelism: " + SystemParameters.maxParal
//                            + " --- Current conditional number: " + SystemParameters.MAX_CondNum);

            SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
            Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
                    null, false);

            if (currt != 0) {
                // rearrange workload
                DirectedAcyclicGraph dg = sys.getFirst().get(0);
                long W = dg.getSchedParameters().getWCET();
                List<Node> cs = new ArrayList<>();
                long csw = 0L;
                for (Node nd : dg.getFlatNodes()) {
                    if (nd.isCond) {
                        cs.add(nd);
                        csw += nd.getWCET();
                    }
                }
                double scale = (double) ((W - csw) * currt * 0.01) / ((double) csw * (1 - currt * 0.01));
                long cre = 0L;
                for (Node nd : cs) {
                    long nwcet = (long) (nd.getWCET() * scale);
                    cre += nwcet - nd.getWCET();
                    nd.setWCET(nwcet);
                    nd.idx2len.forEach((key, value) -> nd.idx2len.put(key, (long) (value * scale)));
                    nd.correspondLen.replaceAll(value -> (long) (value * scale));
                }
                dg.getSchedParameters().setWCET(W + cre);

                csw = 0L;
                for (Node nd : dg.getFlatNodes()) {
                    if (nd.isCond) {
                        cs.add(nd);
                        csw += nd.getWCET();
                    }
                }
                W = dg.getSchedParameters().getWCET();
                dg.findPath(true);
            }

            if (RTA) {
                ProbRTA RTAtool = new ProbRTA(sys.getFirst().get(0));
                long stime = System.nanoTime();
                RTAtool.go();
                if (RTAtool.getFilterPath().size() < SystemParameters.FigLimit) {
                    taskSeed++;
                    i++;
                    // task的path数量不达标
                    continue;
                }
                // printDAGinfo(sys.getFirst().get(0));
                List<CondPath> our = RTAtool.getFilterPath();
                timeOur.get(timeOur.size() - 1).add((System.nanoTime() - stime) / 1_000_000.0);
//				for(int pathidx = 0; pathidx < SystemParameters.FigLimit; pathidx ++){
//					CondPath p = our.get(pathidx);
//					notPath.get(pathidx).add(1.0 - RTAtool.pathExist(p.conds, p.idx));
//				}

                if (SystemParameters.MAX_CondNum <= 7) {
                    allProb Alltool = new allProb(sys.getFirst().get(0));
                    stime = System.nanoTime();
                    try {
                        Alltool.go();
                    } catch (OutOfMemoryError e) {
                        // 捕获到堆空间溢出，跳过当前任务
//                        System.err.println("Task " + i + " caused OutOfMemoryError, skipping...");
                        Onewrite(SystemParameters.MAX_Cond, "cond");
                        OneWriteTime(SystemParameters.MAX_Cond, "cond");
                        taskSeed++;
                        i++;
                        continue;
                        // 这里可以进行其他的清理或日志记录操作
                    } catch (Exception e) {
                        // 处理其他可能的异常
//                        System.err.println("Task " + i + " caused an exception: " + e.getMessage());
                        Onewrite(SystemParameters.MAX_Cond, "cond");
                        OneWriteTime(SystemParameters.MAX_Cond, "cond");
                        taskSeed++;
                        i++;
                        continue;
                    }
                    List<LenwithProb> all = Alltool.getPaths();
//					DirectedAcyclicGraph dg = sys.getFirst().get(0);
//					List<Node> cs = new ArrayList<>();
//					long csw = 0L;
//					for(Node nd : dg.getFlatNodes()){
//						if(nd.isCond){
//							cs.add(nd);
//							csw += nd.getWCET();
//						}
//					}
//					long W = dg.getSchedParameters().getWCET();
//					double rt = (double) csw / (double) W;
                    timeAll.get(timeAll.size() - 1).add((System.nanoTime() - stime) / 1_000_000.0);

                    double totalProb = 0.0;
                    for (CondPath condPath : our) {
                        totalProb += condPath.globalProb;
                    }
                    if (Math.abs(totalProb - 1.0) > 1e-5) {
                        System.out.println("RTA error!!");
                        System.exit(-1);
                    }
                    compute_area_rto(our, all);
                }

                if (SystemParameters.CompareSingle) {
                    stime = System.nanoTime();
                    long empty = He19.getResponseTime(sys.getFirst(), SystemParameters.coreNum).get(0).best_response_time;
                    timeHe.get(timeHe.size() - 1).add((System.nanoTime() - stime) / 1_000_000.0);

                    stime = System.nanoTime();
                    empty = DAGtoPython.pharseDAGForPython(sys.getFirst().get(0), SystemParameters.coreNum).getFirst();
                    timeZhao.get(timeZhao.size() - 1).add((System.nanoTime() - stime) / 1_000_000.0);
                }

                record++;
            }

            // visual progress bar
            long elapsed = System.currentTimeMillis() - startTime;
            double percent = (double)record / SystemParameters.TargetNum;
            int completedBars = (int) (percent * barLength);
            int remainingBars = barLength-completedBars;

            String progressBar = "";
            if (name == ExpName.multi_pDAG_psr){
                progressBar += ("Fig. 5a : psr = " + currt + " : ");
            }else if (name == ExpName.multi_pDAG_para){
                progressBar += ("Fig. 5b : parallelism = " + SystemParameters.maxParal + " : ");
            }else if(name == ExpName.multi_pDAG_cond){
                progressBar += ("Figs. 5c & 6 : condNum = " + SystemParameters.MAX_CondNum + " : ");
            }

            progressBar += "[" + "#".repeat(completedBars) + "-".repeat(remainingBars)+"]";
            double elapsedSec = elapsed / 1000.0;
            double etaSec = (record==0) ? 0 : elapsedSec / record * (SystemParameters.TargetNum - record);
            System.out.printf("\r%s %.1f%% (%d / %d) | Elapsed: %.1fs | ETA: %.1fs",
                    progressBar, percent * 100, record, SystemParameters.TargetNum, elapsedSec, etaSec);

            if (record >= SystemParameters.TargetNum) {
                // enough collected tasks
                break;
            }

            taskSeed++;
            i++;
        }
    }


    public static void compute_area_rto(List<CondPath> our, List<LenwithProb> all) {
        double x = calculateAreaUnderCDF_v1(our);
        double y = calculateAreaUnderCDF_v2(all);
        double rt = x / y;
        Error_Avg_List.get(Error_Avg_List.size() - 1).add(rt);
    }

    public static double calculateAreaUnderCDF_v1(List<CondPath> our) {
        double[] values = new double[our.size()];
        double[] probabilities = new double[our.size()];

        for (int i = 0; i < our.size(); i++) {
            values[i] = our.get(our.size() - 1 - i).getLength();
            probabilities[i] = our.get(our.size() - 1 - i).globalProb;
        }
        // 计算CDF
        double[] cdf = new double[probabilities.length];
        cdf[0] = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            cdf[i] = cdf[i - 1] + probabilities[i];
        }

        // 使用梯形法计算CDF和x轴围成的面积
        double area = 0.0;
        for (int i = 1; i < values.length; i++) {
            double width = values[i] - values[i - 1];
            double height = (cdf[i] + cdf[i - 1]) / 2.0;
            area += width * height;
        }
        area += 1;

        return area;
    }

    public static double calculateAreaUnderCDF_v2(List<LenwithProb> all) {
        double[] values = new double[all.size()];
        double[] probabilities = new double[all.size()];

        for (int i = 0; i < all.size(); i++) {
            values[i] = all.get(all.size() - 1 - i).getLength();
            probabilities[i] = all.get(all.size() - 1 - i).getProb();
        }
        // 计算CDF
        double[] cdf = new double[probabilities.length];
        cdf[0] = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            cdf[i] = cdf[i - 1] + probabilities[i];
        }

        // 使用梯形法计算CDF和x轴围成的面积
        double area = 0.0;
        for (int i = 1; i < values.length; i++) {
            double width = values[i] - values[i - 1];
            double height = (cdf[i] + cdf[i - 1]) / 2.0;
            area += width * height;
        }
        area += 1;

        return area;
    }


    public static double compute_overlap(List<CondPath> our, List<LenwithProb> all) {

        // 创建 x 和 y 值
        double[] x1 = new double[our.size() + 2];
        double[] y1 = new double[our.size() + 2];
        double[] x2 = new double[all.size() + 2];
        double[] y2 = new double[all.size() + 2];

        // 初始化 x1 和 y1
        for (int i = 0; i < our.size(); i++) {
            x1[i + 2] = our.get(our.size() - 1 - i).getLength();
            y1[i + 1] = our.get(our.size() - 1 - i).globalProb;
        }
        y1[0] = 0;
        y1[y1.length - 1] = 0;

        // 初始化 x2 和 y2
        for (int i = 0; i < all.size(); i++) {
            x2[i + 2] = all.get(all.size() - 1 - i).getLength();
            y2[i + 1] = all.get(all.size() - 1 - i).getProb();
        }
        y2[0] = 0;
        y2[y2.length - 1] = 0;

        double[] t1 = new double[our.size()];
        double[] p1 = new double[our.size()];
        double[] t2 = new double[all.size()];
        double[] p2 = new double[all.size()];
        for (int i = 0; i < our.size(); i++) {
            t1[i] = our.get(our.size() - 1 - i).getLength();
            p1[i] = our.get(our.size() - 1 - i).globalProb;
        }
        // 初始化 x2 和 y2
        for (int i = 0; i < all.size(); i++) {
            t2[i] = all.get(all.size() - 1 - i).getLength();
            p2[i] = all.get(all.size() - 1 - i).getProb();
        }

        // 计算重叠面积
        double totalOverlapArea = 0;

        for (int i = 0; i < x1.length - 1; i++) {
            for (int j = 0; j < x2.length - 1; j++) {
                double overlapXStart = Math.max(x1[i], x2[j]);
                double overlapXEnd = Math.min(x1[i + 1], x2[j + 1]);

                if (overlapXStart < overlapXEnd) { // 如果有重叠
                    double yMin = Math.min(y1[i], y2[j]);
                    double yMax = Math.max(y1[i], y2[j]);
                    totalOverlapArea += (overlapXEnd - overlapXStart) * (yMax - yMin);
                }
            }
        }

        double[] ny = (max(x1) > max(x2)) ? y1 : y2;
        double[] nx = (max(x1) > max(x2)) ? x1 : x2;
        double thre = Math.min(max(x1), max(x2));

        for (int i = 0; i < nx.length; i++) {
            if (nx[i] <= thre) {
                continue;
            } else {
                totalOverlapArea += (nx[i] - Math.max(thre, nx[i - 1])) * ny[i - 1];
            }
        }

        return totalOverlapArea;
    }

    public static double compute_all(List<LenwithProb> all) {
        double s = 0.0;
        for (int i = 0; i < all.size(); i++) {
            s += (all.get(i).getLength() - (i == all.size() - 1 ? 0 : all.get(i + 1).getLength())) * all.get(i).getProb();
        }
        return s;
    }

    public static double max(double[] array) {
        double max = array[0];
        for (double num : array) {
            if (num > max) {
                max = num;
            }
        }
        return max;
    }

    public static List<List<Double>> Error_Avg_List = new ArrayList<>();

    public static void record_avg_error(List<CondPath> our, List<LenwithProb> all) {
        int i = 0, j = 0, n = our.size(), m = all.size();
        int globalIndex = 0;
        double err = 0.0;
        while (i < n && j < m) {
            CondPath curOur = our.get(i);
            LenwithProb curAll = all.get(j);
            long curLen = Math.max(curOur.getLength(), curAll.getLength());

            if (curLen == curOur.getLength() && curLen == curAll.getLength()) {
                err += Math.abs(curOur.getGlobalProb() - curAll.getProb());
                i++;
                j++;
            } else if (curLen == curOur.getLength()) {
                err += curOur.getGlobalProb();
                i++;
            } else {
                err += curAll.getProb();
                j++;
            }

            globalIndex++;
        }
        while (i < n) {
            err += our.get(i).getGlobalProb();
            i++;
            globalIndex++;
        }
        while (j < m) {
            err += all.get(j).getProb();
            j++;
            globalIndex++;
        }
        Error_Avg_List.get(Error_Avg_List.size() - 1).add(err / (double) globalIndex);
    }
}

