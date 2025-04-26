import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MemeticAlgorithm {
    private int populationSize;
    private int maxNoChangeCount;

    private double mutationRate;
    private int mutationNum;
    private double localSearchRate;
    private int localSearchNum;

    private List<Solution> population;
    private Solution bestSolution;
    private Random random;

    private double durationInSeconds;
    private double timeLimitInSeconds;

    public MemeticAlgorithm(int populationSize, int generations, double mutationRate, int mutationNum,
            double localSearchRate, int localSearchNum, double timeLimitInSeconds) {
        this.populationSize = populationSize;
        this.maxNoChangeCount = generations;

        this.mutationRate = mutationRate;
        this.mutationNum = mutationNum;
        this.localSearchRate = localSearchRate;
        this.localSearchNum = localSearchNum;

        this.population = new ArrayList<>();
        this.bestSolution = null;
        this.random = new Random();

        this.durationInSeconds = 0.0;
        this.timeLimitInSeconds = timeLimitInSeconds;
    }

    public Solution getBestSolution() {
        return bestSolution;
    }

    public double getDurationInSeconds() {
        return durationInSeconds;
    }

    public void run() {
        long startTime = System.nanoTime();
        int noChangeCount = 0;

        initializePopulation();
        int prevBestTurnuses = bestSolution.getNumberOfTurnuses();

        while (noChangeCount < maxNoChangeCount &&  durationInSeconds < timeLimitInSeconds) {
            List<Solution> newPopulation = new ArrayList<>();

            List<Solution> sortedPopulation = new ArrayList<>(population);
            Collections.sort(sortedPopulation, Comparator.comparing(Solution::getNumberOfTurnuses).thenComparing(Solution::getFitness));
            int elitismCount = Math.max((int)(populationSize * 0.1), 1);
            for (int i = 0; i < elitismCount; i++) {
                newPopulation.add(sortedPopulation.get(i));
            }

            while (newPopulation.size() < populationSize) {
                Solution parent1 = selectParent();
                Solution parent2 = selectParent();
                Solution child = crossover(parent1, parent2);
                evaluatePopulation(child);

                child = mutate(child);
                child = localSearch(child);
                newPopulation.add(child);

                if (bestSolution.getNumberOfTurnuses() == prevBestTurnuses) {
                    noChangeCount++;
                } else {
                    noChangeCount = 0;
                    prevBestTurnuses = bestSolution.getNumberOfTurnuses();
                }
            }
            population = newPopulation;

            long endTime = System.nanoTime();
            this.durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        }
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

        Turnus turnus1 = getTurnus(parent1, null);
        Turnus turnus2 = getTurnus(parent2, turnus1);

        List<Trip> trips1 = getTripsWithoutDepo(turnus1);
        List<Trip> trips2 = getTripsWithoutDepo(turnus2);

        List<Trip> duplicatedTrips = getCrossoverTrips(trips1, trips2);
        List<Trip> missingTrips = getCrossoverTrips(trips2, trips1);

        List<Trip> parent2Trips = new ArrayList<>();
        for (Turnus turnus : parent2.getTurnuses()) {
            if (turnus != turnus2) {
                List<Trip> trips = getTripsWithoutDepo(turnus);
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

    private Turnus getTurnus(Solution solution, Turnus other) {
        Turnus turnus = solution.getTurnuses().get(random.nextInt(solution.getTurnuses().size()));
        while ((other != null && turnus == other) || turnus.getTrips().size() < 3) {
            turnus = solution.getTurnuses().get(random.nextInt(solution.getTurnuses().size()));
        }

        return turnus;
    }

    private List<Trip> getCrossoverTrips(List<Trip> trips1, List<Trip> trips2) {
        List<Trip> trips = new ArrayList<>(trips1);

        for (Trip trip1 : trips1) {
            for (Trip trip2 : trips2) {
                if (trip1.getId() == trip2.getId()) {
                    trips.remove(trip1);
                    break;
                }
            }
        }

        return trips;
    }

    private List<Trip> getTripsWithoutDepo(Turnus turnus) {
        List<Trip> trips = new ArrayList<>(turnus.getTrips());
        trips.remove(0);
        trips.remove(trips.size() - 1);

        return trips;
    }

    private Solution mutate(Solution solution) {
        Solution best = new Solution(solution);

        for (int i = 0; i < mutationNum; i++) {
            if (random.nextDouble() > mutationRate) {
                continue;
            }

            Solution mutated = new Solution();

            Turnus turnus1 = getTurnus(solution, null);
            Turnus turnus2 = getTurnus(solution, turnus1);

            for (Turnus turnus : solution.getTurnuses()) {
                if (turnus != turnus1 && turnus != turnus2) {
                    mutated.addTurnus(new Turnus(turnus));
                }
            }

            List<Trip> trips1 = getTripsWithoutDepo(turnus1);
            List<Trip> trips2 = getTripsWithoutDepo(turnus2);

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

            List<Trip> toBeAddedTrips1 = getToBeAddedTrips(trips1, seq1, seq2);
            List<Trip> notAdded = new ArrayList<>();
            Turnus newTurnus1 = new Turnus();
            fillNotAddedTrips(mutated, toBeAddedTrips1, notAdded, newTurnus1);
            mutated.addTurnus(newTurnus1);

            List<Trip> toBeAddedTrips2 = getToBeAddedTrips(trips2, seq2, seq1);
            Turnus newTurnus2 = new Turnus();
            fillNotAddedTrips(mutated, toBeAddedTrips2, notAdded, newTurnus2);
            mutated.addTurnus(newTurnus2);
            mutated.addTrips(notAdded);

            if (mutated.getFitness() < best.getFitness()) {
                best = new Solution(mutated);
                evaluatePopulation(best);
            }
        }

        return best;
    }

    private void fillNotAddedTrips(Solution mutated, List<Trip> toBeAddedTrips, List<Trip> notAdded, Turnus newTurnus) {
        for (Trip trip : toBeAddedTrips) {
            if (!newTurnus.addTrip(trip, mutated.getStopToChargers(), mutated.getChargerToEvents())) {
                notAdded.add(trip);
            }
        }
    }

    private List<Trip> getToBeAddedTrips(List<Trip> trips1, List<Trip> seq1, List<Trip> seq2) {
        List<Trip> toBeAddedTrips = new ArrayList<>();
        toBeAddedTrips.addAll(trips1);
        toBeAddedTrips.removeAll(seq1);
        toBeAddedTrips.addAll(seq2);
        toBeAddedTrips.sort(Comparator.comparing(Trip::getStartTime));

        return toBeAddedTrips;
    }

    private Solution localSearch(Solution solution) {
        Solution best = new Solution(solution);

        for (int i = 0; i < localSearchNum; i++) {
            if (random.nextDouble() > localSearchRate) {
                continue;
            }

            Solution localSearched = new Solution(solution);

            Turnus turnus = getTurnus(localSearched, null);
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
