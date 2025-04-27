package data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import enums.ChargingStrategy;
import enums.Season;
import model.ChargingEvent;
import model.Trip;

public class StaticData {
    public static HashMap<Integer, Integer> stopIdToIndex;

    public static HashMap<Integer, HashSet<Integer>> stopToChargers;
    public static HashMap<Integer, ArrayList<ChargingEvent>> chargerToEvents;

    public static List<Trip> trips;
    public static Trip depoStart;
    public static Trip depoEnd;

    public static double[][] matrixKm;
    public static int[][] matrixTime;

    public static ChargingStrategy CHARGING_STRATEGY = ChargingStrategy.WHEN_POSSIBLE;
    public static Season SEASON = Season.SPRING;
    public static double CONSUMPTION_PER_KM = SEASON == Season.SPRING ? 1.5 : 2.0;
    public static double MIN_BATTERY = 0;
    public static double MAX_BATTERY = SEASON == Season.WINTER ? 100.0 : 125.0;

    public static double getDeadheadEnergy(int from, int to) {
        if (from == to) {
            return 0;
        }
        return getTravelDistance(from, to) * CONSUMPTION_PER_KM;
    }

    public static int getTravelTime(int from, int to) {
        if (from == to) {
            return 0;
        }
        return matrixTime[from][to];
    }

    public static double getTravelDistance(int from, int to) {
        if (from == to) {
            return 0;
        }
        return matrixKm[from][to];
    }

    public static HashMap<Integer, HashSet<Integer>> getStopToChargersCopy() {
        HashMap<Integer, HashSet<Integer>> copy = new HashMap<>();

        for (Map.Entry<Integer, HashSet<Integer>> entry : StaticData.stopToChargers.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        return copy;
    }

    public static HashMap<Integer, ArrayList<ChargingEvent>> getChargerToEventsCopy() {
        HashMap<Integer, ArrayList<ChargingEvent>> copy = new HashMap<>();

        for (Map.Entry<Integer, ArrayList<ChargingEvent>> entry : StaticData.chargerToEvents.entrySet()) {
            ArrayList<ChargingEvent> copiedList = new ArrayList<>();
            
            for (ChargingEvent event : entry.getValue()) {
                copiedList.add(new ChargingEvent(event));
            }
            copy.put(entry.getKey(), copiedList);
        }
        
        return copy;
    }
}
