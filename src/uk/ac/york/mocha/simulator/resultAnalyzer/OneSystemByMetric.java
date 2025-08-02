package uk.ac.york.mocha.simulator.resultAnalyzer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class OneSystemByMetric {

	private DecimalFormat df = new DecimalFormat("#.###");

	List<List<Double>> resultsPerMetric = new ArrayList<>();

	public List<List<Double>> compare = new ArrayList<>();

	private int metricIndex;

	public OneSystemByMetric(List<OneSystemByMethod> valuePerMethod, int metricIndex) {
		this.metricIndex = metricIndex;
		getResults(valuePerMethod);
	}

	public void getResults(List<OneSystemByMethod> resultByMethod) {

		for (int j = 0; j < resultByMethod.size(); j++) {
			List<List<Double>> m = resultByMethod.get(j).resultsPerMethod;
			resultsPerMetric.add(m.get(metricIndex));
		}

		for (int j = 0; j < resultsPerMetric.size() - 1; j++) {
			List<Double> m1 = resultsPerMetric.get(j);
			List<Double> m2 = resultsPerMetric.get(j + 1);

			List<Double> compareTwoMethod = new ArrayList<>();
			for (int k = 0; k < m1.size(); k++) {

				if (m1.get(k) >= m2.get(k)) {
					double reducePercent = ((double) (m1.get(k) - m2.get(k))) / (double) m1.get(k);
					compareTwoMethod.add(Double.parseDouble(df.format(reducePercent)));
				} else {
					double reducePercent = -((double) (m2.get(k) - m1.get(k))) / (double) m2.get(k);
					compareTwoMethod.add(Double.parseDouble(df.format(reducePercent)));
				}

			}
			compare.add(compareTwoMethod);
		}

	}

}
