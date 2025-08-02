package uk.ac.york.mocha.simulator.experiments_pDAG;

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
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Allocation;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.ExpName;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.Hardware;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.RecencyType;
import uk.ac.york.mocha.simulator.parameters.SystemParameters.SimuType;
import uk.ac.york.mocha.simulator.resultAnalyzer.OneSystemResults;
import uk.ac.york.mocha.simulator.simulator.Simualtor;
import uk.ac.york.mocha.simulator.simulator.SimualtorNWC;
import uk.ac.york.mocha.simulator.simulator.Utils;

import java.io.*;

public class AllExprs {

	static DecimalFormat df = new DecimalFormat("#.###");

	static int cores = 4;
	static int nos = 500;// 500 for time; 1000 for default
	static int bias = 0;// 500
	static int intanceNum = 1;// 100

	static int startUtil = 4;
	static int incrementUtil = 4;
	static int endUtil = 40;
	static double scaleU = 1.0;

	static int startParal = 3;
	static int incrementParal = 1;
	static int endParal = 10;
	static int startrt = 10;
	static int incrementrt = 10;
	static int endrt = 80;
	static int currt = 40;

	static boolean print = false;

	static boolean printInfo = false;
	static boolean RTA = true;
	static int startCond = 2;
	static int incrementCond = 1;
	static int endCond = 3;
	static int startAcp = 6;
	static int incrementAcp = 1;
	static int endAcp = 6;

	public static List<Long> avgOur = new ArrayList<>();
	public static List<Long> avgAll = new ArrayList<>();
	public static List<List<Double>> notPath = new ArrayList<>();

	public static List<List<Integer>> avgNum = new ArrayList<>();

	public static List<List<Double>> timeOur = new ArrayList<>();
	public static List<List<Double>> timeAll = new ArrayList<>();
	public static List<List<Double>> timeHe = new ArrayList<>();
	public static List<List<Double>> timeZhao = new ArrayList<>();
	public static long HeRTA = 0;
	public static long ZhaoRTA = 0;
	public static long GraRTA = 0;
	public static List<Long> OurRTA = new ArrayList<>();
	public static List<Long> ChenRTA = new ArrayList<>();
	public static List<Integer> timeIdx = new ArrayList<>();
	public static int basketCnt;
	public static List<Boolean> flagBits;

	public static TPDSHe He19;
	public static String param;

	public static void main(String args[]) {
		while(notPath.size() < SystemParameters.FigLimit){
			notPath.add(new ArrayList<>());
		}
		if(SystemParameters.CompareSingle || SystemParameters.EXPthree){
			He19 = new TPDSHe();
		}
		oneTaskWithFaults();
	}

