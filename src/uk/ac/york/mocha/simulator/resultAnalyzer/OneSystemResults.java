package uk.ac.york.mocha.simulator.resultAnalyzer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.york.mocha.simulator.entity.DirectedAcyclicGraph;
import uk.ac.york.mocha.simulator.resultAnalyzer.AllSystemsResults.ResultType;

public class OneSystemResults {
	DecimalFormat df = new DecimalFormat("#.###");

	public List<List<DirectedAcyclicGraph>> allMethods;

	long[][] maxValues;

	public List<OneSystemByMethod> resultsPerMethod = new ArrayList<>();
	public List<OneSystemByMetric> resultsPerMetric = new ArrayList<>();

	List<String> resultsToString = new ArrayList<>();
	List<String> resultsCompareToString = new ArrayList<>();

	List<double[]> cachePerf;

	public OneSystemResults(List<List<DirectedAcyclicGraph>> allMethods, List<double[]> cachePerformance) {
		this.allMethods = new ArrayList<>(allMethods);
		this.cachePerf = cachePerformance;

		maxValues = getMaxValues(allMethods);

		for (int i = 0; i < allMethods.size(); i++) {
			resultsPerMethod.add(new OneSystemByMethod(allMethods.get(i), cachePerformance.get(i), maxValues));
		}

		for (int i = 0; i < ResultType.values().length; i++) {
			resultsPerMetric.add(new OneSystemByMetric(resultsPerMethod, i));
		}

		for (int i = 0; i < ResultType.values().length; i++) {
			String s = stringBuilding(resultsPerMetric.get(i).resultsPerMetric);
			resultsToString.add(s);

			String c = stringBuilding(resultsPerMetric.get(i).compare);
			resultsCompareToString.add(c);
		}
	}

	public long[][] getMaxValues(List<List<DirectedAcyclicGraph>> allMethods) {

		assert (allMethods.size() > 0);

		int resultTypeNum = ResultType.values().length;
		int taskNum = allMethods.get(0).stream().mapToInt(c -> c.id).max().getAsInt() + 1;

		long[][] maxValues = new long[resultTypeNum][taskNum];

		for (int i = 0; i < maxValues.length; i++) {
			for (int j = 0; j < maxValues[i].length; j++) {
				maxValues[i][j] = Long.MIN_VALUE;
			}
		}

		for (int i = 0; i < allMethods.get(0).size(); i++) {
			final int index = i;
			final int dagID = allMethods.get(0).get(i).id;

			for (int j = 0; j < resultTypeNum; j++) {
				switch (j) {
				case 0:
					List<Long> makespan = allMethods.stream().map(c -> c.get(index).finishTime - c.get(index).startTime)
							.collect(Collectors.toList());
					long makespanMax = makespan.stream().mapToLong(c -> c).max().getAsLong();

					if (maxValues[0][dagID] < makespanMax)
						maxValues[0][dagID] = makespanMax;

					break;
				case 1:
					List<Long> sumC = allMethods.stream().map(
							c -> c.get(index).getFlatNodes().stream().mapToLong(c1 -> c1.finishAt - c1.start).sum())
							.collect(Collectors.toList());
					long sumCMax = sumC.stream().mapToLong(c -> c).max().getAsLong();

					if (maxValues[1][dagID] < sumCMax)
						maxValues[1][dagID] = sumCMax;
					break;
				case 2:
					List<Long> duration = allMethods.stream()
							.map(c -> c.get(index).finishTime - c.get(index).releaseTime).collect(Collectors.toList());
					long durationMax = duration.stream().mapToLong(c -> c).max().getAsLong();

					if (maxValues[2][dagID] < durationMax)
						maxValues[2][dagID] = durationMax;

					break;

				default:
					break;
				}
			}

		}

		return maxValues;
	}

	public String stringBuilding(List<List<Double>> results) {

		StringBuilder builder = new StringBuilder();

		/*
		 * Add normalized durations to duration buffer.
		 */
		for (List<Double> l : results) {
			l.stream().forEach(c -> {
				builder.append(c + ",");
			});
			builder.append("\n");
		}

		return builder.toString();
	}

}
