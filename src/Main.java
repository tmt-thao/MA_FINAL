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

        Solution best = null;
        for (int i = 0; i < replications; i++) {
            MemeticAlgorithm ma = new MemeticAlgorithm(populationSize, generations, mutationRate, mutationNum, localSearchRate, localSearchNum);
            ma.run();
            Solution curr = ma.getBestSolution();

            if (best == null || (curr.getNumberOfTurnuses() < best.getNumberOfTurnuses() && curr.getFitness() < best.getFitness())) {
                best = curr;
            }
        }

        System.out.println("Best solution: " + best);
        System.out.println("\nTurnuses: " + best.getTurnuses().size());
        System.out.println("\nTrips: " + best.getUniqueTripsCount());
        System.out.println("\nVersion: " + version);

        try (FileWriter writer = new FileWriter(outputFilename)) {
            writer.write("Dataset: " + version + "\n");
            writer.write("\nTurnuses: " + best.getTurnuses().size() + "\n");
            writer.write("Trips: " + best.getUniqueTripsCount() + "\n");
            writer.write("\nSolution: " + best + "\n");
            
            
        }
    }

    public static void main(String[] args) throws IOException {
        runWithParams("T1_3", 10, 5, 500, 0.8, 10, 0.8, 10, "vystup.txt");
    }
}