import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    public Trip getLastTrip() {
        List<Trip> trips = getTrips();
        return trips.get(trips.size() - 1); // urcite je v turnuse minimalne 1 trip, a to ranne depo
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

    public Trip getNexTrip(int index) {
        for (int i = index + 1; i < elements.size(); i++) {
            if (elements.get(i) instanceof Trip trip) {
                return trip;
            }
        }
        return null;
    }

    public boolean isValid() {
        double battery = StaticData.MAX_BATTERY;

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem.getEndTime() > currElem.getStartTime()) {
                return false;
            }

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

    public boolean isValidAfterAddedTrip(double battery, int nextElemIndex) {
        for (int i = nextElemIndex; i < elements.size(); i++) {
            TurnusElement currElem = elements.get(i);

            if (currElem instanceof Trip currTrip) {
                battery -= currTrip.getEnergy();
            } else if (currElem instanceof ChargingEvent currCharger) {
                battery += currCharger.getEnergy();
            }

            if (battery < StaticData.MIN_BATTERY) {
                return false;
            }
        }

        return true;
    }

    // bud je {Trip} al. {ChargingEvent, Trip}
    public boolean addTrip(Trip newTrip, List<ChargingEvent> freeChargers) {
        double battery = StaticData.MAX_BATTERY;

        for (int i = 1; i < elements.size(); i++) {

            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem.getEndTime() > newTrip.getStartTime()) {
                return false;
            }

            if (prevElem instanceof ChargingEvent && currElem instanceof ChargingEvent currCharger) {
                battery += currCharger.getEnergy();
                continue;
            } 
            
            if (prevElem instanceof ChargingEvent && currElem instanceof Trip currTrip) {
                battery -= currTrip.getEnergy();
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

                    && finalBattery - prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy() >= StaticData.MIN_BATTERY
                    ) {
                        
                elements.add(i, newTrip);
                finalBattery += -prevToCurrBattery + prevToNewBattery + newToCurrBattery - newTrip.getEnergy();
                battery += prevToNewBattery;

                List<ChargingEvent> candidateChargers = getCandidateChargers(prevToNewTime, freeChargers, newTrip, battery);
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
                    freeChargers.remove(candidate);
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

    public List<ChargingEvent> getCandidateChargers(int arrivalTime, List<ChargingEvent> freeChargers, Trip trip, double currBattery) {
        List<ChargingEvent> candidates = new ArrayList<>();

        for (ChargingEvent chargingEvent : freeChargers) {
            if (!candidates.isEmpty()
                    && chargingEvent.getCharger() != candidates.get(candidates.size() - 1).getCharger()) {
                break;
            }

            if (chargingEvent.getStop() == trip.getStartStop()
                    && chargingEvent.getStartTime() >= arrivalTime
                    && chargingEvent.getEndTime() <= trip.getStartTime()
                    && chargingEvent.getEnergy() + finalBattery <= StaticData.MAX_BATTERY
                    && chargingEvent.getEnergy() + currBattery <= StaticData.MAX_BATTERY) {
                candidates.add(chargingEvent);
                //finalBattery += chargingEvent.getEnergy();
                currBattery += chargingEvent.getEnergy();
            }
        }

        for (ChargingEvent candidate : candidates) {
            //freeChargers.remove(candidate);
            //elements.add(candidate);
        }
        //elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
        return candidates;
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
        sb.append(String.format("%-100s", elements.get(0))).append("\tBattery: ").append(String.format("%7.3f", currBattery)).append("\n");

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElem = elements.get(i - 1);
            TurnusElement currElem = elements.get(i);

            if (prevElem instanceof ChargingEvent && currElem instanceof ChargingEvent currCharger) {
                currBattery += currCharger.getEnergy();
            } else if (prevElem instanceof ChargingEvent && currElem instanceof Trip currTrip) {
                currBattery -= currTrip.getEnergy();
            } else if (prevElem instanceof Trip prevTrip && currElem instanceof ChargingEvent currCharger) {
                currBattery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currCharger.getStop());
                currBattery += currCharger.getEnergy();
            } else if (prevElem instanceof Trip prevTrip && currElem instanceof Trip currTrip) {
                currBattery -= StaticData.getDeadheadEnergy(prevTrip.getEndStop(), currTrip.getStartStop());
                currBattery -= currTrip.getEnergy();
            }

            sb.append(String.format("%-100s", currElem));
            sb.append("\tBattery: ").append(String.format("%7.3f", currBattery)).append("\n");
        }

        sb.append("Battery Status: ").append(finalBattery).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
