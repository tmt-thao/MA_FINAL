import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Solution {
    private List<Turnus> turnuses;
    private HashMap<Integer, HashSet<Integer>> stopToChargers;
    private HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents;

    public Solution() {
        this.turnuses = new ArrayList<>();
        this.stopToChargers = StaticData.getStopToChargersCopy();
        this.chargerToEvents = StaticData.getChargerToEventsCopy();
    }

    public Solution(Solution other) {
        this.turnuses = new ArrayList<>();
        for (Turnus turnus : other.turnuses) {
            this.turnuses.add(new Turnus(turnus));
        }

        this.stopToChargers = new HashMap<>();
        for (Map.Entry<Integer, HashSet<Integer>> entry : other.getStopToChargers().entrySet()) {
            this.stopToChargers.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        this.chargerToEvents = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<ChargingEvent>> entry : other.getChargerToEvents().entrySet()) {
            ArrayList<ChargingEvent> copiedList = new ArrayList<>();
            for (ChargingEvent event : entry.getValue()) {
                copiedList.add(new ChargingEvent(event));
            }
            this.chargerToEvents.put(entry.getKey(), copiedList);
        }
    }

    public HashMap<Integer, HashSet<Integer>> getStopToChargers() {
        return stopToChargers;
    }

    public HashMap<Integer, ArrayList<ChargingEvent>> getChargerToEvents() {
        return chargerToEvents;
    }
    
    public static Solution generate(List<Trip> trips) {
        Solution solution = new Solution();
        solution.addTrips(trips);
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
            count += t.getTrips().size() - 2;
        }
        return count + 2;
    }

    public void addTurnus(Turnus turnus) {
        this.turnuses.add(turnus);

        for (ChargingEvent chargingEvent : turnus.getChargingEvents()) {
            int charger = chargingEvent.getCharger();
            int stop = chargingEvent.getStop();

            chargerToEvents.get(charger).remove(chargingEvent);
            if (chargerToEvents.get(charger).isEmpty()) {
                chargerToEvents.remove(charger);
                stopToChargers.get(stop).remove(charger);

                if (stopToChargers.get(stop).isEmpty()) {
                    stopToChargers.remove(stop);
                }
            }


            // freeChargers.remove(chargingEvent);
        }
    }

    public void removeTurnus(Turnus turnus) {
        for (ChargingEvent chargingEvent : turnus.getChargingEvents()) {
            int charger = chargingEvent.getCharger();
            int stop = chargingEvent.getStop();

            chargerToEvents.putIfAbsent(charger, new ArrayList<>());
            chargerToEvents.get(charger).add(chargingEvent);

            stopToChargers.putIfAbsent(stop, new HashSet<>());
            stopToChargers.get(stop).add(charger);

            // freeChargers.add(chargingEvent);
        }
        this.turnuses.remove(turnus);
    }

    public void addTrip(Trip trip) {
        boolean added = false;

        for (Turnus turnus : turnuses) {
            if (turnus.addTrip(trip, stopToChargers, chargerToEvents)) {
                added = true;
                break;
            }
        }

        if (!added) {
            Turnus newTurnus = new Turnus();
            newTurnus.addTrip(trip, stopToChargers, chargerToEvents);
            addTurnus(newTurnus);
        }
    }

    public void addTrips(List<Trip> trips) {
        for (Trip trip : trips) {
            addTrip(trip);
        }
    }

    public List<Turnus> getTurnuses() {
        return turnuses;
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
