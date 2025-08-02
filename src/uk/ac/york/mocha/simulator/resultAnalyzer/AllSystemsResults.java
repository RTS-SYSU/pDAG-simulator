package uk.ac.york.mocha.simulator.resultAnalyzer;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.util.Pair;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.parameters.SystemParameters;
import uk.ac.york.mocha.simulator.simulator.Utils;

public class AllSystemsResults {

	public enum ResultType {
		makespan, util, finish, makespan_abs
	}

	int taskNum;
	int systemNum;

	int[] instanceNo;

	List<String> data = new ArrayList<>();
	List<String> compareData = new ArrayList<>();

	List<String> dataAna = new ArrayList<>();
	List<String> compareDataAna = new ArrayList<>();

	String cachePerformance = "";

	String folder;

	DecimalFormat df = new DecimalFormat("#.###");

	String recencyName;

	String rate = "";
	String effect = "";

	public AllSystemsResults(List<OneSystemResults> resPerSystem, int[] instanceNo, int systemNum, int taskNum,
			SystemParameters.ExpName name, boolean append, double rate, double effect) {

		DecimalFormat df_temp = new DecimalFormat("#.#");
		this.rate = df_temp.format(rate) + "";
		this.effect = df_temp.format(effect) + "";

		this.taskNum = taskNum;
		this.systemNum = systemNum;
		this.instanceNo = instanceNo;
		this.recencyName = "";

		if (this.rate.equals(""))
			folder = "result/" + name + "/";
		else
			folder = "result/" + name + "_" + this.rate + "_" + this.effect + "/";
		
		File theDir = new File(folder);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		writeInstanceNum(append);
		writeResults(resPerSystem, append);
		writeCachePerf(resPerSystem, append);
		writeTaskParam(resPerSystem, append);

	}

	public AllSystemsResults(List<OneSystemResults> resPerSystem, int[] instanceNo, int systemNum, int taskNum,
			SystemParameters.ExpName name, boolean append) {
		this(resPerSystem, instanceNo, systemNum, taskNum, name, "", append);
	}

	public AllSystemsResults(List<OneSystemResults> resPerSystem, int[] instanceNo, int systemNum, int taskNum,
			SystemParameters.ExpName name) {
		this(resPerSystem, instanceNo, systemNum, taskNum, name, "", false);
	}

	public AllSystemsResults(List<OneSystemResults> resPerSystem, int[] instanceNo, int systemNum, int taskNum,
			SystemParameters.ExpName name, String recencyName) {
		this(resPerSystem, instanceNo, systemNum, taskNum, name, recencyName, false);
	}

	public AllSystemsResults(List<OneSystemResults> resPerSystem, int[] instanceNo, int systemNum, int taskNum,
			SystemParameters.ExpName name, String recencyName, boolean append) {
		this.taskNum = taskNum;
		this.systemNum = systemNum;
		this.instanceNo = instanceNo;
		this.recencyName = recencyName;

		if (rate.equals(""))
			folder = "result/" + name + "/";
		else
			folder = "result/" + name + "_" + rate + "_" + effect + "/";
		File theDir = new File(folder);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		writeInstanceNum(append);
		writeResults(resPerSystem, append);
		writeCachePerf(resPerSystem, append);
		writeTaskParam(resPerSystem, append);
	}

	public void writeTaskParam(List<OneSystemResults> resPerSystem, boolean append) {
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < resPerSystem.size(); i++) {
			List<DirectedAcyclicGraph> dags = resPerSystem.get(i).allMethods.get(0);

			String dagsInfo = "";
			for (int j = 0; j < 1; j++) {
				String dagInfo = dags.get(j).getSchedParameters().getWCET() + "," + dags.get(j).getSchedParameters().getPeriod() + ","
						+ df.format(dags.get(j).getSchedParameters().getUtil()) + "\n";
				dagsInfo += dagInfo;
				// if (j < dags.size() - 1)
				// dagsInfo += dagInfo + ",";
				// else
				// dagsInfo += dagInfo + "\n";
				// System.out.println(dags.get(j).getSchedParameters().getWCET()
				// );
			}
			builder.append(dagsInfo);
		}

