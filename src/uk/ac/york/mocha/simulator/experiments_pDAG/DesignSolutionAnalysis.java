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

public class DesignSolutionAnalysis {

    static DecimalFormat df = new DecimalFormat("#.###");

    static int cores = 4;
    static int nos = 500;// 500 for time; 1000 for default
    static int intanceNum = 1;// 100

    static boolean print = false;

    static int startCond = 3;
    static int incrementCond = 1;
    static int endCond = 9;
    static int startAcp = 7;
    static int incrementAcp = 1;
    static int endAcp = 10;
    static int currt = 40;

    public static List<List<Double>> notPath = new ArrayList<>();

    public static long HeRTA = 0;
    public static long ZhaoRTA = 0;
    public static long GraRTA = 0;
    public static List<Long> OurRTA = new ArrayList<>();
    public static List<Long> ChenRTA = new ArrayList<>();

    public static TPDSHe He19;

    public static String resRootPath = "result/design_solution_analysis";

    public static void main(String args[]) {
        run();
        System.out.println();
    }

    public static void run() {
        System.out.println("Start running design solution analysis");

        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        SystemParameters.EXPthree = true;
        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }
        oneTaskWithFaults();

        backup.restore();
    }

    public static void run(int custStartCond, int custEndCond) {
        System.out.println("Start running design solution analysis");

        StaticFieldBackup backup = new StaticFieldBackup(SystemParameters.class);

        SystemParameters.EXPthree = true;
        while (notPath.size() < SystemParameters.FigLimit) {
            notPath.add(new ArrayList<>());
        }
        if (SystemParameters.CompareSingle || SystemParameters.EXPthree) {
            He19 = new TPDSHe();
        }

        int hyperPeriodNum = -1;
        int seed = 1000;

        for (int j = 0; j < endAcp - startAcp + 1; j++) {
            ChenRTA.add(0L);
            OurRTA.add(0L);
        }

        startCond = custStartCond;
        endCond = custEndCond;

        for (int i = startCond; i <= endCond; i = i + incrementCond) {
            SystemParameters.MAX_CondNum = i;
            System.out.println();
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_design_solution);
            writeCore(i);
            for (int j = 0; j < ChenRTA.size(); j++) {
                ChenRTA.set(j, 0L);
                OurRTA.set(j, 0L);
            }
            HeRTA = 0;
            ZhaoRTA = 0;
            GraRTA = 0;
        }

        backup.restore();
    }

    public static void oneTaskWithFaults() {

        int hyperPeriodNum = -1;
        int seed = 1000;

        for (int j = 0; j < endAcp - startAcp + 1; j++) {
            ChenRTA.add(0L);
            OurRTA.add(0L);
        }

        for (int i = startCond; i <= endCond; i = i + incrementCond) {
            SystemParameters.MAX_CondNum = i;
            System.out.println();
            RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.multi_pDAG_design_solution);
            writeCore(i);
            for (int j = 0; j < ChenRTA.size(); j++) {
                ChenRTA.set(j, 0L);
                OurRTA.set(j, 0L);
            }
            HeRTA = 0;
            ZhaoRTA = 0;
            GraRTA = 0;
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
        // exp three
        while (true) {
//            System.out.println(
//                    "Current psr: " + currt + " --- Current system number: " + (i + 1)
//                            + " --- Current collected tasks: " + record + " --- Current Parallelism: " + SystemParameters.maxParal
//                            + " --- Current conditional number: " + SystemParameters.MAX_CondNum);

            SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
            Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
                    null, false);
            DirectedAcyclicGraph dg = sys.getFirst().get(0);

            if (dg.bestDistance > dg.getSchedParameters().getDeadline()) {
                // discard, cannot be scheduled
                taskSeed++;
                i++;
                continue;
            }

            ProbRTA RTAtool = new ProbRTA(sys.getFirst().get(0));
            RTAtool.go();
            if (RTAtool.getFilterPath().size() < SystemParameters.FigLimit) {
                taskSeed++;
                i++;
                // task的path数量不达标
                continue;
            }

            // jjchen
            List<LenwithProb> all = null;
            if (SystemParameters.MAX_CondNum <= 7) {
                allProb Alltool = new allProb(sys.getFirst().get(0));
                try {
                    Alltool.go();
                } catch (OutOfMemoryError e) {
                    // 捕获到堆空间溢出，跳过当前任务
//                    System.err.println("Task " + i + " caused OutOfMemoryError, skipping...");
                    taskSeed++;
                    i++;
                    continue;
                    // 这里可以进行其他的清理或日志记录操作
                } catch (Exception e) {
                    // 处理其他可能的异常
//                    System.err.println("Task " + i + " caused an exception: " + e.getMessage());
                    taskSeed++;
                    i++;
                    continue;
                }
                all = Alltool.getPaths();
            }

            List<CondPath> our = RTAtool.getFilterPath();

            for (int pb = startAcp; pb <= endAcp; pb += incrementAcp) {
                SystemParameters.AccepetProb = Double.parseDouble(df.format((double) pb / (double) 10));
                // our method
                int l = 2, r = 200;
                while (l < r) {
                    int mid = (l + r) / 2;

                    double prob = 1.0;
                    for (CondPath pt : our) {
                        long RTB = (long) (pt.getLength() + Math.ceil((double) (dg.getSchedParameters().getWCET() - pt.getLength()) / mid));
                        if (RTB <= dg.getSchedParameters().getDeadline()) break;
                        else prob -= pt.getGlobalProb();
                    }

                    // 根据判定条件进行判断
                    if (prob >= SystemParameters.AccepetProb) {
                        r = mid;  // 如果 mid 满足条件，更新左边界
                    } else {
                        l = mid + 1;  // 如果 mid 不满足条件，更新右边界
                    }
                }
                OurRTA.set(pb - startAcp, OurRTA.get(pb - startAcp) + l);

                if (SystemParameters.MAX_CondNum <= 7) {
                    l = 2;
                    r = 200;
                    while (l < r) {
                        int mid = (l + r) / 2;

                        double prob = 1.0;
                        for (LenwithProb pt : all) {
                            long RTB = (long) (pt.getLength() + Math.ceil((double) (dg.getSchedParameters().getWCET() - pt.getLength()) / mid));
                            if (RTB <= dg.getSchedParameters().getDeadline()) break;
                            else prob -= pt.getProb();
                        }

                        // 根据判定条件进行判断
                        if (prob >= SystemParameters.AccepetProb) {
                            r = mid;  // 如果 mid 满足条件，更新左边界
                        } else {
                            l = mid + 1;  // 如果 mid 不满足条件，更新右边界
                        }
                    }
                    ChenRTA.set(pb - startAcp, ChenRTA.get(pb - startAcp) + l);
                }
            }

//				// He19
//				int l = 2;
//				int r = 200;
//				while (l < r) {
//					int mid = (l + r) / 2;
//
//					// 根据判定条件进行判断
//					if (He19.getResponseTime(sys.getFirst(), mid).get(0).best_response_time <= dg.getSchedParameters().getDeadline()) {
//						r = mid;  // 如果 mid 满足条件，更新左边界
//					} else {
//						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
//					}
//				}
////				long brt = He19.getResponseTime(sys.getFirst(), l).get(0).best_response_time;
//				HeRTA += l;
//
//				// Zhao20
//				l = 2;
//				r = 200;
//				while (l < r) {
//					int mid = (l + r) / 2;
//
//					// 根据判定条件进行判断
//					if (DAGtoPython.pharseDAGForPython(sys.getFirst().get(0), mid).getFirst() <= dg.getSchedParameters().getDeadline()) {
//						r = mid;  // 如果 mid 满足条件，更新左边界
//					} else {
//						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
//					}
//				}
//				ZhaoRTA += l;

            // Graham
            int l = 2;
            int r = 200;
            while (l < r) {
                int mid = (l + r) / 2;

                // 根据判定条件进行判断
//                if (dg.bestDistance + (dg.getSchedParameters().getWCET() - dg.bestDistance) / mid <= dg.getSchedParameters().getDeadline()) {
                if (dg.bestDistance + (long) Math.ceil((double) (dg.getSchedParameters().getWCET() - dg.bestDistance) / mid) <= dg.getSchedParameters().getDeadline()) {
                    r = mid;  // 如果 mid 满足条件，更新左边界
                } else {
                    l = mid + 1;  // 如果 mid 不满足条件，更新右边界
                }
            }
            GraRTA += l;

            record++;

            // visual progress bar
            long elapsed = System.currentTimeMillis() - startTime;
            double percent = (double) record / SystemParameters.TargetNum;
            int completedBars = (int) (percent * barLength);
            int remainingBars = barLength - completedBars;
            double elapsedSec = elapsed / 1000.0;
            double etaSec = (record == 0) ? 0 : elapsedSec / record * (SystemParameters.TargetNum - record);
            String progressBar = "Table V : condNum = " + SystemParameters.MAX_CondNum + " : "
                    + "[" + "#".repeat(completedBars) + "-".repeat(remainingBars) + "]";
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


    public static double max(double[] array) {
        double max = array[0];
        for (double num : array) {
            if (num > max) {
                max = num;
            }
        }
        return max;
    }

    public static void writeCore(int cond) {
        Path path = Paths.get(resRootPath, "CoresCMP_cond_" + String.valueOf(cond) + ".txt");
        File file = new File(path.toString());
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (Long value : OurRTA) {
                writer.write(String.format("%.4f", (double) value / SystemParameters.TargetNum) + ", ");
                writer.newLine();
            }
            for (Long aLong : ChenRTA) {
                writer.write(String.format("%.4f", (double) aLong / SystemParameters.TargetNum) + ", ");
                writer.newLine();
            }
            writer.write(String.format("%.4f", (double) ZhaoRTA / SystemParameters.TargetNum) + ", ");
            writer.newLine();
            writer.write(String.format("%.4f", (double) HeRTA / SystemParameters.TargetNum));
            writer.newLine();
            writer.write(String.format("%.4f", (double) GraRTA / SystemParameters.TargetNum));
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

