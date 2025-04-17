import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Turnus {
    private List<TurnusElement> elements;
    private double batteryStatus;

    public Turnus() {
        this.elements = new ArrayList<>();
        this.elements.add(new Trip(StaticData.depoStart));
        this.elements.add(new Trip(StaticData.depoEnd));
        this.batteryStatus = StaticData.MAX_BATTERY;
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

        this.batteryStatus = other.batteryStatus;
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

    // public void addDepoEnd() {
    //     elements.add(new Trip(StaticData.depoEnd));
    //     batteryStatus -= StaticData.getDeadheadEnergy(getLastTrip().getEndStop(), StaticData.depoEnd.getStartStop());
    // }

    public Trip getNexTrip(int index) {
        for (int i = index + 1; i < elements.size(); i++) {
            if (elements.get(i) instanceof Trip trip) {
                return trip;
            }
        }
        return null;
    }

    // public boolean addTrip2(Trip newTrip, List<ChargingEvent> freeChargers) {
    //     for (int i = 0; i < elements.size() - 1; i++) {
    //         if (elements.get(i) instanceof Trip currTrip) {
    //             Trip nextTrip = getNexTrip(i);
    
    //             int arrivalTime = currTrip.getEndTime()
    //                     + StaticData.getTravelTime(currTrip.getEndStop(), newTrip.getStartStop());
    //             double energyConsumption = StaticData.getDeadheadEnergy(currTrip.getEndStop(), newTrip.getStartStop())
    //                     + newTrip.getEnergy() 
    //                     + StaticData.getDeadheadEnergy(newTrip.getEndStop(), nextTrip.getStartStop())
    //                     - StaticData.getDeadheadEnergy(currTrip.getEndStop(), nextTrip.getStartStop());
    
    //             if (arrivalTime <= newTrip.getStartTime()
    //                     && batteryStatus - energyConsumption >= StaticData.MIN_BATTERY
    //                     && newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), nextTrip.getStartStop()) <= nextTrip.getStartTime()) {

    //                         if (i != 0) {
    //                             List<ChargingEvent> toDelete = new ArrayList<>();
    //                             for (int j = i + 1; j < elements.size(); j++) {
    //                                 if (elements.get(j) instanceof ChargingEvent charger) {
    //                                     toDelete.add(charger);
    //                                 } else {
    //                                     break;
    //                                 }
    //                             }
    //                             for (ChargingEvent charger : toDelete) {
    //                                 batteryStatus -= charger.getEnergy();
    //                             }
    //                             elements.removeAll(toDelete);
    //                             freeChargers.addAll(toDelete);
    //                         }
    
                            
    //                         ;
    //                         addChargingEventsAtCurrTripEndStop(newTrip.getStartTime(), freeChargers, currTrip);
    //                         elements.add(newTrip);
    //                         elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
    //                         batteryStatus -= energyConsumption;
    //                         return true;
    //             }
                
    //         }
    //     }
    //     return false;
    // }

    public boolean addTrip(Trip newTrip, List<ChargingEvent> freeChargers) {
        double currBattery = StaticData.MAX_BATTERY;

        for (int i = 0; i < elements.size() - 1; i++) {
            if (elements.get(i) instanceof Trip currTrip) {

                currBattery -= currTrip.getEnergy();

                TurnusElement nextElement = elements.get(i + 1);
                int nextStartStop = 0;
    
                if (nextElement instanceof Trip nextTrip) {
                    nextStartStop = nextTrip.getStartStop();
                } else if (nextElement instanceof ChargingEvent nextCharger) {
                    nextStartStop = nextCharger.getStop();
                }
    
                int arrivalTime = currTrip.getEndTime()
                        + StaticData.getTravelTime(currTrip.getEndStop(), newTrip.getStartStop());
                double energyConsumption = StaticData.getDeadheadEnergy(currTrip.getEndStop(), newTrip.getStartStop())
                        + newTrip.getEnergy() 
                        + StaticData.getDeadheadEnergy(newTrip.getEndStop(), nextStartStop)
                        - StaticData.getDeadheadEnergy(currTrip.getEndStop(), nextStartStop);
    
                if (arrivalTime <= newTrip.getStartTime()
                        && batteryStatus - energyConsumption >= StaticData.MIN_BATTERY
                        && currBattery - energyConsumption >= StaticData.MIN_BATTERY
                        && newTrip.getEndTime() + StaticData.getTravelTime(newTrip.getEndStop(), nextStartStop) <= nextElement.getStartTime()) {
    
                    elements.add(i + 1, newTrip);
                    batteryStatus -= energyConsumption;
                    currBattery -= energyConsumption;
                    addChargingEvents(arrivalTime, freeChargers, newTrip, currBattery);
                    return true;
                }
                currBattery -= StaticData.getDeadheadEnergy(currTrip.getEndStop(), nextStartStop);
            } else if (elements.get(i) instanceof ChargingEvent currCharger) {
                currBattery += currCharger.getEnergy();

            }
        }
    
        // Trip lastTrip = getLastTrip();
        // int arrivalTime = lastTrip.getEndTime()
        //         + StaticData.getTravelTime(lastTrip.getEndStop(), newTrip.getStartStop());
        // double energyConsumption = StaticData.getDeadheadEnergy(lastTrip.getEndStop(), newTrip.getStartStop())
        //         + newTrip.getEnergy();
    
        // if (arrivalTime <= newTrip.getStartTime()
        //         && batteryStatus - energyConsumption >= StaticData.MIN_BATTERY) {
        //     elements.add(newTrip);
        //     batteryStatus -= energyConsumption;
        //     addChargingEvent(arrivalTime, freeChargers, newTrip);
        //     return true;
        // }
    
        return false;

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /// 
        /// stare - pridavanie IBA na koniec

        // Trip lastTrip = getLastTrip();
        // int arrivalTime = lastTrip.getEndTime() +
        // StaticData.getTravelTime(lastTrip.getEndStop(), newTrip.getStartStop());
        // double energyConsumption =
        // StaticData.getDeadheadEnergy(lastTrip.getEndStop(), newTrip.getStartStop()) +
        // newTrip.getEnergy();

        // if (arrivalTime <= newTrip.getStartTime()
        // && batteryStatus - energyConsumption >= StaticData.MIN_BATTERY *
        // StaticData.BATTERY_CAPACITY
        // && batteryStatus - energyConsumption -
        // StaticData.getDeadheadEnergy(newTrip.getEndStop(),
        // StaticData.depoEnd.getStartStop()) >= 0
        // )
        // {
        // elements.add(newTrip);
        // batteryStatus -= energyConsumption;
        // addChargingEvent(arrivalTime, freeChargers, newTrip);
        // return true;
        // }
        // return false;
    }

    public void addChargingEvents(int arrivalTime, List<ChargingEvent> freeChargers, Trip trip, double currBattery) {
        List<ChargingEvent> candidates = new ArrayList<>();

        for (ChargingEvent chargingEvent : freeChargers) {
            if (!candidates.isEmpty()
                    && chargingEvent.getCharger() != candidates.get(candidates.size() - 1).getCharger()) {
                break;
            }

            if (chargingEvent.getStop() == trip.getStartStop()
                    && chargingEvent.getStartTime() >= arrivalTime
                    && chargingEvent.getEndTime() <= trip.getStartTime()
                    && chargingEvent.getEnergy() + currBattery <= StaticData.MAX_BATTERY) {
                candidates.add(chargingEvent);
                batteryStatus += chargingEvent.getEnergy();
                currBattery += chargingEvent.getEnergy();
            }
        }

        for (ChargingEvent candidate : candidates) {
            freeChargers.remove(candidate);
            elements.add(candidate);
        }
        elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
    }

    // asi nedava zmysel
    // public void addChargingEventsAtCurrTripEndStop(int newTripStartTime, List<ChargingEvent> freeChargers, Trip oldTrip) {
    //     List<ChargingEvent> candidates = new ArrayList<>();

    //     for (ChargingEvent chargingEvent : freeChargers) {
    //         if (!candidates.isEmpty()
    //                 && chargingEvent.getCharger() != candidates.get(candidates.size() - 1).getCharger()) {
    //             break;
    //         }

    //         if (chargingEvent.getStop() == oldTrip.getEndStop()
    //                 && chargingEvent.getStartTime() >= oldTrip.getEndTime()
    //                 && chargingEvent.getEnergy() + batteryStatus <= StaticData.MAX_BATTERY
    //                 && chargingEvent.getEndTime() <= newTripStartTime) {

    //             candidates.add(chargingEvent);
    //             batteryStatus += chargingEvent.getEnergy();
    //         }
    //     }

    //     for (ChargingEvent candidate : candidates) {
    //         freeChargers.remove(candidate);
    //         elements.add(candidate);
    //         //elements.sort(Comparator.comparingInt(TurnusElement::getStartTime));
    //     }
    // }

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
        sb.append(String.format("%-100s", elements.get(0))).append("\tBattery: ").append(String.format("%3.2f", currBattery)).append("\n");

        for (int i = 1; i < elements.size(); i++) {
            TurnusElement prevElement = elements.get(i - 1);
            TurnusElement currElement = elements.get(i);

            int prevStop = 0;
            int currStop = 0;

            if (prevElement instanceof Trip prevTrip) {
                prevStop = prevTrip.getEndStop();
            } else if (prevElement instanceof ChargingEvent prevCharger) {
                prevStop = prevCharger.getStop();
            }

            if (currElement instanceof Trip currTrip) {
                currBattery -= currTrip.getEnergy();
                currStop = currTrip.getStartStop();
            } else if (currElement instanceof ChargingEvent currCharger) {
                currBattery += currCharger.getEnergy();
                currStop = currCharger.getStop();
            }

            currBattery -= StaticData.getDeadheadEnergy(prevStop, currStop);

            sb.append(String.format("%-100s", currElement));
            sb.append("\tBattery: ").append(String.format("%7.3f", currBattery)).append("\n");
        }

        //sb.append("Battery Status: ").append(batteryStatus).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