		String fileName = "taskparam" + "_" + taskNum + "_" + SystemParameters.utilPerTask + recencyName + ".txt";
		Utils.writeResult(folder + fileName, builder.toString(), append);
	}

	public void writeCachePerf(List<OneSystemResults> resPerSystem, boolean append) {

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < resPerSystem.get(0).cachePerf.size(); i++) {
			final int methodIndex = i;

			for (int j = 0; j < resPerSystem.size(); j++) {

				double[] cacheOneMethod = resPerSystem.get(j).cachePerf.get(methodIndex);

				for (int k = 0; k < cacheOneMethod.length; k++) {
					builder.append(cacheOneMethod[k] + ",");
				}
				builder.append("\n");
			}
			builder.append("\n");

		}

		String cacheFileName = "cache" + "_" + taskNum + "_" + SystemParameters.utilPerTask + recencyName + ".txt";
		Utils.writeResult(folder + cacheFileName, builder.toString(), append);
	}

	public void writeResults(List<OneSystemResults> resPerSystem, boolean append) {
		for (int i = 0; i < ResultType.values().length; i++) {
			Pair<String, String> dataPair = dataPerSysToAllSys(resPerSystem, i, false);
			data.add(dataPair.getFirst());
			dataAna.add(dataPair.getSecond());

			Pair<String, String> dataComparePair = dataPerSysToAllSys(resPerSystem, i, true);
			compareData.add(dataComparePair.getFirst());
			compareDataAna.add(dataComparePair.getSecond());
		}

		for (int i = 0; i < ResultType.values().length; i++) {
			String dataFileName = ResultType.values()[i].name() + "_" + taskNum + "_" + SystemParameters.utilPerTask + recencyName + ".txt";
			Utils.writeResult(folder + dataFileName, data.get(i).toString(), append);

			String compareFileName = ResultType.values()[i].name() + "_compare" + "_" + taskNum + "_" + SystemParameters.utilPerTask
					+ recencyName + ".txt";
			Utils.writeResult(folder + compareFileName, compareData.get(i).toString(), append);

			String dataAnaFileName = "A_" + ResultType.values()[i].name() + "_" + taskNum + "_" + SystemParameters.utilPerTask + recencyName
					+ ".txt";
			Utils.writeResult(folder + dataAnaFileName, dataAna.get(i).toString(), append);

			String compareDataAnaFileName = "A_" + ResultType.values()[i].name() + "_compare" + "_" + taskNum + "_"
					+ SystemParameters.utilPerTask + recencyName + ".txt";
			Utils.writeResult(folder + compareDataAnaFileName, compareDataAna.get(i).toString(), append);
		}
	}

	public Pair<String, String> dataPerSysToAllSys(List<OneSystemResults> resPerSystem, int metricIndex, boolean compare) {

		List<List<String>> temp = new ArrayList<>();
		for (int i = 0; i < resPerSystem.size(); i++) {

			String[] oneMetric = null;

			if (!compare)
				oneMetric = resPerSystem.get(i).resultsToString.get(metricIndex).split("\n");
			else
				oneMetric = resPerSystem.get(i).resultsCompareToString.get(metricIndex).split("\n");

			List<String> d = new ArrayList<>();
			for (int j = 0; j < oneMetric.length; j++)
				d.add(oneMetric[j]);

			temp.add(d);
		}

		StringBuilder resByMetric = new StringBuilder();
		StringBuilder resAnalyse = new StringBuilder();

		for (int i = 0; i < temp.get(0).size(); i++) {
			final int index = i;

			temp.stream().forEach(c1 -> {
				resByMetric.append(c1.get(index) + "\n");
			});

			resByMetric.append("\n");
		}

		resAnalyse.append("\n\nData analysis for each instance \n");
		resAnalyse.append("AVG,MED,MAX,MIN\n");

		List<List<List<Double>>> analysedDataEachMethod = new ArrayList<>();

		for (int k = 0; k < temp.get(0).size(); k++) {
			List<List<Double>> summaryAll = new ArrayList<>();
			final int index = k;

			temp.stream().forEach(s -> {

				String c = s.get(index);
				String[] cs = c.split(",");

				List<Double> v = new ArrayList<>();
				for (int i = 0; i < cs.length; i++) {
					try {
						double d = Double.parseDouble(cs[i]);
						v.add(d);
					} catch (NullPointerException e) {
					} catch (NumberFormatException e) {
					}
				}

				double avg = v.stream().mapToDouble(c1 -> c1).sum() / (double) v.size();
				double max = v.stream().mapToDouble(c1 -> c1).max().getAsDouble();
				double min = v.stream().mapToDouble(c1 -> c1).min().getAsDouble();

				Median median = new Median();
				double[] v_d = new double[v.size()];
				for (int i = 0; i < v.size(); i++) {
					v_d[i] = v.get(i);
				}
				double med = median.evaluate(v_d);

				List<Double> summary = new ArrayList<>();
				summary.add(avg);
				summary.add(med);
				summary.add(max);
				summary.add(min);
				summaryAll.add(summary);

				resAnalyse.append(df.format(avg) + ",");
				resAnalyse.append(df.format(med) + ",");
				resAnalyse.append(df.format(max) + ",");
				resAnalyse.append(df.format(min) + ",\n");

			});

			resAnalyse.append("\n\n");
			analysedDataEachMethod.add(summaryAll);
		}

		resAnalyse.append("\n\nFurther Data analysis of all test cases \n");
		resAnalyse.append("avg med max min \n");

		for (int k = 0; k < analysedDataEachMethod.size(); k++) {
			List<List<Double>> summaryAll = analysedDataEachMethod.get(k);
			List<List<Double>> summartAllHtoV = new ArrayList<>();

			for (int j = 0; j < summaryAll.get(0).size(); j++) {
				List<Double> summary = new ArrayList<>();
				for (int i = 0; i < summaryAll.size(); i++) {
					summary.add(summaryAll.get(i).get(j));
				}
				summartAllHtoV.add(summary);
			}

			summartAllHtoV.forEach(v -> {

				Median median = new Median();
				double[] v_d = new double[v.size()];
				for (int i = 0; i < v.size(); i++) {
					v_d[i] = v.get(i);
				}

				double avg = v.stream().mapToDouble(c1 -> c1).sum() / (double) v.size();
				double med = median.evaluate(v_d);
				double max = v.stream().mapToDouble(c1 -> c1).max().getAsDouble();
				double min = v.stream().mapToDouble(c1 -> c1).min().getAsDouble();

				int count = summartAllHtoV.indexOf(v);

				switch (count) {
				case 0:
					resAnalyse.append("AVGs,");
					break;
				case 1:
					resAnalyse.append("MEDs,");
					break;
				case 2:
					resAnalyse.append("MAXs,");
					break;
				case 3:
					resAnalyse.append("MINs,");
					break;
				default:
					break;
				}

				resAnalyse.append(df.format(avg) + ",");
				resAnalyse.append(df.format(med) + ",");
				resAnalyse.append(df.format(max) + ",");
				resAnalyse.append(df.format(min) + ",\n");

			});
			resAnalyse.append("\n\n");
		}

		return new Pair<String, String>(resByMetric.toString(), resAnalyse.toString());
	}

	public void writeInstanceNum(boolean append) {
		String instanceNumString = "";
		for (int i = 0; i < instanceNo.length; i++) {
			if (i != instanceNo.length - 1)
				instanceNumString += instanceNo[i] + ",";
			else
				instanceNumString += instanceNo[i] + "\n";
		}

		Utils.writeResult(folder + "instanceNum_" + taskNum + "_" + SystemParameters.utilPerTask + recencyName + ".txt", instanceNumString,
				append);
	}

}
