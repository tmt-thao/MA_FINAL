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

    private boolean isValidAfterAddedTrip(double battery, int nextElemIndex) {
        for (int i = nextElemIndex; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem instanceof ChargingEvent && currElem instanceof ChargingEvent currCharger) {
                battery += currCharger.getEnergy();
            } else if (prevElem instanceof ChargingEvent && currElem instanceof Trip currTrip) {
                battery -= currTrip.getEnergy();
            } else if (prevElem instanceof Trip prevTrip && currElem instanceof ChargingEvent currCharger) {
                battery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currCharger.getStop());
                battery += currCharger.getEnergy();
            } else if (prevElem instanceof Trip prevTrip && currElem instanceof Trip currTrip) {
                battery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currTrip.getStartStop());
                battery -= currTrip.getEnergy();
            }

            if (battery < StaticData.MIN_BATTERY) {
                return false;
            }
        }

        return true;
    }

    public boolean addTrip(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        if (StaticData.CHARGING_STRATEGY == ChargingStrategy.AT_START_STOP) {
            return chargingStartStop(newTrip, stopToChargers, chargerToEvents);
        }

        if (StaticData.CHARGING_STRATEGY == ChargingStrategy.AT_END_STOP) {
            return chargingEndStop(newTrip, stopToChargers, chargerToEvents);
        }

        return chargingClosestStop(newTrip, stopToChargers, chargerToEvents);
    }

    private boolean chargingClosestStop(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        

        return false;
    }

    private boolean chargingEndStop(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double battery = StaticData.MAX_BATTERY;

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem.getEndTime() > newTrip.getStartTime()) return false;

            if (currElem instanceof ChargingEvent) {
                battery += currElem.getEnergy();
                continue;
            }

            Trip currTrip = (Trip) currElem;
            int prevStop = prevElem instanceof Trip ? ((Trip) prevElem).getEndStop() : ((ChargingEvent) prevElem).getStop();

            double prevToCurrBattery = -StaticData.getDeadheadEnergy(prevStop, currTrip.getStartStop());
            double prevToNewBattery = -StaticData.getDeadheadEnergy(prevStop, newTrip.getStartStop());
            double newToCurrBattery = -StaticData.getDeadheadEnergy(newTrip.getEndStop(), currTrip.getStartStop());

            int prevToNewTime = prevElem.getEndTime() + StaticData.getTravelTime(prevStop, newTrip.getStartStop());
            int newToCurrTime = newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), currTrip.getStartStop());

            if (battery + prevToNewBattery - newTrip.getEnergy() >= StaticData.MIN_BATTERY
                    && prevToNewTime <= newTrip.getStartTime()
                    && newToCurrTime <= currElem.getStartTime()
                    && finalBattery - prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy() >= StaticData.MIN_BATTERY) {
                        
                elements.add(i, newTrip);
                finalBattery += -prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy();
                battery += prevToNewBattery - newTrip.getEnergy();

                List<ChargingEvent> candidateChargers = getCandidateChargers(newTrip.getEndTime(), stopToChargers, chargerToEvents, 
                currElem.getStartTime() - StaticData.getTravelTime(newTrip.getEndStop(), currTrip.getStartStop()), battery, newTrip.getEndStop());
                double chargersBattery = 0.0;
                for (ChargingEvent chargingEvent : candidateChargers) {
                    chargersBattery += chargingEvent.getEnergy();
                }

                battery += chargersBattery;
                if (battery < StaticData.MIN_BATTERY || !isValidAfterAddedTrip(battery, i + 1 + candidateChargers.size())) {
                    elements.remove(newTrip);
                    finalBattery -= -prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy();
                    return false;
                }

                finalBattery += chargersBattery;
                for (ChargingEvent candidate : candidateChargers) {
                    //freeChargers.remove(candidate);
                    int charger = candidate.getCharger();
                    int stop = candidate.getStop();

                    chargerToEvents.get(charger).remove(candidate);
                    if (chargerToEvents.get(charger).isEmpty()) {
                        chargerToEvents.remove(charger);
                        stopToChargers.get(stop).remove(charger);

                        if (stopToChargers.get(stop).isEmpty()) {
                            stopToChargers.remove(stop);
                        }
                    }
                    
                    elements.add(candidate);
                }
                elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
                return true;
            }

            battery += prevToCurrBattery;
            battery -= currElem.getEnergy();
        }

        return false;
    }

    private boolean chargingStartStop(Trip newTrip, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double battery = StaticData.MAX_BATTERY;

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem.getEndTime() > newTrip.getStartTime()) return false;

            if (prevElem instanceof ChargingEvent) {
                battery += currElem instanceof ChargingEvent ? currElem.getEnergy() : -currElem.getEnergy();
                continue;
            }

            Trip prevTrip = (Trip) prevElem;
            int currStop = currElem instanceof Trip ? ((Trip) currElem).getStartStop() : ((ChargingEvent) currElem).getStop();

            double prevToCurrBattery = -StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currStop);
            double prevToNewBattery = -StaticData.getDeadheadEnergy(prevTrip.getEndStop(), newTrip.getStartStop());
            double newToCurrBattery = -StaticData.getDeadheadEnergy(newTrip.getEndStop(), currStop);

            int prevToNewTime = prevTrip.getEndTime() + StaticData.getTravelTime(prevTrip.getEndStop(), newTrip.getStartStop());
            int newToCurrTime = newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), currStop);

            if (battery + prevToNewBattery >= StaticData.MIN_BATTERY
                    && prevToNewTime <= newTrip.getStartTime()
                    && newToCurrTime <= currElem.getStartTime()
                    && finalBattery - prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy() >= StaticData.MIN_BATTERY) {
                        
                elements.add(i, newTrip);
                finalBattery += -prevToCurrBattery + prevToNewBattery + newToCurrBattery  - newTrip.getEnergy();
                battery += prevToNewBattery;

                List<ChargingEvent> candidateChargers = getCandidateChargers(prevToNewTime, stopToChargers, chargerToEvents, newTrip.getStartTime(), battery, newTrip.getStartStop());
                double chargersBattery = 0.0;
                for (ChargingEvent chargingEvent : candidateChargers) {
                    chargersBattery += chargingEvent.getEnergy();
                }

                battery += chargersBattery - newTrip.getEnergy();
                if (battery < StaticData.MIN_BATTERY || !isValidAfterAddedTrip(battery, i + 1)) {
                    elements.remove(newTrip);
                    finalBattery -= -prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy();
                    return false;
                }

                finalBattery += chargersBattery;
                for (ChargingEvent candidate : candidateChargers) {
                    //freeChargers.remove(candidate);
                    int charger = candidate.getCharger();
                    int stop = candidate.getStop();

                    chargerToEvents.get(charger).remove(candidate);
                    if (chargerToEvents.get(charger).isEmpty()) {
                        chargerToEvents.remove(charger);
                        stopToChargers.get(stop).remove(charger);

                        if (stopToChargers.get(stop).isEmpty()) {
                            stopToChargers.remove(stop);
                        }
                    }

                    elements.add(candidate);
                }
                elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
                return true;
            }

            battery += prevToCurrBattery;
            battery += currElem instanceof Trip ? -currElem.getEnergy() : currElem.getEnergy();
        }

        return false;
    }

    private List<ChargingEvent> getCandidateChargers(int arrivalTime, HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents, int nextElemStartTime, double currBattery, int stop) {
        HashSet<Integer> chargers = stopToChargers.get(stop);
        if (chargers == null) return new ArrayList<>();

        double bestCharged = 0.0;
        List<ChargingEvent> bestCandidates = new ArrayList<>();

        for (int charger : chargers) {
            double currCharged = 0.0;
            List<ChargingEvent> currCandidates = new ArrayList<>();

            for (ChargingEvent chargingEvent : chargerToEvents.get(charger)) {
                if (chargingEvent.getStartTime() < arrivalTime) continue;
                if (chargingEvent.getEndTime() > nextElemStartTime) break;
                if (chargingEvent.getEnergy() + currCharged + finalBattery > StaticData.MAX_BATTERY) break;
                if (chargingEvent.getEnergy() + currCharged + currBattery > StaticData.MAX_BATTERY) break;

                currCharged += chargingEvent.getEnergy();
                currCandidates.add(chargingEvent);
            }

            if (currCharged > bestCharged) {
                bestCharged = currCharged;
                bestCandidates = new ArrayList<>(currCandidates);
                currCandidates.clear();
            }
        }

        return bestCandidates;
    }

    private List<ChargingEvent> getBestChargers(Trip newTrip, Trip currTrip, double currBattery,
            HashMap<Integer, HashSet<Integer>> stopToChargers, HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents) {
        double bestCharged = 0.0;
        List<ChargingEvent> bestCandidates = new ArrayList<>();

        for (int stop : stopToChargers.keySet()) {
            double energyTo = StaticData.getDeadheadEnergy(newTrip.getEndStop(), stop);
            double energyFrom = StaticData.getDeadheadEnergy(stop, currTrip.getStartStop());
            if (currBattery - energyTo < StaticData.MIN_BATTERY) continue;

            int minStartTime = newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), stop);
            int maxEndTime = currTrip.getStartTime() - StaticData.getTravelTime(stop, currTrip.getStartStop());
            if (minStartTime > maxEndTime) continue;

            HashSet<Integer> chargers = stopToChargers.get(stop);
            if (chargers == null) continue;

            for (int charger : chargers) {
                double currCharged = 0.0;
                List<ChargingEvent> currCandidates = new ArrayList<>();

                for (ChargingEvent chargingEvent : chargerToEvents.get(charger)) {
                    if (chargingEvent.getStartTime() < minStartTime) continue;
                    if (chargingEvent.getEndTime() > maxEndTime) break;
                    if (chargingEvent.getEnergy() + currCharged - energyTo + finalBattery > StaticData.MAX_BATTERY) break;
                    if (chargingEvent.getEnergy() + currCharged - energyTo + currBattery > StaticData.MAX_BATTERY) break;

                    currCharged += chargingEvent.getEnergy();
                    currCandidates.add(chargingEvent);
                }

                if (currCharged > bestCharged && currCharged > energyTo + energyFrom) {
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

            if (prevElem instanceof ChargingEvent && currElem instanceof ChargingEvent currCharger) {
                currBattery += currCharger.getEnergy();
            } else if (prevElem instanceof ChargingEvent prevCharger && currElem instanceof Trip currTrip) {
                if (StaticData.CHARGING_STRATEGY == ChargingStrategy.AT_END_STOP) {
                    currBattery -= StaticData.getDeadheadEnergy(prevCharger.getStop(), currTrip.getStartStop());
                }
                currBattery -= currTrip.getEnergy();
            } else if (prevElem instanceof Trip prevTrip && currElem instanceof ChargingEvent currCharger) {
                if (StaticData.CHARGING_STRATEGY == ChargingStrategy.AT_START_STOP) {
                    currBattery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currCharger.getStop());
                }
                currBattery += currCharger.getEnergy();
            } else if (prevElem instanceof Trip prevTrip && currElem instanceof Trip currTrip) {
                currBattery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currTrip.getStartStop());
                currBattery -= currTrip.getEnergy();
            }

            sb.append(String.format("%-100s", currElem));
            sb.append("\tBattery: ").append(String.format(Locale.US, "%7.3f", currBattery)).append("\n");
        }

        sb.append("Battery Status: ").append(finalBattery).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
