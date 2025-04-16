// import java.io.BufferedWriter;
// import java.io.FileWriter;
// import java.io.IOException;

// public class Main {
//     public static void main(String[] args) throws IOException {
//         DataLoader.loadStopIdToIndex("data/ZastavkyAll.csv");
//         DataLoader.loadMatrixKm("data/matrixKm.txt", StaticData.stopIdToIndex.size());
//         DataLoader.loadMatrixTime("data/matrixTime.txt", StaticData.stopIdToIndex.size());
//         String[] versions = {
//         //    "T1_3", "T2_3", "T3_3", "T4_3", 
//         //    "B1_3", "B2_3", "B3_3", "B4_3", "B5_3", "B6_3", "B7_3", 
//             "A_4",
//             "T1_3"
//         };

//         int[] populationSizes = {
//             5, 
//             // 10, 
//             // 50, 
//             // 100
//         };
//         int[] generations = {
//             // 100,
//             // 500, 
//             1000, 
//             // 2000
//         };

//         double[] mutationRates = {
//             // 0.1, 
//             // 0.2, 
//             // 0.5, 
//             0.8
//         };
//         int[] mutationNumbers = {
//             5, 
//             // 10, 
//             // 15, 
//             // 20
//         };

//         double[] localSearchRates = {
//             // 0.1, 
//             // 0.2, 
//             // 0.5, 
//             0.8
//         };
//         int[] localSearchNumbers = {
//             5, 
//             // 10, 
//             // 15, 
//             // 20
//         };

//         for (String version : versions) {
//             // Dynamicky vytvorte názov súboru pre aktuálnu verziu
//             String fileName = "res_" + version + ".txt";

//             // Pre každú verziu vytvorte nový súbor a prepisujte jeho obsah
//             try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
//                 DataLoader.loadChargingEvents("data/ChEvents_" + version + ".csv");
//                 DataLoader.loadTrips("data/spoje_id_" + version + ".csv");

//                 Solution overallBestSolution = null;

//                 for (int populationSize : populationSizes) {
//                     for (int generation : generations) {
//                         for (double mutationRate : mutationRates) {
//                             for (int mutationNumber : mutationNumbers) {
//                                 for (double localSearchRate : localSearchRates) {
//                                     for (int localSearchNumber : localSearchNumbers) {
//                                         Solution bestSolutionForCombination = null;

//                                         for (int i = 0; i < 1; i++) {
//                                             MemeticAlgorithm ma = new MemeticAlgorithm(populationSize, generation, mutationRate, mutationNumber, localSearchRate, localSearchNumber);
//                                             ma.run();

//                                             Solution currentSolution = ma.getBestSolution();
//                                             if (bestSolutionForCombination == null || currentSolution.getFitness() < bestSolutionForCombination.getFitness()) {
//                                                 bestSolutionForCombination = currentSolution;
//                                             }
//                                         }

//                                         // Zapíšte najlepší výsledok pre túto kombináciu
//                                         writer.write("Population Size: " + populationSize + ", Generations: " + generation +
//                                                 ", Mutation Rate: " + mutationRate + ", Mutation Number: " + mutationNumber +
//                                                 ", Local Search Rate: " + localSearchRate + ", Local Search Number: " + localSearchNumber + "\n");
//                                         writer.write("Best Solution: " + bestSolutionForCombination.getNumberOfTurnuses() +
//                                                 ", Unique Trips Count: " + bestSolutionForCombination.getUniqueTripsCount() +
//                                                 ", All Trips Count: " + bestSolutionForCombination.getAllTripsCount() + "\n\n");

//                                         // Aktualizujte celkovo najlepší výsledok
//                                         if (overallBestSolution == null || bestSolutionForCombination.getFitness() < overallBestSolution.getFitness()) {
//                                             overallBestSolution = bestSolutionForCombination;
//                                         }
//                                     }
//                                 }
//                             }
//                         }
//                     }
//                 }

//                 // Zapíšte celkovo najlepší výsledok na koniec súboru
//                 writer.write("Overall Best Solution:\n");
//                 writer.write("Best Solution: " + overallBestSolution.getNumberOfTurnuses() +
//                         ", Unique Trips Count: " + overallBestSolution.getUniqueTripsCount() +
//                         ", All Trips Count: " + overallBestSolution.getAllTripsCount() + "\n");
//                 writer.write("Solution: " + overallBestSolution + "\n");
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
//         }
//     }
// }


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        // 140, 0.8
        // T:   5,    10,     12,     29
        // B:   4,    4,      5,      8,      9,      13,     30
        // A:   62

        // 140, 1.08
        // T:   5,    10,     12,     34
        // B:   4,    4,      6,      8,      10,      15,     37
        // A:   89

        // 105, 1.08
        // T:   5,    11,     13,     36
        // B:   4,    5,      6,      9,      11,      17,     46
        // A:   105

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        DataLoader.loadStopIdToIndex("data/ZastavkyAll.csv");
        DataLoader.loadMatrixKm("data/matrixKm.txt", StaticData.stopIdToIndex.size());
        DataLoader.loadMatrixTime("data/matrixTime.txt", StaticData.stopIdToIndex.size());

        String version = "T4_3";
        DataLoader.loadChargingEvents("data/ChEvents_" + version + ".csv");
        DataLoader.loadTrips("data/spoje_id_" + version + ".csv");

        MemeticAlgorithm ma = new MemeticAlgorithm(5, 1000, 0.8, 10, 0.8, 10);
        ma.run();
        // 5, 1000, 0.8, 10, 0.8, 10
        // JAR
        // T:   5,    10,     13,     31
        // B:   4,    4,      5,      8,     9,      16,    41
        // A:   81
    }
}
