import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        // 140, 0.8
        // JAR
        // T:   5,    10,     12,     29
        // B:   4,    4,      5,      8,      9,      13,     30
        // A:   62

        // 140, 1.08
        // LETO
        // T:   5,    10,     12,     34
        // B:   4,    4,      6,      8,      10,      15,     37
        // A:   89

        // 105, 1.08
        // ZIMA
        // T:   5,    11,     13,     36
        // B:   4,    5,      6,      9,      11,      17,     46
        // A:   105

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        DataLoader.loadStopIdToIndex("data/ZastavkyAll.csv");
        DataLoader.loadMatrixKm("data/matrixKm.txt", StaticData.stopIdToIndex.size());
        DataLoader.loadMatrixTime("data/matrixTime.txt", StaticData.stopIdToIndex.size());

        String version = "B3_3";
        DataLoader.loadChargingEvents("data/ChEvents_" + version + ".csv");
        DataLoader.loadTrips("data/spoje_id_" + version + ".csv");

        MemeticAlgorithm ma = new MemeticAlgorithm(5, 500, 0.8, 10, 0.8, 10);
        ma.run();
        // 5, 500, 0.8, 10, 0.8, 10
        // JAR
        // T:   5,    10,     13,     34
        // B:   4,    4,      5,      8,     9,      17,    44
        // A:   100
    }
}
