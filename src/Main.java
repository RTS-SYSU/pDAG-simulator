import uk.ac.york.mocha.simulator.experiments_pDAG.ComputationCostAnalysis;
import uk.ac.york.mocha.simulator.experiments_pDAG.DesignSolutionAnalysis;
import uk.ac.york.mocha.simulator.experiments_pDAG.DeviationAnalysis;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Main [deviation|cost|design] ...");
            return;
        }

        String cmd = args[0].toLowerCase();
        try {
            switch (cmd) {
                case "deviation":
                    if (args.length == 1) {
                        DeviationAnalysis.run();
                    } else if (args.length == 2) {
                        switch (args[1].toLowerCase()) {
                            case "psr":
                                DeviationAnalysis.runPsr(0.2, 0.8);
                                break;
                            case "para":
                                DeviationAnalysis.runPara(3, 9);
                                break;
                            case "cond":
                                DeviationAnalysis.runCond(2, 10);
                                break;
                            default:
                                System.out.println("Usage: deviation [psr|para|cond] [start end]");
                        }
                    } else if (args.length == 4) {
                        switch (args[1].toLowerCase()) {
                            case "psr":
                                DeviationAnalysis.runPsr(Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                                break;
                            case "para":
                                DeviationAnalysis.runPara(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                                break;
                            case "cond":
                                DeviationAnalysis.runCond(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                                break;
                        }
                    } else {
                        System.out.println("Usage: deviation [psr|para|cond] [start end]");
                    }
                    break;

                case "cost":
                    if (args.length == 1) {
                        ComputationCostAnalysis.run();
                    } else if (args.length == 3) {
                        ComputationCostAnalysis.run(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                    } else {
                        System.out.println("Usage: cost [start end]");
                    }
                    break;

                case "design":
                    if (args.length == 1) {
                        DesignSolutionAnalysis.run();
                    } else if (args.length == 3) {
                        DesignSolutionAnalysis.run(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                    } else {
                        System.out.println("Usage: design [start end]");
                    }
                    break;

                default:
                    System.out.println("Unknown command: " + cmd);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}