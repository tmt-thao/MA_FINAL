import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void runWithParams(String version, int replications, int populationSize, int generations,
                                     double mutationRate, int mutationNum, double localSearchRate, int localSearchNum,
                                     String outputFilename) throws IOException {

        DataLoader.loadStopIdToIndex("data/ZastavkyAll.csv");
        DataLoader.loadMatrixKm("data/matrixKm.txt", StaticData.stopIdToIndex.size());
        DataLoader.loadMatrixTime("data/matrixTime.txt", StaticData.stopIdToIndex.size());

        DataLoader.loadChargingEvents("data/ChEvents_" + version + ".csv");
        DataLoader.loadTrips("data/spoje_id_" + version + ".csv");

        int[] turnuses = new int[replications];
        Solution best = null;
        for (int i = 0; i < replications; i++) {
            MemeticAlgorithm ma = new MemeticAlgorithm(populationSize, generations, mutationRate, mutationNum, localSearchRate, localSearchNum);
            ma.run();
            Solution curr = ma.getBestSolution();
            turnuses[i] = curr.getNumberOfTurnuses();

            if (best == null || (curr.getNumberOfTurnuses() < best.getNumberOfTurnuses() && curr.getFitness() < best.getFitness())) {
                best = curr;
            }
        }

        int bestCount = 0;
        for (int i = 0; i < turnuses.length; i++) {
            if (turnuses[i] == best.getNumberOfTurnuses()) {
                bestCount++;
            }
        }

        System.out.println("\nBest solution: " + best);
        System.out.println("Best turnuses: " + best.getTurnuses().size());
        System.out.println("Best count: " + bestCount + "/" + replications);
        System.out.println("Trips: " + best.getUniqueTripsCount());
        System.out.println("Version: " + version);
        System.out.println();

        try (FileWriter writer = new FileWriter(outputFilename)) {
            writer.write("Dataset: " + version + "\n");
            writer.write("\nTurnuses: " + best.getTurnuses().size() + "\n");
            writer.write("Best count: " + bestCount + "/" + replications + "\n");
            writer.write("Trips: " + best.getUniqueTripsCount() + "\n");
            writer.write("\nSolution: " + best + "\n");
            
            
        }
    }

    public static void main(String[] args) throws IOException {
        String[] versions = {"B7_3", "A_4"};
        // int[] popSizes = {5, 50, 100};
        int[] gens = {100, 300, 500};
        double[] mutRates = {0.2, 0.5, 0.8};
        double[] locSearchRates = {0.2, 0.5, 0.8};

        int popSize = 5;
        //int gen = 500;
        double mutRate = 0.8;
        double locSearchRate = 0.8;
        for (String version : versions) {
            for (int gen : gens) {
                runWithParams(version, 10, popSize, gen, mutRate, 10, locSearchRate, 10, 
                version + "_" + popSize + "_" + gen + "_" + mutRate + "_" + locSearchRate + ".txt");
            }
        }

        //runWithParams("T1_3", 5, 5, 500, 0.8, 10, 0.8, 10, "vystup.txt");
    }
}