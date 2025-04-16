import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Solution {
    private List<Turnus> turnuses;
    private List<ChargingEvent> freeChargers;

    public Solution() {
        this.turnuses = new ArrayList<>();

        this.freeChargers = new ArrayList<>();
        for (ChargingEvent chargingEvent : StaticData.chargingEvents) {
            this.freeChargers.add(new ChargingEvent(chargingEvent));
        }
    }

    public Solution(Solution other) {
        this.turnuses = new ArrayList<>();
        for (Turnus turnus : other.turnuses) {
            this.turnuses.add(new Turnus(turnus));
        }

        this.freeChargers = new ArrayList<>();
        for (ChargingEvent chargingEvent : other.freeChargers) {
            this.freeChargers.add(new ChargingEvent(chargingEvent));
        }
    }

    public void clear() {
        this.turnuses.clear();

        this.freeChargers.clear();
        for (ChargingEvent chargingEvent : StaticData.chargingEvents) {
            this.freeChargers.add(new ChargingEvent(chargingEvent));
        }
    }
    
    public static Solution generateSolution(List<Trip> trips) {
        Solution solution = new Solution();

        for (Trip trip : trips) {
            boolean added = false;

            for (Turnus turnus : solution.turnuses) {
                if (turnus.addTrip(trip, solution.freeChargers)) {
                    added = true;
                    break;
                }
            }
            if (!added) {
                Turnus newTurnus = new Turnus();
                newTurnus.addTrip(trip, solution.freeChargers);
                solution.addTurnus(newTurnus);
            }
        }

        for (Turnus turnus : solution.turnuses) {
            turnus.addDepoEnd();
        }

        return solution;
    }

    public double getFitness() {
        double deadheadDistance = 0;
        for (Turnus turnus : turnuses) {
            deadheadDistance += turnus.getDeadheadDistance();
        }

        return 1000 * turnuses.size() + 10 * deadheadDistance;
    }

    public int getUniqueTripsCount() {
        Set<Integer> ids = new HashSet<>();
        for (Turnus t : turnuses) {
            for (Trip trip : t.getTrips()) ids.add(trip.getId());
        }
        return ids.size();
    }

    public int getAllTripsCount() {
        int count = 0;
        for (Turnus t : turnuses) {
            count += t.getTrips().size() - 2;   // -2 for depo start and end
        }
        return count + 2;   // +2 for depo start and end
    }

    public void addTurnus(Turnus turnus) {
        this.turnuses.add(turnus);

        for (ChargingEvent chargingEvent : turnus.getChargingEvents()) {
            freeChargers.remove(chargingEvent);
        }
    }

    public void removeTurnus(Turnus turnus) {
        for (ChargingEvent chargingEvent : turnus.getChargingEvents()) {
            freeChargers.add(chargingEvent);
        }
        this.turnuses.remove(turnus);
    }

    // TODO: addTrip<Trip> - pridat trip do nejakeho turnusu, ak sa tam zmesti, inak vytvorit novy turnus
    public void addTrips(List<Trip> trips) {
        for (Trip trip : trips) {
            boolean added = false;

            for (Turnus turnus : turnuses) {
                if (turnus.getLastTrip().getEndStop() == StaticData.depoEnd.getStartStop()) {
                    turnus.getTrips().remove(turnus.getTrips().size() - 1); // skip depo end trip
                }
                if (turnus.addTrip(trip, freeChargers)) {
                    added = true;
                    break;
                }
            }
            if (!added) {
                Turnus newTurnus = new Turnus();
                newTurnus.addTrip(trip, freeChargers);
                addTurnus(newTurnus);
            }
        }

        for (Turnus turnus : turnuses) {
            if (turnus.getLastTrip().getStartStop() != StaticData.depoEnd.getStartStop()) {
                turnus.addDepoEnd();
            }
        }
    }

    public List<Turnus> getTurnuses() {
        return turnuses;
    }

    public List<ChargingEvent> getFreeChargers() {
        return freeChargers;
    }

    public int getNumberOfTurnuses() {
        return turnuses.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Solution{\n");

        for (int i = 0; i < turnuses.size(); i++) {
            Turnus turnus = turnuses.get(i);
            sb.append(i + 1).append(". ").append(turnus).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
