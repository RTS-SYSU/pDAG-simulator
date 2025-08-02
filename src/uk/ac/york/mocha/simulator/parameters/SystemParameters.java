package uk.ac.york.mocha.simulator.parameters;

public class SystemParameters {

	public static final int etHist_length = 20;
	public static final int etHist_threshold = 20;
	public static final int etHist_start = 1;
	// public static final int instNo_x1 = 10;
	// public static final int instNo_x2 = 80;

	public static double[] d_weights = { 0.120, 0.097, 0.010, 0.014, 0.055, 0.069 };
	// public static double[] cc_weights = { 0.4559, 0.5999, 0.4010, 0.3558,
	// 0.4511, 0.4732 };
	// public static double[] cc_weights = { 0.6778, 0.6374, 0.5094, 0.5015,
	// 0.6460, 0.6055 };

	public static double[] cc_weights = { 0.6784, 0.6381, 0.5102, 0.5026, 0.6470, 0.6068 }; // two way
	// public static double[] cc_weights = { 0.4961, 0.4343, 0.2314, 0.2055,
	// 0.3454, 0.3642 }; // one way increase
	// public static double[] cc_weights = { 0.4038, 0.3123, 0.2837, 0.2844, 0.4532,
	// 0.4800 }; // one
	// way
	// decrease

	// Delay enabled
	// public static double[] cc_weights = { 0.2465, 0.3465, 0.1691, 0.1175,
	// 0.168, 0.1776 };

	// WFD + Top 10%
	// public static double[] cc_weights = { 0.3584, 0.5480, 0.2919, 0.1937,
	// 0.2778, 0.3029 }; // best

	// WFD + Normalised Ranking
	// public static double[] cc_weights = { 0.4949, 0.5186, 0.2906, 0.2161,
	// 0.3591, 0.3705 };

	// No Schedule effects included + flag
	// public static double[] cc_weights = { 0.536, 0.385, 0.086, 0.109, 0.101,
	// 0.096 };

	// public static double[] cc_weights = { 0.5199, 0.6343, 0.3617, 0.3283,
	// 0.3486, 0.4646 };

	// No Schedule effects included + normalization
	// public static double[] cc_weights = { 0.8241, 0.2973, 0.0643, 0.0897,
	// 0.1238, 0.1110 };

	// public static double[] d_weights = { 0.120, 0.097, 0, 0, 0.055, 0.069 };
	// public static double[] cc_weights = { 0.2465, 0.3465, 0, 0, 0.168, 0.1776
	// };

	/**********************************************************************
	 ********************** Simualtor Configuration ***********************
	 **********************************************************************/
	public enum SimuType {
		CLOCK_LEVEL, NODE_LEVEL
	};

	public enum Hardware {
		PROC, PROC_CACHE
	};

	public enum Allocation {
		SIMPLE, RANDOM, BEST_FIT, FIRST_FIT, WORST_FIT, WORST_FIT_OUR, CACHE_AWARE, CACHE_AWARE_NEW,
		CACHE_AWARE_ROBUST_v2_1, CACHE_AWARE_ROBUST_v2_2, OFFLINE_CACHE_AWARE, CACHE_AWARE_OUR, CACHE_AWARE_RESERVE,
		CACHE_AWARE_COMPARE, CARVB,
		CACHE_AWARE_PREDICT_WCET, WORST_FIT_NEW, WORST_FIT_NEW_BASE, FIXED_SCHEDULE_ALLOCATION_NEW, CACHE_AWARE_NEW_BASE

	};

	/**********************************************************************
	 *********************** Experimental Settings ************************
	 **********************************************************************/
	public enum ExpName {
		oneDAG, taskNum, recency_fault, recency_fault_util, recency_pattern, offline, offline_multi, methods, periods,
		tasks, sysUtil, sysUtilOneDAG, util_compare, util_compare_three, predict, predict_rule1, predict_rule2,
		error_crp,
		multi_pDAG_psr, multi_pDAG_para, multi_pDAG_cond,
		multi_pDAG_design_solution
	}

	public static int FigLimit = 2;// 5
	public static int NoS = 1000;
	public static final boolean printSim = false;
	public static final boolean printGen = false;

	/**********************************************************************
	 ************************** Number of cores ***************************
	 **********************************************************************/
	public static int coreNum = 4;

	/**********************************************************************
	 ************************** Cache Hierarchy ***************************
	 **********************************************************************/
	public static final int cacheLevel = 10;
	public static final int Level2CoreNum = 1;

	/**********************************************************************
	 **************************** DAG Structure ***************************
	 **********************************************************************/

	/* parameters for generic DAGs */
	public static final double connectProb = 0.2;
	public static final int minLayer = 5;// 5
	public static final int maxLayer = 6;// 10 6
	public static int minParal = 2;// 2
	public static int maxParal = 6;// 10 5

	/* parameters for conditional DAGs */
	public static final double CconnectProb = 0.1;
	public static final int CminLayer = 2;
	public static final int CmaxLayer = 4;
	public static int CminParal = 2;
	public static int CmaxParal = 4;

	/* boxplot params */
	public static int TargetNum = 500;
	public static boolean FixCondNum = true;

	public static boolean isRTA = true;
	public static boolean CompareSingle = false;
	public static boolean EXPthree = false;
	public static double AccepetProb = 1.0;

	/* parameters for NFJ DAGs */
	public static final int depth = 5;
	public static final double fork_prob = 0.3;
	public static final double join_prob = 0.8;
	public static final int fork_max = 2;
	public static final int fork_min = 4;
	public static final int fan_in = 3;

	/* Error Range */
	public static double err_median = 0.3;
	public static int err_range = 100;

	/* Recency fault rate */
	public static int fault_rate = 5;
	public static int fault_range = 10;
	public static int fault_median = 5;

	/**********************************************************************
	 **************************** Recency table ***************************
	 **********************************************************************/
	/*
	 * Recency Table Type
	 */
	public static enum RecencyType {
		REAL, ORDER, TIME_DEFAULT, TIME_CURVE, TIME_STEP
	};

	/*
	 * Recency Table by order
	 */
	public static final int[] costFactorMIN = { 30, 50, 60 };
	public static final int[] costFactorMAX = { 50, 60, 70 };
	public static final int[] recencyDepth = { 6, 20, 50 };

	/*
	 * Recency Table by time
	 */
	public static final long v1 = 0;
	public static final long v2 = 10000;
	public static final long v3 = 100000;
	public static final long v4 = 500000;

	public static final double delta1 = 0.3;
	public static final double delta2 = 0.5;
	public static final double delta3 = 0.8;
	public static final double delta4 = 1.0;

	/**********************************************************************
	 ************************** System Generator **************************
	 **********************************************************************/

	public final static int MAX_PRIORITY = 1000;

	public static double utilPerTask = 2.0;
	public static double utilforTask = 1.0;

	// probability that nodes are transferred to conditional
	public static double pCond = 0.4;
	// max branches
	public static int MAX_Cond = 3;
	// exactly numbers
	public static int MAX_CondNum = 3;

	/*
	 * Harmonic periods
	 */
	public final static int MAX_PERIOD = 144;
	public final static int MIN_PERIOD = 10;

	/*
	 * Non-harmonic periods
	 */
	public final static int minT = 100;
	public final static int maxT = 1000;

}
