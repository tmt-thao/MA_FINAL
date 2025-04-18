// import java.io.IOException;

// public class Main {
//     public static void main(String[] args) throws IOException {

//         // 140, 0.8
//         // JAR
//         // T:   5,    10,     12,     29
//         // B:   4,    4,      5,      8,      9,      13,     30
//         // A:   62

//         // 140, 1.08
//         // LETO
//         // T:   5,    10,     12,     34
//         // B:   4,    4,      6,      8,      10,      15,     37
//         // A:   89

//         // 105, 1.08
//         // ZIMA
//         // T:   5,    11,     13,     36
//         // B:   4,    5,      6,      9,      11,      17,     46
//         // A:   105

//         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//         DataLoader.loadStopIdToIndex("data/ZastavkyAll.csv");
//         DataLoader.loadMatrixKm("data/matrixKm.txt", StaticData.stopIdToIndex.size());
//         DataLoader.loadMatrixTime("data/matrixTime.txt", StaticData.stopIdToIndex.size());

//         String version = "T2_3";
//         DataLoader.loadChargingEvents("data/ChEvents_" + version + ".csv");
//         DataLoader.loadTrips("data/spoje_id_" + version + ".csv");

//         int replications = 10;
//         int populationSize = 5;
//         int generations = 500;
//         double mutationRate = 0.8;
//         int mutationNum = 10;
//         double localSearchRate = 0.8;
//         int localSearchNum = 10;

//         Solution best = null;
//         for (int i = 0; i < replications; i++) {
//             MemeticAlgorithm ma = new MemeticAlgorithm(populationSize, generations, mutationRate, mutationNum, localSearchRate, localSearchNum);
//             ma.run();
//             Solution curr = ma.getBestSolution();

//             if (best == null || (curr.getNumberOfTurnuses() < best.getNumberOfTurnuses() && curr.getFitness() < best.getFitness())) {
//                 best = curr;
//             }
//         }

//         System.out.println("Best solution: " + best);
//         System.out.println("\nTurnuses: " + best.getTurnuses().size());
//         System.out.println("\nTrips: " + best.getUniqueTripsCount());
//         System.out.println("\nVersion: " + version);

//         // 5, 500, 0.8, 10, 0.8, 10
//         // JAR
//         // T:   5,    10,     13,     34
//         // B:   4,    4,      5,      8,     10,      17,    47
//         // A:   93
//     }
// }

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

        // Výstup do konzoly
        System.out.println("Best solution: " + best);
        System.out.println("\nTurnuses: " + best.getTurnuses().size());
        System.out.println("\nTrips: " + best.getUniqueTripsCount());
        System.out.println("\nVersion: " + version);

        // Výstup do súboru
        try (FileWriter writer = new FileWriter(outputFilename)) {
            writer.write("Version: " + version + "\n");
            writer.write("Turnuses: " + best.getTurnuses().size() + "\n");
            writer.write("Trips: " + best.getUniqueTripsCount() + "\n");
            writer.write("Solution: " + best + "\n");
            
            
        }
    }

    // Pôvodný main ostáva ak chceš spúšťať aj manuálne
    public static void main(String[] args) throws IOException {
        runWithParams("T2_3", 10, 5, 500, 0.8, 10, 0.8, 10, "vystup.txt");
    }
}
