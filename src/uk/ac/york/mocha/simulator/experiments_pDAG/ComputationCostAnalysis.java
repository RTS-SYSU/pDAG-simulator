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

public class ComputationCostAnalysis {

    static DecimalFormat df = new DecimalFormat("#.###");

    static int cores = 4;
    static int nos = 500;// 500 for time; 1000 for default
    static int intanceNum = 1;// 100
    static int currt = 40;

    static boolean print = false;
    static boolean RTA = true;
    static int startCond = 2;
    static int incrementCond = 1;
    static int endCond = 10;

    public static List<List<Double>> notPath = new ArrayList<>();

    public static List<List<Double>> timeOur = new ArrayList<>();
    public static List<List<Double>> timeAll = new ArrayList<>();
    public static List<List<Double>> timeHe = new ArrayList<>();
    public static List<List<Double>> timeZhao = new ArrayList<>();
    public static List<Integer> timeIdx = new ArrayList<>();

    public static TPDSHe He19;
    public static String param = "cond";

    public static String resRootPath = "result/computation_cost_analysis";

    public static void main(String args[]) {
//        System.out.println("The second experiment about measuring time has been completed in the previous experiment to avoid wasting time.");
        run();
        System.out.println();
    }

    public static void run() {
        System.out.println("Start running computation cost analysis");
        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        oneTaskWithFaults();

        backup.restore();
    }

    public static void run(int custStartCond, int custEndCond){
        System.out.println("Start running computation cost analysis");
        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }

        int hyperPeriodNum = -1;
        int seed = 1000;
        param = "cond";
        startCond = custStartCond;
        endCond = custEndCond;
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
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
            OneWriteTime(numberCond, param);
            Multiwrite(param);
        }

        backup.restore();
    }

    public static void oneTaskWithFaults() {
        int hyperPeriodNum = -1;
        int seed = 1000;
        param = "cond";
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
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
            OneWriteTime(numberCond, param);
            Multiwrite(param);
        }
    }

    public static void OneWriteTime(int param, String type) {
        Path path = Paths.get(resRootPath, "time_all_" + type + "_" + String.valueOf(param) + ".txt");
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
        BufferedWriter writer;

        File file = new File(Paths.get(resRootPath, "time_Chen_" + type + ".txt").toString());
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

        file = new File(Paths.get(resRootPath, "time_Our_" + type + ".txt").toString());
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

        file = new File(Paths.get(resRootPath, "time_He_" + type + ".txt").toString());
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

        file = new File(Paths.get(resRootPath, "time_Zhao_" + type + ".txt").toString());
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
            System.out.println("Cannot get same instances number for randomly generated periods.");
        }

        long startTime = System.currentTimeMillis();
        int barLength = 40;

        int record = 0, i = 0;
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
                        OneWriteTime(SystemParameters.MAX_Cond, "cond");
                        taskSeed++;
                        i++;
                        continue;
                        // 这里可以进行其他的清理或日志记录操作
                    } catch (Exception e) {
                        // 处理其他可能的异常
//                        System.err.println("Task " + i + " caused an exception: " + e.getMessage());
                        OneWriteTime(SystemParameters.MAX_Cond, "cond");
                        taskSeed++;
                        i++;
                        continue;
                    }
                    List<LenwithProb> all = Alltool.getPaths();
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

            String progressBar = ("Figs. 6 : condNum = " + SystemParameters.MAX_CondNum + " : ");

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
        Error_Avg_List.get(Error_Avg_List.size() - 1).add(1.0 - rt);
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

    public static List<List<Double>> Error_Avg_List = new ArrayList<>();

}