	public static Map<Integer, Double> psr2errorRate = new LinkedHashMap<>();
	public static void oneTaskWithFaults() {

		int hyperPeriodNum = -1;
		int seed = 1000;

		if(!SystemParameters.EXPthree) {
			// param = "util";
			// param = "para";
//			param = "cond";
			 param = "ratio";
			switch (param) {
				case "ratio":
					// SystemParameters.utilPerTask = Double.parseDouble(df.format((double) 4 / (double) 10));
					for (int i = startrt; i <= endrt; i = i + incrementrt) {
						currt = i;
						timeIdx.add(i);
						timeOur.add(new ArrayList<>());
						timeAll.add(new ArrayList<>());
						timeHe.add(new ArrayList<>());
						timeZhao.add(new ArrayList<>());
						Error_Avg_List.add(new ArrayList<>());
						RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
						Onewrite(i, param);
						OneWriteTime(i, param);
					}
					Multiwrite(param);

					for (Map.Entry<Integer, Double> entry : psr2errorRate.entrySet()){
						System.out.println("Total error rate under psr="+entry.getKey()+" : " + entry.getValue());
					}
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
						RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
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
						RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
						Onewrite(numberCond, param);
						OneWriteTime(numberCond, param);
						Multiwrite(param);
					}
			}
		}
		else {
			for(int j = 0; j < endAcp - startAcp + 1; j ++){
				ChenRTA.add(0L);
				OurRTA.add(0L);
			}
			for (int i = startCond; i <= endCond; i = i + incrementCond) {
				SystemParameters.MAX_CondNum = i;
				RunOneGroup_new(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict);
				writeCore(i);
				for(int j = 0; j < ChenRTA.size(); j ++){
					ChenRTA.set(j, 0L);
					OurRTA.set(j, 0L);
				}
				HeRTA = 0;
				ZhaoRTA = 0;
				GraRTA = 0;
			}
		}
	}

	public static void Onewrite(int param, String type) {
		File file = new File("testOut/final/one/avg_" + type + "_" + String.valueOf(param) + ".txt");
		BufferedWriter writer;
		List<Double> out = Error_Avg_List.get(Error_Avg_List.size() - 1);
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (Double ele : out){
				writer.write(String.format("%.4f", ele) + ", ");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void OneWriteTime(int param, String type){
		File file = new File("testOut/final/two/time_all_" + type + "_" + String.valueOf(param) + ".txt");
		BufferedWriter writer;
		List<Double> res = timeAll.get(timeAll.size() - 1);
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write("Chen:");
			writer.newLine();
			for (Double ele : res){
				writer.write(String.valueOf(ele) + ", ");
			}
			writer.newLine();

			res = timeOur.get(timeOur.size() - 1);
			writer.write("Our:");
			writer.newLine();
			for (Double ele : res){
				writer.write(String.valueOf(ele) + ", ");
			}
			writer.newLine();

			res = timeHe.get(timeHe.size() - 1);
			writer.write("He:");
			writer.newLine();
			for (Double ele : res){
				writer.write(String.valueOf(ele) + ", ");
			}
			writer.newLine();

			res = timeZhao.get(timeZhao.size() - 1);
			writer.write("Zhao:");
			writer.newLine();
			for (Double ele : res){
				writer.write(String.valueOf(ele) + ", ");
			}

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void Multiwrite(String type) {
		File file = new File("testOut/final/one/avg_Err_" + type + ".txt");
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> l : Error_Avg_List){
				for (Double ele : l) writer.write(String.format("%.4f", ele) + ", ");
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/final/two/time_Chen_" + type + ".txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for(List<Double> l : timeAll) {
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

		file = new File("testOut/final/two/time_Our_" + type + ".txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for(List<Double> l : timeOur) {
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

		file = new File("testOut/final/two/time_He_" + type + ".txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for(List<Double> l : timeHe) {
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

		file = new File("testOut/final/two/time_Zhao_" + type + ".txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for(List<Double> l : timeZhao) {
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

	public static void oneTaskWithFaults_FixConds() {
		int hyperPeriodNum = -1;
		int seed = 1000;

		for (int i = startUtil; i <= endUtil; i = i + incrementUtil) {
			SystemParameters.utilPerTask = Double.parseDouble(df.format((double) i / (double) 10));
			for(int numberCond = startCond; numberCond <= endCond; numberCond += incrementCond){
				SystemParameters.MAX_CondNum = numberCond;
				timeIdx.add(numberCond);
				timeOur.add(new ArrayList<>());
				timeAll.add(new ArrayList<>());
				Error_Avg_List.add(new ArrayList<>());
				RunOneGroup_FixCond(1, intanceNum, hyperPeriodNum, true, null, seed, seed, null, nos, true, ExpName.predict, numberCond);
				// Onewrite(numberCond);
			}
			// Multiwrite();
		}
	}

	static boolean bigger = false;

	public static void RunOneGroup_new(int taskNum, int intanceNum, int hyperperiodNum, boolean takeAllUtil,
								   List<List<Double>> util, int taskSeed, int tableSeed, List<List<Long>> periods, int NoS, boolean randomC,
								   ExpName name) {
		int totalErrorNum = 0;

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


		int record = 0, i = 0;
		if(!SystemParameters.EXPthree){
			// exp one
			while (true) {
				System.out.println(
						"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1)
								+ " --- Current collected tasks: " + record + " --- Current conditional number: " + SystemParameters.MAX_CondNum
								+ " --- Current paral: " + SystemParameters.maxParal);

				SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
				Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
						null, false);

				// output infos for python to figure
//			if (printInfo) {
//				// printDAGinfo(sys.getFirst().get(0));
//				printInfo = false;
//			}
//				// printDAGinfo(sys.getFirst().get(0));
//				if(Objects.equals(param, "util")){
//					for(Node nd : sys.getFirst().get(0).getFlatNodes()){
//						nd.WCET *= scaleU;
//						if(nd.isCond) {
//							nd.idx2len.forEach((key, value) -> nd.idx2len.put(key, (long) (value * scaleU)));
//							nd.correspondLen.replaceAll(value -> (long) (value * scaleU));
//						}
//					}
//				}
//				sys.getFirst().get(0).bestDistance *= scaleU;
//				sys.getFirst().get(0).getSchedParameters().setWCET((long) (sys.getFirst().get(0).getSchedParameters().getWCET() * scaleU));

//				DirectedAcyclicGraph dag = sys.getFirst().get(0);
//				List<Node> csq = new ArrayList<>();
//				long cswq = 0L;
//				for(Node nd : dag.getFlatNodes()){
//					if(nd.isCond){
//						csq.add(nd);
//						cswq += nd.getWCET();
//					}
//				}
//				long Ww = dag.getSchedParameters().getWCET();
//				double rtq = (double) cswq / (double) Ww;
				if(currt != 0){
					// rearrange workload
					DirectedAcyclicGraph dg = sys.getFirst().get(0);
					long W = dg.getSchedParameters().getWCET();
					List<Node> cs = new ArrayList<>();
					long csw = 0L;
					for(Node nd : dg.getFlatNodes()){
						if(nd.isCond){
							cs.add(nd);
							csw += nd.getWCET();
						}
					}
					double scale = (double) ((W - csw) * currt * 0.01) / ((double) csw * (1 - currt * 0.01));
					long cre = 0L;
					for(Node nd : cs){
						long nwcet = (long) (nd.getWCET() * scale);
						cre += nwcet - nd.getWCET();
						nd.setWCET(nwcet);
						nd.idx2len.forEach((key, value) -> nd.idx2len.put(key, (long) (value * scale)));
						nd.correspondLen.replaceAll(value -> (long) (value * scale));
					}
					dg.getSchedParameters().setWCET(W + cre);

					csw = 0L;
					for(Node nd : dg.getFlatNodes()){
						if(nd.isCond){
							cs.add(nd);
							csw += nd.getWCET();
						}
					}
					W = dg.getSchedParameters().getWCET();
					double rt = (double) csw / (double) W;
					int debug = dg.getFlatNodes().size();
					int fuck = 0;
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

					if(SystemParameters.MAX_CondNum <= 7){
						allProb Alltool = new allProb(sys.getFirst().get(0));
						stime = System.nanoTime();
						try {
							Alltool.go();
						} catch (OutOfMemoryError e) {
							// 捕获到堆空间溢出，跳过当前任务
							System.err.println("Task " + i + " caused OutOfMemoryError, skipping...");
							Onewrite(SystemParameters.MAX_Cond, "cond");
							OneWriteTime(SystemParameters.MAX_Cond, "cond");
							taskSeed++;
							i++;
							continue;
							// 这里可以进行其他的清理或日志记录操作
						} catch (Exception e) {
							// 处理其他可能的异常
							System.err.println("Task " + i + " caused an exception: " + e.getMessage());
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
//					int debug = dg.getFlatNodes().size();
//					int fuck = 0;
						timeAll.get(timeAll.size() - 1).add((System.nanoTime() - stime) / 1_000_000.0);

						double totalProb = 0.0;
						for (CondPath condPath : our){
							totalProb += condPath.globalProb;
						}
						if (Math.abs(totalProb - 1.0) > 1e-5){
							System.out.println("RTA error!!");
							System.exit(-1);
						}
						if (our.size() != all.size()){
							System.out.println("error!!!!");
							totalErrorNum += 1;
						}
//						 record_avg_error(our, all);
//						 compute_lap_percent(our, all);
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
				if (record >= SystemParameters.TargetNum) {
					// enough collected tasks
					psr2errorRate.put(currt, (double)totalErrorNum / SystemParameters.TargetNum);
					break;
				}

				taskSeed++;
				i++;
			}
		}
		else {
			// exp three
			while (true) {
				System.out.println(
						"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1)
								+ " --- Current collected tasks: " + record + " --- Current conds: " + SystemParameters.MAX_CondNum);

				SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
				Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
						null, false);
				DirectedAcyclicGraph dg = sys.getFirst().get(0);

				if (dg.bestDistance > dg.getSchedParameters().getDeadline()){
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
				if(SystemParameters.MAX_CondNum <= 7){
					allProb Alltool = new allProb(sys.getFirst().get(0));
					try {
						Alltool.go();
					} catch (OutOfMemoryError e) {
						// 捕获到堆空间溢出，跳过当前任务
						System.err.println("Task " + i + " caused OutOfMemoryError, skipping...");
						taskSeed++;
						i++;
						continue;
						// 这里可以进行其他的清理或日志记录操作
					} catch (Exception e) {
						// 处理其他可能的异常
						System.err.println("Task " + i + " caused an exception: " + e.getMessage());
						taskSeed++;
						i++;
						continue;
					}
					all = Alltool.getPaths();
				}

				List<CondPath> our = RTAtool.getFilterPath();

				for(int pb = startAcp; pb <= endAcp; pb += incrementAcp){
					SystemParameters.AccepetProb = Double.parseDouble(df.format((double) pb / (double) 10));
					// our method
					int l = 2, r = 200;
					while (l < r) {
						int mid = (l + r) / 2;

						double prob = 1.0;
						for(CondPath pt : our){
							long RTB = (long) (pt.getLength() + Math.ceil((double) (dg.getSchedParameters().getWCET() - pt.getLength()) / mid));
							if(RTB <= dg.getSchedParameters().getDeadline()) break;
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

					if(SystemParameters.MAX_CondNum <= 7){
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
					if (dg.bestDistance + (dg.getSchedParameters().getWCET() - dg.bestDistance) / mid <= dg.getSchedParameters().getDeadline()) {
						r = mid;  // 如果 mid 满足条件，更新左边界
					} else {
						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
					}
				}
				GraRTA += l;

				record++;
				if (record >= SystemParameters.TargetNum) {
					// enough collected tasks
					break;
				}

				taskSeed++;
				i++;
			}
		}
	}

	public static void compute_lap_percent(List<CondPath> our, List<LenwithProb> all){
		Error_Avg_List.get(Error_Avg_List.size() - 1).add(compute_overlap(our, all) / compute_all(all));
	}

	public static void compute_area_rto(List<CondPath> our, List<LenwithProb> all){
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

	public static double compute_all(List<LenwithProb> all){
		double s = 0.0;
		for(int i = 0; i < all.size(); i ++){
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
		while(i < n && j < m){
			CondPath curOur = our.get(i);
			LenwithProb curAll = all.get(j);
			long curLen = Math.max(curOur.getLength(), curAll.getLength());

			if(curLen == curOur.getLength() && curLen == curAll.getLength()){
				err += Math.abs(curOur.getGlobalProb() - curAll.getProb());
				i ++;
				j ++;
			}
			else if (curLen == curOur.getLength()){
				err += curOur.getGlobalProb();
				i ++;
			}
			else {
				err += curAll.getProb();
				j ++;
			}

			globalIndex ++;
		}
		while(i < n){
			err += our.get(i).getGlobalProb();
			i ++;
			globalIndex ++;
		}
		while(j < m){
			err += all.get(j).getProb();
			j ++;
			globalIndex ++;
		}
		Error_Avg_List.get(Error_Avg_List.size() - 1).add(err / (double) globalIndex);
	}

	public static void RunOneGroup(int taskNum, int intanceNum, int hyperperiodNum, boolean takeAllUtil,
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


		int record = 0, i = 0;
		if(!SystemParameters.EXPthree){
			while (true) {
				System.out.println(
						"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1)
								+ " --- Current collected tasks: " + record);

				SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
				Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
						null, false);

				// output infos for python to figure
//			if (printInfo) {
//				// printDAGinfo(sys.getFirst().get(0));
//				printInfo = false;
//			}
				// printDAGinfo(sys.getFirst().get(0));

				if (i + 1 == 868) {
					i++;
					taskSeed++;
					continue;
				}

				if (RTA) {
					ProbRTA RTAtool = new ProbRTA(sys.getFirst().get(0));
					RTAtool.go();
					// printDAGinfo(sys.getFirst().get(0));
					List<CondPath> our = RTAtool.getFilterPath();

					allProb Alltool = new allProb(sys.getFirst().get(0));
					Alltool.go();
					List<LenwithProb> all = Alltool.getPaths();

					int globalIndex = computeErr(our, all, RTAtool, Alltool);

					computeNumandProb(globalIndex, RTAtool);
					record++;
				}
				if (record >= SystemParameters.TargetNum) {
					// enough collected tasks
					break;
				}

				taskSeed++;
				i++;
			}
			processAll();
		}
		else {
			// exp three
			while (true) {
				System.out.println(
						"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1)
								+ " --- Current collected tasks: " + record);

				SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
				Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
						null, false);
				DirectedAcyclicGraph dg = sys.getFirst().get(0);
				if (i + 1 == 868) {
					i++;
					taskSeed++;
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

				int l = 2, r = 200;
				// our method
				List<CondPath> our = RTAtool.getFilterPath();
				while (l < r) {
					int mid = (l + r) / 2;

					double prob = 1.0;
					for(CondPath pt : our){
						long RTB = (long) (pt.getLength() + Math.ceil((double) (dg.getSchedParameters().getWCET() - pt.getLength()) / mid));
						if(RTB <= dg.getSchedParameters().getDeadline()) break;
						else prob -= pt.getGlobalProb();
					}

					// 根据判定条件进行判断
					if (prob <= SystemParameters.AccepetProb) {
						r = mid;  // 如果 mid 满足条件，更新左边界
					} else {
						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
					}
				}
				// OurRTA += l;

				// jjchen
				allProb Alltool = new allProb(sys.getFirst().get(0));
				Alltool.go();
				List<LenwithProb> all = Alltool.getPaths();

				l = 2;
				r = 200;
				while (l < r) {
					int mid = (l + r) / 2;

					double prob = 1.0;
					for(LenwithProb pt : all){
						long RTB = (long) (pt.getLength() + Math.ceil((double) (dg.getSchedParameters().getWCET() - pt.getLength()) / mid));
						if(RTB <= dg.getSchedParameters().getDeadline()) break;
						else prob -= pt.getProb();
					}

					// 根据判定条件进行判断
					if (prob <= SystemParameters.AccepetProb) {
						r = mid;  // 如果 mid 满足条件，更新左边界
					} else {
						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
					}
				}
				List<Long> test = new ArrayList<>();
				for(LenwithProb pt : all){
					long RTB = (long) (pt.getLength() + Math.ceil((double) (dg.getSchedParameters().getWCET() - pt.getLength()) / l));
					if(RTB <= dg.getSchedParameters().getDeadline()) break;
					else test.add(RTB);
				}

				// ChenRTA += l;

				// He19
				l = 2;
				r = 200;
				while (l < r) {
					int mid = (l + r) / 2;

					// 根据判定条件进行判断
					if (He19.getResponseTime(sys.getFirst(), mid).get(0).best_response_time <= dg.getSchedParameters().getDeadline()) {
						r = mid;  // 如果 mid 满足条件，更新左边界
					} else {
						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
					}
				}
				long brt = He19.getResponseTime(sys.getFirst(), l).get(0).best_response_time;
				HeRTA += l;

				// Zhao20
				l = 2;
				r = 200;
				while (l < r) {
					int mid = (l + r) / 2;

					// 根据判定条件进行判断
					if (DAGtoPython.pharseDAGForPython(sys.getFirst().get(0), mid).getFirst() <= dg.getSchedParameters().getDeadline()) {
						r = mid;  // 如果 mid 满足条件，更新左边界
					} else {
						l = mid + 1;  // 如果 mid 不满足条件，更新右边界
					}
				}
				ZhaoRTA += l;

				record++;
				if (record >= SystemParameters.TargetNum) {
					// enough collected tasks
					break;
				}

				taskSeed++;
				i++;
			}
			// writeCore();
		}
	}

	public static void writeCore(int cond){
		File file = new File("testOut/final/three/CoresCMP_cond_" + String.valueOf(cond) + ".txt");
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
			writer.write(String.format("%.4f", (double) ZhaoRTA /  SystemParameters.TargetNum) + ", ");
			writer.newLine();
			writer.write(String.format("%.4f", (double) HeRTA /  SystemParameters.TargetNum));
			writer.newLine();
			writer.write(String.format("%.4f", (double) GraRTA /  SystemParameters.TargetNum));
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void RunOneGroupBasket(int taskNum, int intanceNum, int hyperperiodNum, boolean takeAllUtil,
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


		// 初始化计数器
		basketCnt = 0;
		flagBits = new ArrayList<>();
		for(int i = 0; i < SystemParameters.FigLimit; i ++){
			flagBits.add(false);
		}
		int i = 0;
		while (true) {
			System.out.println(
					"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1)
							+ " --- Current collected tasks: " + basketCnt);

			SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
			Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
					null, false);

			// output infos for python to figure
			if (printInfo) {
				// printDAGinfo(sys.getFirst().get(0));
				printInfo = false;
			}

			if(RTA){
				ProbRTA RTAtool = new ProbRTA(sys.getFirst().get(0));
				RTAtool.go();
				List<CondPath> our = RTAtool.getFilterPath();

				allProb Alltool = new allProb(sys.getFirst().get(0));
				Alltool.go();
				List<LenwithProb> all = Alltool.getPaths();

				int globalIndex = computeErrN(our, all);
			}

			if(basketCnt == flagBits.size()){
				break;
			}

			taskSeed++;
			i ++;
		}

		processAll();
	}

	public static void RunOneGroup_FixCond(int taskNum, int intanceNum, int hyperperiodNum, boolean takeAllUtil,
								   List<List<Double>> util, int taskSeed, int tableSeed, List<List<Long>> periods, int NoS, boolean randomC,
								   ExpName name, int conds) {

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

		int i = bias, cnt = 0, tmp = timeOur.size() - 1;
		while (cnt < nos) {
			System.out.println(
					"Util per task: " + SystemParameters.utilPerTask + " --- Current system number: " + (i + 1)
					+ " --- Current conds: " + conds + " --- Current collected tasks: " + cnt);
//			if(i + 1 == 156 || i + 1 == 173 || i + 1 == 208 || i + 1 == 241 || i + 1 == 272 || i + 1 == 307 || i + 1 == 309) continue;
//			if(i + 1 == 277 || i + 1 == 292 || i + 1 == 302 || i + 1 == 304 || i + 1 == 305 || i + 1 == 306) continue;
//			if(i + 1 == 868) continue;

			SystemGenerator gen = new SystemGenerator(cores, 1, true, true, null, taskSeed + i * 10, true, print);
			Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys = gen.generatedDAGInstancesInOneHP(intanceNum, -1,
					null, false);

			if(sys.getFirst().get(0).getFlatNodes().size() < SystemParameters.MAX_CondNum + 2){
				// 不足以产生相应数量的conditional nodes
				taskSeed++;
				i ++;
				continue;
			}

			// output infos for python to figure
//			if (printInfo) {
//				// printDAGinfo(sys.getFirst().get(0));
//				printInfo = false;
//			}

			if(RTA){
				ProbRTA RTAtool = new ProbRTA(sys.getFirst().get(0));
				long stime = System.nanoTime();
				RTAtool.go();
				double ftime = (System.nanoTime() - stime) / 1_000_000.0;
				timeOur.get(tmp).add(ftime);

				allProb Alltool = new allProb(sys.getFirst().get(0));
				stime = System.nanoTime();
				Alltool.go();
				ftime = (System.nanoTime() - stime) / 1_000_000.0;
				timeAll.get(tmp).add(ftime);

				if (SystemParameters.CompareSingle) {
					stime = System.nanoTime();
					long empty = He19.getResponseTime(sys.getFirst(), SystemParameters.coreNum).get(0).best_response_time;
					ftime = (System.nanoTime() - stime) / 1_000_000.0;
//					timeHe.add(ftime);

					stime = System.nanoTime();
					empty = DAGtoPython.pharseDAGForPython(sys.getFirst().get(0), SystemParameters.coreNum).getFirst();
					ftime = (System.nanoTime() - stime) / 1_000_000.0;
//					timeZhao.add(ftime);
				}
			}
			taskSeed++;
			i ++;
			cnt ++;
		}
//		processAllRTA(); yet to be finished
	}

	public static void processAllRTA() {
		File file = new File("testOut/RTAboxplot.txt");
		BufferedWriter writer;

		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : pathErr){
				for (Double ele : cur){
					writer.write(String.format("%.4f", ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/RTApOUR.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : pathOur){
				for (Double ele : cur){
					writer.write(String.format("%.4f", ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/pAll.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : pathAll){
				for (Double ele : cur){
					writer.write(String.format("%.4f", ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public static void printDelta() {
//		File file = new File("testOut/DeltaThree.txt");
//		BufferedWriter writer;
//
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (Double cur : DeltaThree){
//				writer.write(String.valueOf(cur) + ", ");
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		file = new File("testOut/DeltaFour.txt");
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (Double cur : DeltaFour){
//				writer.write(String.valueOf(cur) + ", ");
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

//	public static void printNotPath() {
//		File file = new File("testOut/SingleNotExist.txt");
//		BufferedWriter writer;
//
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (List<Double> cur : notPath){
//				for (Double ele : cur){
//					writer.write(String.valueOf(ele) + ", ");
//				}
//				writer.newLine();
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

//	public static void processNumandProb() {
//		File file = new File("testOut/CoexNum.txt");
//		BufferedWriter writer;
//
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (List<Integer> cur : avgNum){
//				for (Integer ele : cur){
//					writer.write(String.valueOf(ele) + ", ");
//				}
//				writer.newLine();
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

//		File file = new File("testOut/CoexProb.txt");
//		BufferedWriter writer;
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (List<Double> cur : avgProb){
//				for (Double ele : cur){
//					writer.write(String.valueOf(ele) + ", ");
//				}
//				writer.newLine();
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public static void computeNumandProb(int globalIndex, ProbRTA RTAtool) {
		if(globalIndex > avgNum.size()){
			int diff = globalIndex - avgNum.size();
			while(diff > 0){
				avgNum.add(new ArrayList<>());
//				avgProb.add(new ArrayList<>());
				diff --;
			}
		}
		List<List<Double>> curData = RTAtool.notCoProb;
		for(int i = 0; i < SystemParameters.FigLimit; i ++){
			avgNum.get(i).add(curData.get(i).size());

//			Double sum = 0.0;
//			for(Double j : curData.get(i)){
//				sum += j;
//			}
//			avgProb.get(i).add(curData.get(i).size() == 0 ? 0 : sum / (double) curData.get(i).size());
		}
	}


//	public static void processGlobAvg(){
//		double avgO = calculate(avgOur);
//		double avgA = calculate(avgAll);
//		File file = new File("testOut/time.txt");
//		BufferedWriter writer;
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			writer.write(String.valueOf("Average execution time of " + String.valueOf(nos) + " systems under our method is: " + String.valueOf(avgO)));
//			writer.newLine();
//			writer.write(String.valueOf("Average execution time of " + String.valueOf(nos) + " systems under all is: " + String.valueOf(avgA)));
//
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

//	public static void processTime() {
//		File file = new File("testOut/timeOur.txt");
//		BufferedWriter writer;
//
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (List<Long> cur : timeArrOur){
//				for (Long ele : cur){
//					writer.write(String.valueOf(ele) + ", ");
//				}
//				writer.newLine();
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		file = new File("testOut/timeAll.txt");
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (List<Long> cur : timeArrAll){
//				for (Long ele : cur){
//					writer.write(String.valueOf(ele) + ", ");
//				}
//				writer.newLine();
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public static void processTimeNew() {
		File file = new File("testOut/timeOur.txt");
		BufferedWriter writer;

		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : timeOur){
				for (double ele : cur){
					writer.write(String.valueOf(ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/timeAll.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : timeAll){
				for (Double ele : cur){
					writer.write(String.valueOf(ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/timeIdx.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (Integer cur : timeIdx){
				writer.write(String.valueOf(cur));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public static void computeTime(int globalIndex, long timeOur, long timeAll) {
//		if(globalIndex > timeArrOur.size()){
//			int diff = globalIndex - timeArrOur.size();
//			while(diff > 0){
//				timeArrOur.add(new ArrayList<>());
//				timeArrAll.add(new ArrayList<>());
//				diff --;
//			}
//		}
//		timeArrOur.get(globalIndex - 1).add(timeOur);
//		timeArrAll.get(globalIndex - 1).add(timeAll);
//	}

	public static void processAll() {
		File file = new File("testOut/boxplot.txt");
		BufferedWriter writer;

		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : pathErr){
				for (Double ele : cur){
					writer.write(String.format("%.4f", ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/pOUR.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : pathOur){
				for (Double ele : cur){
					writer.write(String.format("%.4f", ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File("testOut/pAll.txt");
		try {
			writer = new BufferedWriter(new FileWriter(file));
			for (List<Double> cur : pathAll){
				for (Double ele : cur){
					writer.write(String.format("%.4f", ele) + ", ");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public static void processAvg() {
//		List<Double> res = new ArrayList<>();
//		for (List<Double> cur : pathErr){
//			res.add(calculateDB(cur));
//		}
//
//		File file = new File("testOut/analysis.txt");
//		BufferedWriter writer;
//
//		try {
//			writer = new BufferedWriter(new FileWriter(file));
//			for (Double i : res){
//				writer.write(String.format("%.4f", i) + ", ");
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public static List<List<Double>> pathErr = new ArrayList<>();
	public static List<List<Double>> pathAll = new ArrayList<>();
	public static List<List<Double>> pathOur = new ArrayList<>();
	public static List<Double> DeltaThree = new ArrayList<>();
	public static List<Double> DeltaFour = new ArrayList<>();
	public static List<List<Long>> timeArrOur = new ArrayList<>();
	public static List<List<Long>> timeArrAll = new ArrayList<>();

	public static void setPathErrValue(int globalIdx, double value){
		if(pathErr.size() == globalIdx){
			List<Double> cur = new ArrayList<>();
			cur.add(value);
			pathErr.add(cur);
		}
		else{
			pathErr.get(globalIdx).add(value);
		}
	}

	public static void setPathOurValue(int globalIdx, double value){
		if(pathOur.size() == globalIdx){
			List<Double> cur = new ArrayList<>();
			cur.add(value);
			pathOur.add(cur);
		}
		else{
			pathOur.get(globalIdx).add(value);
		}
	}

	public static void setPathAllValue(int globalIdx, double value){
		if(pathAll.size() == globalIdx){
			List<Double> cur = new ArrayList<>();
			cur.add(value);
			pathAll.add(cur);
		}
		else{
			pathAll.get(globalIdx).add(value);
		}
	}

	public static void setPathErrValueN(int globalIdx, double value){
		if(pathErr.size() == globalIdx){
			List<Double> cur = new ArrayList<>();
			cur.add(value);
			pathErr.add(cur);
		}
		else{
			pathErr.get(globalIdx).add(value);
		}

		// 更新计数器状态
		if(pathErr.get(globalIdx).size() == SystemParameters.TargetNum){
			flagBits.set(globalIdx, true);
			basketCnt ++;
		}
	}

	public static int computeErr(List<CondPath> our, List<LenwithProb> all, ProbRTA RTAtool, allProb Alltool) {
		int i = 0, j = 0, n = our.size(), m = all.size();
		int globalIndex = 0;
//		double dt = 0;

		while(i < n && j < m){
			CondPath curOur = our.get(i);
			LenwithProb curAll = all.get(j);
			long curLen = Math.max(curOur.getLength(), curAll.getLength());
			double value, valueAll, valueOur;

			if(curLen == curOur.getLength() && curLen == curAll.getLength()){
				value = curOur.getGlobalProb() - curAll.getProb();
				valueAll = curAll.getProb();
				valueOur = curOur.getGlobalProb();
//				if((globalIndex == 0 || globalIndex == 1) && Math.abs(value) > 0.001){
//					// int debug = 0;
//					System.out.println(
//							"fuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuck");
//					return -100;
//				}
				i ++;
				j ++;
			}
			else if (curLen == curOur.getLength()){
				value = curOur.getGlobalProb();
				valueAll = 0.0;
				valueOur = curOur.getGlobalProb();
				i ++;
			}
			else {
				value = -1.0 * curAll.getProb();
				valueAll = curAll.getProb();
				valueOur = 0.0;
				j ++;
			}
			if(globalIndex == 1 && value > 0.01){
				int debug = 0;
			}
			setPathErrValue(globalIndex, value);
			setPathOurValue(globalIndex, valueOur);
			setPathAllValue(globalIndex, valueAll);
			globalIndex ++;

//			if(globalIndex == 3){
//				DeltaThree.add(value);
//				dt = value;
//			}
//			else if(globalIndex == 4){
//				DeltaFour.add(value + dt);
//			}
			if(globalIndex >= SystemParameters.FigLimit){
				break;
			}
		}

		while(i < n){
			if(globalIndex >= SystemParameters.FigLimit){
				break;
			}
			double value = our.get(i).getGlobalProb();
			setPathErrValue(globalIndex, value);
			setPathOurValue(globalIndex, value);
			setPathAllValue(globalIndex, 0.0);
			i ++;
			globalIndex ++;
//			if(globalIndex == 3){
//				DeltaThree.add(value);
//				dt = value;
//			}
//			else if(globalIndex == 4){
//				DeltaFour.add(value + dt);
//			}
		}

		while(j < m){
			if(globalIndex >= SystemParameters.FigLimit){
				break;
			}
			double value = -1.0 * all.get(j).getProb();
			setPathErrValue(globalIndex, value);
			setPathAllValue(globalIndex, -1.0 * value);
			setPathOurValue(globalIndex, 0.0);
			j ++;
			globalIndex ++;
//			if(globalIndex == 3){
//				DeltaThree.add(value);
//				dt = value;
//			}
//			else if(globalIndex == 4){
//				DeltaFour.add(value + dt);
//			}
		}
		return globalIndex;
	}

	public static int computeErrN(List<CondPath> our, List<LenwithProb> all) {
		int i = 0, j = 0, n = our.size(), m = all.size();
		int globalIndex = 0;

		while(i < n && j < m){
			CondPath curOur = our.get(i);
			LenwithProb curAll = all.get(j);
			long curLen = Math.max(curOur.getLength(), curAll.getLength());
			double value;

			if(curLen == curOur.getLength() && curLen == curAll.getLength()){
				value = curOur.getGlobalProb() - curAll.getProb();
//				if((globalIndex == 0 || globalIndex == 1) && Math.abs(value) > 0.0001){
//					int debug = 0;
//				}
				i ++;
				j ++;
			}
			else if (curLen == curOur.getLength()){
				value = curOur.getGlobalProb();
				i ++;
			}
			else {
				value = -1.0 * curAll.getProb();
				j ++;
			}
			if(!flagBits.get(globalIndex)){
				setPathErrValueN(globalIndex, value);
			}
			globalIndex ++;

			if(globalIndex >= SystemParameters.FigLimit){
				break;
			}
		}

		while(i < n){
			if(globalIndex >= SystemParameters.FigLimit){
				break;
			}
			double value = our.get(i).getGlobalProb();
			if(!flagBits.get(globalIndex)){
				setPathErrValueN(globalIndex, value);
			}
			i ++;
			globalIndex ++;
		}

		while(j < m){
			if(globalIndex >= SystemParameters.FigLimit){
				break;
			}
			double value = -1.0 * all.get(j).getProb();
			if(!flagBits.get(globalIndex)){
				setPathErrValueN(globalIndex, value);
			}
			j ++;
			globalIndex ++;
		}
		return globalIndex;
	}

	public static void computeAvgErr(List<CondPath> our, List<LenwithProb> all) {
		int i = 0, j = 0, n = our.size(), m = all.size();
		int globalIndex = 0;

		while(i < n && j < m){
			CondPath curOur = our.get(i);
			LenwithProb curAll = all.get(j);
			long curLen = Math.max(curOur.getLength(), curAll.getLength());

			if(curLen == curOur.getLength() && curLen == curAll.getLength()){
				double value = Math.max(0.0, curOur.getGlobalProb() - curAll.getProb());// 非负
				setPathErrValue(globalIndex, value);
				i ++;
				j ++;
			}
			else if (curLen == curOur.getLength()){
				double value = curOur.getGlobalProb();
				setPathErrValue(globalIndex, value);
				i ++;
			}
			else {
//				double value = -1.0 * curAll.getProb();
//				setPathErrValue(globalIndex, value);
				j ++;
			}
			globalIndex ++;
		}

		while(i < n){
			double value = our.get(i).getGlobalProb();
			setPathErrValue(globalIndex, value);
			i ++;
			globalIndex ++;
		}

//		while(j < m){
//			double value = -1.0 * all.get(j).getProb();
//			setPathErrValue(globalIndex, value);
//			j ++;
//			globalIndex ++;
//		}
	}

	private static double calculate(List<Long> arr) {
		long sum = 0L;
		for (Long i : arr) sum += i;
		return (double) sum / nos;
	}

	private static double calculateDB(List<Double> arr) {
		double sum = 0.0;
		for (Double i : arr) sum += i;
		return sum / (double) arr.size();
	}


//	public static void compareMethodsForsingle(List<CondPath> our, List<LenwithProb> all, long timeo, long timea) {
//		int i = 0, j = 0, n = our.size(), m = all.size();
//		File file_our = new File("testOut/our.txt");
//		File file_all = new File("testOut/all.txt");
//		BufferedWriter writerO;
//		BufferedWriter writerA;
//		try {
//			writerO = new BufferedWriter(new FileWriter(file_our));
//			writerA = new BufferedWriter(new FileWriter(file_all));
//			writerO.write("Execution time:" + String.valueOf(timeo) + " ms");
//			writerO.newLine();
//			writerA.write("Execution time:" + String.valueOf(timea) + " ms");
//			writerA.newLine();
//			while(i < n && j < m){
//				CondPath curOur = our.get(i);
//				LenwithProb curAll = all.get(j);
//				long curLen = Math.max(curOur.getLength(), curAll.getLength());
//
//				if(curLen == curOur.getLength()){
//					writerO.write(String.valueOf(curLen) + " " + String.valueOf(curOur.getGlobalProb()));
//					i ++;
//				}
//				else{
//					writerO.write(String.valueOf(curLen) + " 0");
//				}
//				writerO.newLine();
//
//				if(curLen == curAll.getLength()){
//					writerA.write(String.valueOf(curLen) + " " + String.valueOf(curAll.getProb()));
//					j ++;
//				}
//				else{
//					writerA.write(String.valueOf(curLen) + " 0");
//				}
//				writerA.newLine();
//			}
//
//			while(i < n){
//				long curLen = our.get(i).getLength();
//				writerO.write(String.valueOf(curLen) + " " + String.valueOf(our.get(i).getGlobalProb()));
//				writerO.newLine();
//				writerA.write(String.valueOf(curLen) + " 0");
//				writerA.newLine();
//				i ++;
//			}
//
//			while(j < m){
//				long curLen = all.get(j).getLength();
//				writerA.write(String.valueOf(curLen) + " " + String.valueOf(all.get(j).getProb()));
//				writerA.newLine();
//				writerO.write(String.valueOf(curLen) + " 0");
//				writerO.newLine();
//				j ++;
//			}
//
//			writerO.close();
//			writerA.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		int debug = 0;
//	}

	public static void printDAGinfo(DirectedAcyclicGraph d) {
		// 1、normal nodes including conditional nodes(done)
		// 2、set of conditional nodes' index
		// 3、sub-dag of each conditional nodes(node index - path: new index)
		// number of branchs && successors
		// 4、normal edges except conditonal nodes to their successors(done)
		// todo in py(done): 增加conditional node到每个branch source的边 + 增加sink
		// node，并增加每个branch
		// sink的边 + 增加sink到successors的边
		List<Integer> condition = new ArrayList<>();
		List<Integer> allNodes = new ArrayList<>();
		List<List<Integer>> edges = new ArrayList<>();
		for (Node n : d.getFlatNodes()) {
			int curId = n.getId();
			allNodes.add(curId);// 1

			if (n.isCond) {
				condition.add(curId);// 2
				// number of branchs && successors set
				File file_con = new File("datavis/edges/" + String.valueOf(curId) + ".txt");
				BufferedWriter writer_con;
				try {
					// node id + successors id
					writer_con = new BufferedWriter(new FileWriter(file_con));

					// 第一行写 branch 个数
					writer_con.write(String.valueOf(n.nCond));
					writer_con.newLine();

					// 第二行写 child 的 id
					for (Node child : n.getChildren()) {
						writer_con.write(String.valueOf(child.getId()) + " ");
					}

					writer_con.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// sub-dag of each conditional nodes(node index - path: new index)
				for (int index = 0; index < n.nCond; index++) {
					File file = new File(
							"datavis/edges/" + String.valueOf(curId) + "-" + String.valueOf(index) + ".txt");
					BufferedWriter writer;
					try {
						writer = new BufferedWriter(new FileWriter(file));

						DirectedAcyclicGraph subdag = n.choosePathwithIndex(index);
						// 先写入该branch的节点数，方便后续创建id
						int num = subdag.getFlatNodes().size();
						writer.write(String.valueOf(num));
						writer.newLine();
						// 第二行开始写入边的关系
						for (Node subnode : subdag.getFlatNodes()) {
							for (Node child : subnode.getChildren()) {
								writer.write(subnode.getId() + " " + child.getId());
								writer.newLine();
							}
						}

						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else {
				for (Node child : n.getChildren()) {
					edges.add(Arrays.asList(curId, child.getId()));// 4
				}
			}
		}

		File all = new File("datavis/edges/" + "all.txt");
		BufferedWriter allwriter;
		try {
			allwriter = new BufferedWriter(new FileWriter(all));

			// 先写入所有的节点信息，第一行
			for (Integer i : allNodes) {
				allwriter.write(String.valueOf(i) + " ");
			}
			allwriter.newLine();
			// 再写入所有conditional nodes信息，第二行
			for (Integer i : condition) {
				allwriter.write(String.valueOf(i) + " ");
			}
			allwriter.newLine();
			// 最后写入所有边的信息
			for (List<Integer> i : edges) {
				allwriter.write(String.valueOf(i.get(0)) + " " + i.get(1));
				allwriter.newLine();
			}

			allwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// print WCET
		File WCET = new File("datavis/edges/" + "WCET.txt");
		BufferedWriter WCETwriter;
		try {
			WCETwriter = new BufferedWriter(new FileWriter(WCET));

			for (Node n : d.getFlatNodes()){
				if(!n.isCond){
					WCETwriter.write(n.getId() + " " + n.getWCET());
					WCETwriter.newLine();
				}
				else{
					for (int bran = 0; bran < n.nCond; bran ++){
						DirectedAcyclicGraph subDAG = n.choosePathwithIndex(bran);
						for (Node subNode : subDAG.getFlatNodes()){
							WCETwriter.write(n.getId() + "." + String.valueOf(bran) + "." + subNode.getId() + " " + subNode.getWCET());
							WCETwriter.newLine();
						}
					}
				}
			}
			WCETwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// print prob
		File prob = new File("datavis/edges/" + "prob.txt");
		BufferedWriter probwriter;
		try {
			probwriter = new BufferedWriter(new FileWriter(prob));

			for (Node n : d.getFlatNodes()){
				if(n.isCond){
					for(int i = 0; i < n.nCond; i ++){
						probwriter.write(n.getId() + " " + n.getId() + "." + String.valueOf(i) + ".0 " + String.valueOf(n.idx2prob.get(i)));
						probwriter.newLine();
					}
				}
			}
			probwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public static double pathExist(List<Node> conds, List<Integer> idx){
//		double prob = 1;
//		for (int i = 0; i < conds.size(); i ++){
//			Node cur = conds.get(i);
//			int branch = idx.get(i);
//			prob *= cur.idx2prob.getOrDefault(branch, 0.0);
//		}
//		return prob;
//	}

	/**
	 * This test case will generate two fixed DAG structure.
	 */
	public static OneSystemResults testOneCaseThreeMethod(Pair<List<DirectedAcyclicGraph>, CacheHierarchy> sys,
			int tasks, int[] NoInstances, int cores, int taskSeed, int tableSeed, int not) {

		boolean lcif = true;

		double cc_sens = 0;

		for (int k = 0; k < SystemParameters.cc_weights.length; k++) {
			cc_sens += SystemParameters.cc_weights[k];
		}

		for (DirectedAcyclicGraph d : sys.getFirst()) {
			for (Node n : d.getFlatNodes()) {
				n.sensitivity = 0;
				for (int k = 0; k < n.weights.length; k++) {
					n.sensitivity += n.weights[k] * SystemParameters.cc_weights[k] / cc_sens;
				}
			}
		}

		Simualtor sim1 = new Simualtor(SimuType.CLOCK_LEVEL, Hardware.PROC, Allocation.WORST_FIT,
				RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed, lcif);
		Pair<List<DirectedAcyclicGraph>, double[]> pair1 = sim1.simulate(print);

		SimualtorNWC sim2 = new SimualtorNWC(SimuType.CLOCK_LEVEL, Hardware.PROC, Allocation.CARVB,
				RecencyType.TIME_DEFAULT, sys.getFirst(), sys.getSecond(), cores, tableSeed, lcif);
		Pair<List<DirectedAcyclicGraph>, double[]> pair2 = sim2.simulate(print);

		// for (DirectedAcyclicGraph d : sys.getFirst()) {
		// for (Node n : d.getFlatNodes()) {
		// n.sensitivity = 0;
		// for (int k = 0; k < n.weights.length; k++) {
		// n.sensitivity += n.weights[k];
		// }
		// }
		// }
		//
		// SimualtorNWC cacheCASim = new SimualtorNWC(SimuType.CLOCK_LEVEL,
		// Hardware.PROC_CACHE,
		// Allocation.CACHE_AWARE_PREDICT_R, RecencyType.TIME_DEFAULT,
		// sys.getFirst(), sys.getSecond(), cores,
		// tableSeed, lcif);
		// Pair<List<DirectedAcyclicGraph>, double[]> pair2 =
		// cacheCASim.simulate(print);

		List<DirectedAcyclicGraph> m1 = pair1.getFirst();
		List<DirectedAcyclicGraph> m2 = pair2.getFirst();

		List<List<DirectedAcyclicGraph>> allMethods = new ArrayList<>();

		List<DirectedAcyclicGraph> method1 = new ArrayList<>();
		List<DirectedAcyclicGraph> method2 = new ArrayList<>();

		List<DirectedAcyclicGraph> dags = sys.getFirst();

		/*
		 * get a number of instances from each DAG based on long[] NoInstances.
		 */
		int count = 0;
		int currentID = -1;
		for (int i = 0; i < dags.size(); i++) {
			if (currentID != dags.get(i).id) {

				currentID = dags.get(i).id;
				count = 0;
			}

			if (count < NoInstances[dags.get(i).id]) {
				method1.add(m1.get(i));
				method2.add(m2.get(i));
				count++;
			}
		}

		allMethods.add(method1);
		allMethods.add(method2);

		List<double[]> cachePerformance = new ArrayList<>();
		cachePerformance.add(pair1.getSecond());
		cachePerformance.add(pair2.getSecond());

		OneSystemResults result = new OneSystemResults(allMethods, cachePerformance);

		return result;
	}

	public static int computeRTAErr(List<CondPath> our, List<LenwithProb> all, ProbRTA RTAtool, allProb Alltool) {
		int i = 0, j = 0, n = our.size(), m = all.size();
		int globalIndex = 0;
//		double dt = 0;

		while (i < n && j < m) {
			CondPath curOur = our.get(i);
			LenwithProb curAll = all.get(j);
			long curLen = Math.max(curOur.getLength(), curAll.getLength());
			double value, valueAll, valueOur;

			if (curLen == curOur.getLength() && curLen == curAll.getLength()) {
				value = curOur.getGlobalProb() - curAll.getProb();
				valueAll = curAll.getProb();
				valueOur = curOur.getGlobalProb();
//				if((globalIndex == 0 || globalIndex == 1) && Math.abs(value) > 0.001){
//					// int debug = 0;
//					System.out.println(
//							"fuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuckfuck");
//					return -100;
//				}
				i++;
				j++;
			} else if (curLen == curOur.getLength()) {
				value = curOur.getGlobalProb();
				valueAll = 0.0;
				valueOur = curOur.getGlobalProb();
				i++;
			} else {
				value = -1.0 * curAll.getProb();
				valueAll = curAll.getProb();
				valueOur = 0.0;
				j++;
			}
			if (globalIndex == 1 && value > 0.01) {
				int debug = 0;
			}
			setPathErrValue(globalIndex, value);
			setPathOurValue(globalIndex, valueOur);
			setPathAllValue(globalIndex, valueAll);
			globalIndex++;

//			if(globalIndex == 3){
//				DeltaThree.add(value);
//				dt = value;
//			}
//			else if(globalIndex == 4){
//				DeltaFour.add(value + dt);
//			}
			if (globalIndex >= SystemParameters.FigLimit) {
				break;
			}
		}

		while (i < n) {
			if (globalIndex >= SystemParameters.FigLimit) {
				break;
			}
			double value = our.get(i).getGlobalProb();
			setPathErrValue(globalIndex, value);
			setPathOurValue(globalIndex, value);
			setPathAllValue(globalIndex, 0.0);
			i++;
			globalIndex++;
//			if(globalIndex == 3){
//				DeltaThree.add(value);
//				dt = value;
//			}
//			else if(globalIndex == 4){
//				DeltaFour.add(value + dt);
//			}
		}

		while (j < m) {
			if (globalIndex >= SystemParameters.FigLimit) {
				break;
			}
			double value = -1.0 * all.get(j).getProb();
			setPathErrValue(globalIndex, value);
			setPathAllValue(globalIndex, -1.0 * value);
			setPathOurValue(globalIndex, 0.0);
			j++;
			globalIndex++;
//			if(globalIndex == 3){
//				DeltaThree.add(value);
//				dt = value;
//			}
//			else if(globalIndex == 4){
//				DeltaFour.add(value + dt);
//			}
		}
		return globalIndex;
	}
}

