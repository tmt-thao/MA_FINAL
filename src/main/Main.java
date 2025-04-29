package main;
import java.io.FileWriter;
import java.io.IOException;

import algorithm.MemeticAlgorithm;
import algorithm.Solution;
import data.DataLoader;
import data.StaticData;
import enums.ChargingStrategy;
import enums.Season;

public class Main {

    public static void runWithParams(String version, int replications, int populationSize, int generations,
                                     double mutationRate, int mutationNum, double localSearchRate, int localSearchNum,
                                     String outputFilename) throws IOException {

        DataLoader.loadStopIdToIndex("datasets/ZastavkyAll.csv");
        DataLoader.loadMatrixKm("datasets/matrixKm.txt", StaticData.stopIdToIndex.size());
        DataLoader.loadMatrixTime("datasets/matrixTime.txt", StaticData.stopIdToIndex.size());

        DataLoader.loadChargers("datasets/chargers_" + version + ".csv");
        DataLoader.loadChargingEvents("datasets/ChEvents_" + version + ".csv");
        DataLoader.loadTrips("datasets/spoje_id_" + version + ".csv");

        
        Solution bestSolution = null;
        double bestDuration = Double.MAX_VALUE;

        int[] turnuses = new int[replications];
        double avgTurnuses = 0.0;
        double avgDurations = 0.0;

        for (int i = 0; i < replications; i++) {
            MemeticAlgorithm ma = new MemeticAlgorithm(populationSize, generations, mutationRate, mutationNum, localSearchRate, localSearchNum, 1800);
            ma.run();

            Solution currSolution = ma.getBestSolution();
            double currDuration = ma.getDurationInSeconds();
            
            turnuses[i] = currSolution.getNumberOfTurnuses();
            avgTurnuses += turnuses[i];
            avgDurations += currDuration;

            if (bestSolution == null || (currSolution.getNumberOfTurnuses() < bestSolution.getNumberOfTurnuses() && currSolution.getFitness() < bestSolution.getFitness())) {
                bestSolution = currSolution;
                bestDuration = currDuration;
            }
        }

        avgDurations /= replications;
        avgTurnuses /= replications;
        int bestCount = 0;
        for (int i = 0; i < turnuses.length; i++) {
            if (turnuses[i] == bestSolution.getNumberOfTurnuses()) {
                bestCount++;
            }
        }

        System.out.println("\nBest solution: " + bestSolution);

        System.out.println("\nPopulation size: " + populationSize);
        System.out.println("Generations: " + generations);
        System.out.println("Mutation rate: " + mutationRate);
        System.out.println("Local search rate: " + localSearchRate);

        System.out.println("\nSeason: " + StaticData.SEASON);
        System.out.println("Consumption per km: " + StaticData.CONSUMPTION_PER_KM);
        System.out.println("Max battery: " + StaticData.MAX_BATTERY);
        System.out.println("Charging strategy: " + StaticData.CHARGING_STRATEGY);
        
        System.out.println("\nAvg turnuses: " + avgTurnuses);
        System.out.println("Avg duration: " + avgDurations + " seconds");
        System.out.println("Best turnuses: " + bestSolution.getTurnuses().size());
        System.out.println("Best duration: " + bestDuration + " seconds");
        System.out.println("Best count: " + bestCount + "/" + replications);
        System.out.println("Trips: " + bestSolution.getUniqueTripsCount());

        System.out.println("\nDataset: " + version);

        try (FileWriter writer = new FileWriter("results/" + outputFilename)) {
            writer.write("Dataset: " + version + "\n");
            
            writer.write("\nSeason: " + StaticData.SEASON + "\n");
            writer.write("Consumption per km: " + StaticData.CONSUMPTION_PER_KM + "\n");
            writer.write("Max battery: " + StaticData.MAX_BATTERY + "\n");
            writer.write("Charging strategy: " + StaticData.CHARGING_STRATEGY + "\n");

            writer.write("\nPopulation size: " + populationSize + "\n");
            writer.write("Generations: " + generations + "\n");
            writer.write("Mutation rate: " + mutationRate + "\n");
            writer.write("Local search rate: " + localSearchRate + "\n");

            writer.write("\nAvg turnuses: " + avgTurnuses + "\n");
            writer.write("Avg duration: " + avgDurations + " seconds\n");
            writer.write("Best turnuses: " + bestSolution.getTurnuses().size() + "\n");
            writer.write("Best duration: " + bestDuration + "\n");
            writer.write("Best count: " + bestCount + "/" + replications + "\n");
            writer.write("Trips: " + bestSolution.getUniqueTripsCount() + "\n");

            writer.write("\nBest solution: " + bestSolution + "\n");
        }
    }

    public static void main(String[] args) throws IOException {
        String version = "T1_3";

        int replications = 10;

        int popSize = 200;
        int gen = 500;
        double mutRate = 0.8;
        double locSearchRate = 0.8;


        runWithParams(version, replications, popSize, gen, mutRate, 10, locSearchRate, 10, "output.txt");
    }
}