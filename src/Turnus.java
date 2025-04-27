import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class Turnus {
    private List<TurnusElement> elements;
    private double finalBattery;

    public Turnus() {
        this.elements = new ArrayList<>();
        this.elements.add(new Trip(StaticData.depoStart));
        this.elements.add(new Trip(StaticData.depoEnd));

        this.finalBattery = StaticData.MAX_BATTERY;
    }

    public Turnus(Turnus other) {
        this.elements = new ArrayList<>(other.elements.size());
        for (TurnusElement element : other.elements) {
            if (element instanceof Trip trip) {
                this.elements.add(new Trip(trip));
            } else if (element instanceof ChargingEvent chargingEvent) {
                this.elements.add(new ChargingEvent(chargingEvent));
            }
        }

        this.finalBattery = other.finalBattery;
    }

    public List<Trip> getTrips() {
        List<Trip> trips = new ArrayList<>();
        for (TurnusElement element : elements) {
            if (element instanceof Trip trip) {
                trips.add(trip);
            }
        }
        return trips;
    }

    public List<ChargingEvent> getChargingEvents() {
        List<ChargingEvent> chargingEvents = new ArrayList<>();
        for (TurnusElement element : elements) {
            if (element instanceof ChargingEvent chargingEvent) {
                chargingEvents.add(chargingEvent);
            }
        }
        return chargingEvents;
    }

    private boolean isValidAfterAddedTripWhenPossible(double battery, int nextElemIndex) {
        for (int i = nextElemIndex; i < elements.size() - 1; i++) {
            TurnusElement currElem = elements.get(i);
            TurnusElement nextElem = elements.get(i + 1);

            int currStop = currElem instanceof Trip ? ((Trip) currElem).getEndStop() : ((ChargingEvent) currElem).getStop();
            int nextStop = nextElem instanceof Trip ? ((Trip) nextElem).getStartStop() : ((ChargingEvent) nextElem).getStop();

            battery += currElem instanceof Trip ? -currElem.getEnergy() : currElem.getEnergy();
            if (battery < StaticData.MIN_BATTERY || battery > StaticData.MAX_BATTERY) {
                return false;
            }

            battery -= StaticData.getDeadheadEnergy(currStop, nextStop);
            if (battery < StaticData.MIN_BATTERY) {
                return false;
            }
        }

        return true;
    }

    public boolean addTrip(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        if (StaticData.chargingStrategy == ChargingStrategy.WHEN_NEEDED) {
            return isFeasibleWhenNeeded(newTrip, stopToChargers, chargerToEvents);
        }
        return isFeasibleWhenPossible(newTrip, stopToChargers, chargerToEvents);
    }

    private boolean isFeasibleWhenNeeded(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        List<ChargingEvent> origChargingEvents = getChargingEvents();
        returnChEvents(origChargingEvents, stopToChargers, chargerToEvents);
        double origFinalBattery = finalBattery;

        List<Trip> trips = getTrips();
        trips.add(newTrip);
        trips.sort(Comparator.comparingInt(Trip::getStartTime));

        List<TurnusElement> newElements = new ArrayList<>();
        newElements.add(StaticData.depoStart);
        List<ChargingEvent> newChargingEvents = new ArrayList<>();
        finalBattery = StaticData.MAX_BATTERY;

        for (int i = 1; i < trips.size(); i++) {
            Trip prev = trips.get(i - 1);
            Trip curr = trips.get(i);

            if (prev.getEndTime() > curr.getStartTime()) {
                returnChEvents(newChargingEvents, stopToChargers, chargerToEvents);
                takeBackChEvents(origChargingEvents, stopToChargers, chargerToEvents);
                finalBattery = origFinalBattery;
                return false;
            }

            double energyNeeded = StaticData.getDeadheadEnergy(prev.getEndStop(), curr.getStartStop()) + curr.getEnergy();
            if (finalBattery - energyNeeded >= StaticData.MIN_BATTERY) {
                finalBattery -= energyNeeded;
                newElements.add(curr);
                continue;
            }

            List<ChargingEvent> chEvents = new ArrayList<>();
            double charged = chargeWhenNeeded(newElements, curr.getId(), chEvents, stopToChargers, chargerToEvents);
            if (charged < energyNeeded) {
                returnChEvents(newChargingEvents, stopToChargers, chargerToEvents);
                takeBackChEvents(origChargingEvents, stopToChargers, chargerToEvents);
                finalBattery = origFinalBattery;
                return false;
            }

            finalBattery -= energyNeeded;
            newElements.add(curr);
            
            newElements.addAll(chEvents);
            newElements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
            newChargingEvents.addAll(chEvents);
            
            takeBackChEvents(chEvents, stopToChargers, chargerToEvents);
        }

        elements = newElements;
        return true;
    }

    private double chargeWhenNeeded(List<TurnusElement> newElements, int lastTripId, List<ChargingEvent> chEvents,
            HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double charged = 0.0;
        finalBattery = StaticData.MAX_BATTERY;

        for (int i = 1; i < newElements.size(); i++) {
            TurnusElement prevElem = newElements.get(i - 1);
            TurnusElement currElem = newElements.get(i);

            int prevStop = prevElem instanceof Trip ? ((Trip) prevElem).getEndStop() : ((ChargingEvent) prevElem).getStop();
            int currStop = currElem instanceof Trip ? ((Trip) currElem).getStartStop() : ((ChargingEvent) currElem).getStop();

            if (currElem instanceof Trip && ((Trip) currElem).getId() == lastTripId) break;

            if (prevElem instanceof Trip prevTrip && currElem instanceof Trip currTrip) {
                double currCharged = chargeBetweenTrips(prevTrip, currTrip, chEvents, stopToChargers, chargerToEvents);

                if (currCharged == 0.0) {
                    finalBattery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currTrip.getStartStop()) + currTrip.getEnergy();
                } else {
                    charged += currCharged;
                    finalBattery += currCharged - currTrip.getEnergy();
                }                
            } else {
                finalBattery -= StaticData.getDeadheadEnergy(prevStop, currStop);
                finalBattery += currElem instanceof ChargingEvent ? currElem.getEnergy() : -currElem.getEnergy();
            }

        }

        return charged;
    }

    private double chargeBetweenTrips(Trip prevTrip, Trip currTrip, List<ChargingEvent> candidates,
            HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double bestCharged = 0.0;
        List<ChargingEvent> bestCandidates = new ArrayList<>();

        for (int stop : stopToChargers.keySet()) {
            double energyTo = StaticData.getDeadheadEnergy(prevTrip.getEndStop(), stop);
            double energyFrom = StaticData.getDeadheadEnergy(stop, currTrip.getStartStop());
            if (finalBattery - energyTo < StaticData.MIN_BATTERY) continue;

            int minStartTime = prevTrip.getEndTime() + StaticData.getTravelTime(prevTrip.getEndStop(), stop);
            int maxEndTime = currTrip.getStartTime() - StaticData.getTravelTime(stop, currTrip.getStartStop());
            if (minStartTime >= maxEndTime) continue;

            HashSet<Integer> chargers = stopToChargers.get(stop);
            if (chargers == null) continue;

            for (int charger : chargers) {
                double currCharged = 0.0;
                List<ChargingEvent> currCandidates = new ArrayList<>();

                List<ChargingEvent> chEvents = chargerToEvents.get(charger);
                if (chEvents == null) continue;

                for (ChargingEvent chargingEvent : chEvents) {
                    if (chargingEvent.getStartTime() < minStartTime) continue;
                    if (chargingEvent.getEndTime() > maxEndTime) break;
                    if (chargingEvent.getEnergy() + currCharged - energyTo + finalBattery > StaticData.MAX_BATTERY) break;

                    currCharged += chargingEvent.getEnergy();
                    currCandidates.add(chargingEvent);
                }

                if (currCharged - energyTo - energyFrom > bestCharged && currCharged > energyTo + energyFrom) {
                    bestCharged = currCharged - energyTo - energyFrom;
                    bestCandidates = currCandidates;
                }
            }
        }

        candidates.addAll(bestCandidates);
        return bestCharged;
    }

    private void takeBackChEvents(List<ChargingEvent> origChargingEvents,
            HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        for (ChargingEvent chargingEvent : origChargingEvents) {
            int charger = chargingEvent.getCharger();
            int stop = chargingEvent.getStop();

            chargerToEvents.putIfAbsent(charger, new ArrayList<>());
            chargerToEvents.get(charger).add(chargingEvent);

            stopToChargers.putIfAbsent(stop, new HashSet<>());
            stopToChargers.get(stop).add(charger);
        }
    }

    private void returnChEvents(List<ChargingEvent> origChargingEvents,
            HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        for (ChargingEvent chargingEvent : origChargingEvents) {
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
        }
    }

    private boolean isFeasibleWhenPossible(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double battery = StaticData.MAX_BATTERY;

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem.getEndTime() > newTrip.getStartTime()) return false;

            int prevStop = prevElem instanceof Trip ? ((Trip) prevElem).getEndStop() : ((ChargingEvent) prevElem).getStop();
            int currStop = currElem instanceof Trip ? ((Trip) currElem).getStartStop() : ((ChargingEvent) currElem).getStop();

            if (currElem instanceof ChargingEvent) {
                battery -= StaticData.getDeadheadEnergy(prevStop, currStop);
                battery += currElem.getEnergy();
                continue;
            }
            Trip currTrip = (Trip) currElem;

            double prevToCurrBattery = StaticData.getDeadheadEnergy(prevStop, currTrip.getStartStop());
            double prevToNewBattery = StaticData.getDeadheadEnergy(prevStop, newTrip.getStartStop());
            double newToCurrBattery = StaticData.getDeadheadEnergy(newTrip.getEndStop(), currTrip.getStartStop());

            int prevToNewTime = prevElem.getEndTime() + StaticData.getTravelTime(prevStop, newTrip.getStartStop());
            int newToCurrTime = newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), currTrip.getStartStop());

            if (battery - prevToNewBattery - newTrip.getEnergy() >= StaticData.MIN_BATTERY
                    && prevToNewTime <= newTrip.getStartTime()
                    && newToCurrTime <= currElem.getStartTime()
                    && finalBattery + prevToCurrBattery - prevToNewBattery - newTrip.getEnergy() - newToCurrBattery >= StaticData.MIN_BATTERY) {
                        
                elements.add(i, newTrip);
                finalBattery += prevToCurrBattery - prevToNewBattery - newTrip.getEnergy();
                battery -= prevToNewBattery + newTrip.getEnergy();

                List<ChargingEvent> candidateChargers = chargeWhenPossible(newTrip, currTrip, battery, stopToChargers, chargerToEvents);
                if (candidateChargers.isEmpty()) {
                    finalBattery -= newToCurrBattery;

                    if (finalBattery < StaticData.MIN_BATTERY || !isValidAfterAddedTripWhenPossible(battery - newToCurrBattery, i + 1)) {
                        elements.remove(newTrip);
                        finalBattery -= prevToCurrBattery - prevToNewBattery - newTrip.getEnergy() - newToCurrBattery;
                        return false;
                    }
                    return true;
                }

                double charged = 0.0;
                for (ChargingEvent chargingEvent : candidateChargers) {
                    charged += chargingEvent.getEnergy();
                }
                int stop = candidateChargers.get(0).getStop();
                int charger = candidateChargers.get(0).getCharger();
                charged -= StaticData.getDeadheadEnergy(newTrip.getEndStop(), stop);
                charged -= StaticData.getDeadheadEnergy(stop, currTrip.getStartStop());

                battery += charged;
                finalBattery += charged;
                if (!isValidAfterAddedTripWhenPossible(battery, i + 1)) {
                    elements.remove(newTrip);
                    finalBattery -= prevToCurrBattery - prevToNewBattery - newTrip.getEnergy() + charged;
                    return false;
                }

                for (ChargingEvent candidate : candidateChargers) {
                    chargerToEvents.get(charger).remove(candidate);                                        
                    elements.add(candidate);
                }
                if (chargerToEvents.get(charger).isEmpty()) {
                    chargerToEvents.remove(charger);
                    stopToChargers.get(stop).remove(charger);

                    if (stopToChargers.get(stop).isEmpty()) {
                        stopToChargers.remove(stop);
                    }
                }
                elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
                return true;
            }

            battery -= prevToCurrBattery + currElem.getEnergy();
        }

        elements.remove(newTrip);
        return false;
    }

    private List<ChargingEvent> chargeWhenPossible(Trip newTrip, Trip currTrip, double currBattery,
            HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double bestCharged = 0.0;
        List<ChargingEvent> bestCandidates = new ArrayList<>();

        for (int stop : stopToChargers.keySet()) {
            double energyTo = StaticData.getDeadheadEnergy(newTrip.getEndStop(), stop);
            double energyFrom = StaticData.getDeadheadEnergy(stop, currTrip.getStartStop());
            if (currBattery - energyTo < StaticData.MIN_BATTERY) continue;

            int minStartTime = newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), stop);
            int maxEndTime = currTrip.getStartTime() - StaticData.getTravelTime(stop, currTrip.getStartStop());
            if (minStartTime >= maxEndTime) continue;

            HashSet<Integer> chargers = stopToChargers.get(stop);
            if (chargers == null) continue;

            for (int charger : chargers) {
                double currCharged = 0.0;
                List<ChargingEvent> currCandidates = new ArrayList<>();
                List<ChargingEvent> chEvents = chargerToEvents.get(charger);
                if (chEvents == null) continue;

                for (ChargingEvent chargingEvent : chEvents) {
                    if (chargingEvent.getStartTime() < minStartTime) continue;
                    if (chargingEvent.getEndTime() > maxEndTime) break;
                    if (chargingEvent.getEnergy() + currCharged - energyTo - energyFrom + finalBattery > StaticData.MAX_BATTERY) break;
                    if (chargingEvent.getEnergy() + currCharged - energyTo + currBattery > StaticData.MAX_BATTERY) break;

                    currCharged += chargingEvent.getEnergy();
                    currCandidates.add(chargingEvent);
                }

                if (currCharged - energyTo - energyFrom > bestCharged && currCharged > energyTo + energyFrom) {
                    bestCharged = currCharged - energyTo - energyFrom;
                    bestCandidates = currCandidates;
                }
            }
        }

        return bestCandidates;
    }

    public double getDeadheadDistance() {
        double sum = 0.0;
        for (int i = 1; i < getTrips().size(); i++) {
            Trip prev = getTrips().get(i - 1);
            Trip curr = getTrips().get(i);
            sum += StaticData.getTravelDistance(prev.getEndStop(), curr.getStartStop());
        }
        return sum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Turnus{\n");

        double currBattery = StaticData.MAX_BATTERY;
        sb.append(String.format("%-100s", elements.get(0)))
            .append("\tBattery: ")
            .append(String.format(Locale.US, "%7.3f", currBattery)).append("\n");

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            int prevStop = prevElem instanceof Trip ? ((Trip) prevElem).getEndStop() : ((ChargingEvent) prevElem).getStop();
            int currStop = currElem instanceof Trip ? ((Trip) currElem).getStartStop() : ((ChargingEvent) currElem).getStop();

            currBattery -= StaticData.getDeadheadEnergy(prevStop, currStop);
            currBattery += currElem instanceof ChargingEvent ? currElem.getEnergy() : -currElem.getEnergy();

            sb.append(String.format("%-100s", currElem));
            sb.append("\tBattery: ").append(String.format(Locale.US, "%7.3f", currBattery)).append("\n");
        }

        sb.append("Battery Status: ").append(finalBattery).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
