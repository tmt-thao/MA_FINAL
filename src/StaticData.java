import java.util.HashMap;
import java.util.List;

public class StaticData {
    public static HashMap<Integer, Integer> stopIdToIndex;
    public static List<Trip> trips;
    public static List<ChargingEvent> chargingEvents;

    public static Trip depoStart;
    public static Trip depoEnd;

    public static double[][] matrixKm;
    public static int[][] matrixTime;

    public static ChargingStrategy chargingStrategy = ChargingStrategy.AT_END_STOP;
    public static double CONSUMPTION_PER_KM = 1.5;    // 1.5, 2, 2
    public static double MIN_BATTERY = 0;
    public static double MAX_BATTERY = 125.0;             // 125, 125, 100

    public static double getDeadheadEnergy(int from, int to) {
        return getTravelDistance(from, to) * CONSUMPTION_PER_KM;
    }

    public static int getTravelTime(int from, int to) {
        return matrixTime[from][to];
    }

    public static double getTravelDistance(int from, int to) {
        return matrixKm[from][to];
    }

    public static Trip getTripById(int id) {
        for (Trip trip : trips) {
            if (trip.getId() == id) {
                return trip;
            }
        }
        return null;
    }
}
