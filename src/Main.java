import uk.ac.york.mocha.simulator.experiments_pDAG.ComputationCostAnalysis;
import uk.ac.york.mocha.simulator.experiments_pDAG.DesignSolutionAnalysis;
import uk.ac.york.mocha.simulator.experiments_pDAG.DeviationAnalysis;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Java container started. Awaiting commands...");

        while (true) {
            System.out.print(">> ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Shutting down...");
                break;
            }
            String[] parts = line.split("\\s+");
            try {
                switch (parts[0].toLowerCase()) {
                    case "deviation":{
                        if (parts.length == 1){
                            DeviationAnalysis.run();
                        }
                        if (parts.length == 2){
                            switch (parts[1].toLowerCase()){
                                case "psr":
                                    DeviationAnalysis.runPsr(0.2, 0.8);
                                    break;
                                case "para":
                                    DeviationAnalysis.runPara(3, 9);
                                    break;
                                case "cond":
                                    DeviationAnalysis.runCond(2, 7);
                                default:
                                    System.out.println("Usage: deviation [psr | para | cond] [start end]");
                                    break;
                            }
                        }
                        else if (parts.length == 4){
                            switch (parts[1].toLowerCase()){
                                case "psr":{
                                    double startPsr = Double.parseDouble(parts[2]);
                                    double endPsr = Double.parseDouble(parts[3]);
                                    DeviationAnalysis.runPsr(startPsr,endPsr);
                                    break;
                                }

                                case "para":{
                                    int startPara = Integer.parseInt(parts[2]);
                                    int endPara = Integer.parseInt(parts[3]);
                                    DeviationAnalysis.runPara(startPara, endPara);
                                    break;
                                }

                                case "cond":{
                                    int startCond = Integer.parseInt(parts[2]);
                                    int endCond = Integer.parseInt(parts[3]);
                                    DeviationAnalysis.runCond(startCond, endCond);
                                    break;
                                }

                                default:
                                    System.out.println("Usage: deviation [psr | para | cond] [start end]");
                                    break;
                            }
                        }
                        else{
                            System.out.println("Usage: deviation [psr | para | cond] [start end]");
                        }
                        break;
                    }
                    case "cost":{
                        if (parts.length == 1){
                            ComputationCostAnalysis.run();
                        }
                        else if(parts.length == 3){
                            int startCond = Integer.parseInt(parts[1]);
                            int endCond = Integer.parseInt(parts[2]);
                            ComputationCostAnalysis.run(startCond, endCond);
                        }else{
                            System.out.println("Usage: cost [start end]");
                        }
                        break;
                    }

                    case "design":
                        DesignSolutionAnalysis.run();
                        break;
                    default:
                        System.out.println("Unknown command: " + parts[0]);
                }
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        }

        scanner.close();
    }
}