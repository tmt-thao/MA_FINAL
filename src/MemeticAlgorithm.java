import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MemeticAlgorithm {
    private int populationSize;
    private int generations;

    private double mutationRate;
    private int mutationNum;
    private double localSearchRate;
    private int localSearchNum;

    private List<Solution> population;
    private Solution bestSolution;
    private Random random;

    public MemeticAlgorithm(int populationSize, int generations, double mutationRate, int mutationNum,
            double localSearchRate, int localSearchNum) {
        this.populationSize = populationSize;
        this.generations = generations;

        this.mutationRate = mutationRate;
        this.mutationNum = mutationNum;
        this.localSearchRate = localSearchRate;
        this.localSearchNum = localSearchNum;

        this.population = new ArrayList<>();
        this.bestSolution = null;
        this.random = new Random();
    }

    public Solution getBestSolution() {
        return bestSolution;
    }

    public void run() {
        initializePopulation();

        for (int generation = 0; generation < generations; generation++) {
            List<Solution> newPopulation = new ArrayList<>();
            newPopulation.add(bestSolution);

            while (newPopulation.size() < populationSize) {
                Solution parent1 = selectParent();
                Solution parent2 = selectParent();
                Solution child = crossover(parent1, parent2);
                evaluatePopulation(child);

                child = mutate(child);
                child = localSearch(child);

                newPopulation.add(child);
            }

            population = newPopulation;
            // System.out.println("Generation " + String.format("%4d", generation + 1) 
            //         + " - best turnuses: " + String.format("%4d", bestSolution.getTurnuses().size())
            //         + ", trips unique: " + bestSolution.getUniqueTripsCount()
            //         + ", trips all: " + bestSolution.getAllTripsCount());
        }

        // System.out.println("\nBest solution: " + bestSolution);
        // System.out.println("\nFinal best turnuses: " + bestSolution.getTurnuses().size() 
        //         + ", trips unique: " + bestSolution.getUniqueTripsCount()
        //         + ", trips all: " + bestSolution.getAllTripsCount());
    }

    private void initializePopulation() {
        population.add(Solution.generate(new ArrayList<>(StaticData.trips)));
        this.bestSolution = new Solution(population.get(0));

        while (population.size() < populationSize) {
            List<Trip> trips = new ArrayList<>(StaticData.trips);
            Collections.shuffle(trips, random);
            Solution solution = Solution.generate(trips);

            population.add(solution);
            evaluatePopulation(solution);
        }

        // System.out.println("Initial best solution turnuses count: " + bestSolution.getTurnuses().size()
        //         + ", trips unique: " + bestSolution.getUniqueTripsCount()
        //         + ", trips all: " + bestSolution.getAllTripsCount() + "\n");
    }

    private void evaluatePopulation(Solution solution) {
        if (solution.getNumberOfTurnuses() < bestSolution.getNumberOfTurnuses() && solution.getFitness() < bestSolution.getFitness()) {
            bestSolution = new Solution(solution);
        }
    }

    private Solution selectParent() {
        List<Double> weights = new ArrayList<>();
        double total = 0.0;

        for (Solution solution : population) {
            double weight = 1.0 / solution.getFitness();
            weights.add(weight);
            total += weight;
        }

        List<Double> cumulative = new ArrayList<>();
        double cumulativeSum = 0.0;
        for (double weight : weights) {
            cumulativeSum += weight / total;
            cumulative.add(cumulativeSum);
        }

        double r = random.nextDouble();
        for (int i = 0; i < cumulative.size(); i++) {
            if (r <= cumulative.get(i)) {
                return new Solution(population.get(i));
            }
        }

        return new Solution(population.get(random.nextInt(population.size())));
    }

    private Solution crossover(Solution parent1, Solution parent2) {
        Solution child = new Solution();

        Turnus turnus1 = parent1.getTurnuses().get(random.nextInt(parent1.getTurnuses().size()));
        Turnus turnus2 = parent2.getTurnuses().get(random.nextInt(parent2.getTurnuses().size()));

        List<Trip> trips1 = new ArrayList<>(turnus1.getTrips());
        trips1.remove(0);
        trips1.remove(trips1.size() - 1);

        List<Trip> trips2 = new ArrayList<>(turnus2.getTrips());
        trips2.remove(0);
        trips2.remove(trips2.size() - 1);

        List<Trip> duplicatedTrips = new ArrayList<>(trips1);
        for (Trip trip1 : trips1) {
            for (Trip trip2 : trips2) {
                if (trip1.getId() == trip2.getId()) {
                    duplicatedTrips.remove(trip1);
                    break;
                }
            }
        }

        List<Trip> missingTrips = new ArrayList<>(trips2);
        for (Trip trip2 : trips2) {
            for (Trip trip1 : trips1) {
                if (trip1.getId() == trip2.getId()) {
                    missingTrips.remove(trip2);
                    break;
                }
            }
        }

        List<Trip> parent2Trips = new ArrayList<>();
        for (Turnus turnus : parent2.getTurnuses()) {
            if (turnus != turnus2) {
                List<Trip> trips = new ArrayList<>(turnus.getTrips());
                trips.remove(0);
                trips.remove(trips.size() - 1);
                parent2Trips.addAll(trips);
            }
        }

        List<Trip> toBeAddedTrips = new ArrayList<>(parent2Trips);
        for (Trip trip : duplicatedTrips) {
            for (Trip trip2 : parent2Trips) {
                if (trip.getId() == trip2.getId()) {
                    toBeAddedTrips.remove(trip2);
                    break;
                }
            }
        }
        toBeAddedTrips.addAll(missingTrips);

        child.addTurnus(new Turnus(turnus1));
        child.addTrips(toBeAddedTrips);
        return child;
    }

    private Solution mutate(Solution solution) {
        Solution best = new Solution(solution);

        for (int i = 0; i < mutationNum; i++) {
            if (random.nextDouble() > mutationRate) {
                continue;
            }

            Solution mutated = new Solution();

            Turnus turnus1 = solution.getTurnuses().get(random.nextInt(solution.getTurnuses().size()));
            while (turnus1.getTrips().size() < 3) {
                turnus1 = solution.getTurnuses().get(random.nextInt(solution.getTurnuses().size()));
            }
            Turnus turnus2 = solution.getTurnuses().get(random.nextInt(solution.getTurnuses().size()));
            while (turnus1 == turnus2 || turnus2.getTrips().size() < 3) {
                turnus2 = solution.getTurnuses().get(random.nextInt(solution.getTurnuses().size()));
            }

            for (Turnus turnus : solution.getTurnuses()) {
                if (turnus != turnus1 && turnus != turnus2) {
                    mutated.addTurnus(new Turnus(turnus));
                }
            }

            List<Trip> trips1 = new ArrayList<>(turnus1.getTrips());
            trips1.remove(0);
            trips1.remove(trips1.size() - 1);

            List<Trip> trips2 = new ArrayList<>(turnus2.getTrips());
            trips2.remove(0);
            trips2.remove(trips2.size() - 1);

            int from = random.nextInt(trips1.size());
            int length = random.nextInt(trips1.size() - from) + 1;
            List<Trip> seq1 = trips1.subList(from, from + length);
            Trip seq1FirstTrip = seq1.get(0);
            Trip seq1LastTrip = seq1.get(seq1.size() - 1);

            List<Trip> seq2 = new ArrayList<>();
            for (Trip trip2 : trips2) {
                if (trip2.getStartTime() >= seq1FirstTrip.getStartTime() && trip2.getEndTime() <= seq1LastTrip.getEndTime()) {
                    seq2.add(trip2);
                } else if (trip2.getStartTime() > seq1LastTrip.getEndTime()) {
                    break;
                }
            }

            List<Trip> toBeAddedTrips1 = new ArrayList<>();
            toBeAddedTrips1.addAll(trips1);
            toBeAddedTrips1.removeAll(seq1);
            toBeAddedTrips1.addAll(seq2);
            toBeAddedTrips1.sort(Comparator.comparing(Trip::getStartTime));

            List<Trip> notAdded = new ArrayList<>();
            Turnus newTurnus1 = new Turnus();
            for (Trip trip : toBeAddedTrips1) {
                if (!newTurnus1.addTrip(trip, mutated.getFreeChargers())) {
                    notAdded.add(trip);
                }
            }
            mutated.addTurnus(newTurnus1);

            List<Trip> toBeAddedTrips2 = new ArrayList<>();
            toBeAddedTrips2.addAll(trips2);
            toBeAddedTrips2.removeAll(seq2);
            toBeAddedTrips2.addAll(seq1);
            toBeAddedTrips2.sort(Comparator.comparing(Trip::getStartTime));

            Turnus newTurnus2 = new Turnus();
            for (Trip trip : toBeAddedTrips2) {
                if (!newTurnus2.addTrip(trip, mutated.getFreeChargers())) {
                    notAdded.add(trip);
                }
            }
            mutated.addTurnus(newTurnus2);
            mutated.addTrips(notAdded);

            if (mutated.getFitness() < best.getFitness()) {
                best = new Solution(mutated);
                evaluatePopulation(best);
            }
        }

        return best;
    }

    private Solution localSearch(Solution solution) {
        Solution best = new Solution(solution);

        for (int i = 0; i < localSearchNum; i++) {
            if (random.nextDouble() > localSearchRate) {
                continue;
            }

            Solution localSearched = new Solution(solution);

            Turnus turnus = localSearched.getTurnuses().get(random.nextInt(localSearched.getTurnuses().size()));
            List<Trip> trips = turnus.getTrips();
            trips.remove(0);
            trips.remove(trips.size() - 1);

            localSearched.removeTurnus(turnus);
            localSearched.addTrips(trips);

            if (localSearched.getFitness() < best.getFitness()) {
                best = new Solution(localSearched);
                evaluatePopulation(best);
            }
        }

        return best;
    }
}
