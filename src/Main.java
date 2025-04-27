import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void runWithParams(String version, int replications, int populationSize, int generations,
                                     double mutationRate, int mutationNum, double localSearchRate, int localSearchNum,
                                     String outputFilename) throws IOException {

        DataLoader.loadStopIdToIndex("data/ZastavkyAll.csv");
        DataLoader.loadMatrixKm("data/matrixKm.txt", StaticData.stopIdToIndex.size());
        DataLoader.loadMatrixTime("data/matrixTime.txt", StaticData.stopIdToIndex.size());

        DataLoader.loadChargers("data/chargers_" + version + ".csv");
        DataLoader.loadChargingEvents("data/ChEvents_" + version + ".csv");
        DataLoader.loadTrips("data/spoje_id_" + version + ".csv");

        int[] turnuses = new int[replications];
        Solution best = null;
        double bestDuration = Double.MAX_VALUE;
        double avgTurnuses = 0.0;
        for (int i = 0; i < replications; i++) {
            MemeticAlgorithm ma = new MemeticAlgorithm(populationSize, generations, mutationRate, mutationNum, localSearchRate, localSearchNum, 1800);
            ma.run();
            Solution curr = ma.getBestSolution();
            double durationInSeconds = ma.getDurationInSeconds();
            turnuses[i] = curr.getNumberOfTurnuses();
            avgTurnuses += turnuses[i];

            if (best == null || (curr.getNumberOfTurnuses() < best.getNumberOfTurnuses() && curr.getFitness() < best.getFitness())) {
                best = curr;
                bestDuration = durationInSeconds;
            }
        }

        avgTurnuses /= replications;
        int bestCount = 0;
        for (int i = 0; i < turnuses.length; i++) {
            if (turnuses[i] == best.getNumberOfTurnuses()) {
                bestCount++;
            }
        }

        System.out.println("\nBest solution: " + best);

        System.out.println("\nPopulation size: " + populationSize);
        System.out.println("Generations: " + generations);
        System.out.println("Mutation rate: " + mutationRate);
        System.out.println("Local search rate: " + localSearchRate);

        System.out.println("\nSeason: " + StaticData.SEASON);
        System.out.println("Consumption per km: " + StaticData.CONSUMPTION_PER_KM);
        System.out.println("Max battery: " + StaticData.MAX_BATTERY);
        System.out.println("Charging strategy: " + StaticData.chargingStrategy);
        
        System.out.println("\nAvg turnuses: " + avgTurnuses);
        System.out.println("Best turnuses: " + best.getTurnuses().size());
        System.out.println("Best count: " + bestCount + "/" + replications);
        System.out.println("Trips: " + best.getUniqueTripsCount());
        System.out.println("Duration: " + bestDuration + " seconds");

        System.out.println("\nDataset: " + version);

        try (FileWriter writer = new FileWriter("results/" + outputFilename)) {
            writer.write("Dataset: " + version + "\n");
            
            writer.write("\nSeason: " + StaticData.SEASON + "\n");
            writer.write("Consumption per km: " + StaticData.CONSUMPTION_PER_KM + "\n");
            writer.write("Max battery: " + StaticData.MAX_BATTERY + "\n");
            writer.write("Charging strategy: " + StaticData.chargingStrategy + "\n");

            writer.write("\nPopulation size: " + populationSize + "\n");
            writer.write("Generations: " + generations + "\n");
            writer.write("Mutation rate: " + mutationRate + "\n");
            writer.write("Local search rate: " + localSearchRate + "\n");

            writer.write("\nAvg turnuses: " + avgTurnuses + "\n");
            writer.write("Best turnuses: " + best.getTurnuses().size() + "\n");
            writer.write("Best count: " + bestCount + "/" + replications + "\n");
            writer.write("Trips: " + best.getUniqueTripsCount() + "\n");
            writer.write("Duration: " + bestDuration + "\n");

            writer.write("\nBest solution: " + best + "\n");
        }
    }

    public static void main(String[] args) throws IOException {
        String[] versions = {
            "T1_3", "T2_3", "T3_3", "T4_3",
            "B1_3", "B2_3", "B3_3", "B4_3", "B5_3", "B6_3", "B7_3",
            "A_4"
        };
        
        // int[] gens = {100, 300, 500};
        // double[] mutRates = {0.2, 0.5, 0.8};
        // double[] locSearchRates = {0.2, 0.5, 0.8};
        // int[] popSizes = {5, 50, 100};

        int replications = 10;
        int popSize = 100;
        int gen = 500;
        double mutRate = 0.8;
        double locSearchRate = 0.8;
        StaticData.chargingStrategy = ChargingStrategy.WHEN_POSSIBLE;

        for (Season season : Season.values()) {
            StaticData.SEASON = season;
            StaticData.CONSUMPTION_PER_KM = season == Season.SPRING ? 1.5 : 2.0;
            StaticData.MAX_BATTERY = season == Season.WINTER ? 100.0 : 125.0;

            for (String version : versions) {
                runWithParams(version, replications, popSize, gen, mutRate, 10, 
                locSearchRate, 10, version + "_" + season + ".txt");
            }
        }
    }
}