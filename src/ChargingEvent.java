public class ChargingEvent implements TurnusElement {
    private int id;
    private int charger;
    private int stop;
    private int startTime;
    private int endTime;
    private double chargingSpeed;


    public ChargingEvent(int id, int charger, int stop, int startTime, int endTime, double chargingSpeed) {
        this.id = id;
        this.charger = charger;
        this.stop = stop;
        this.startTime = startTime;
        this.endTime = endTime;
        this.chargingSpeed = chargingSpeed;
    }

    public ChargingEvent(ChargingEvent other) {
        this.id = other.id;
        this.charger = other.charger;
        this.stop = other.stop;
        this.startTime = other.startTime;
        this.endTime = other.endTime;
        this.chargingSpeed = other.chargingSpeed;
    }
    

    public int getId() {
        return id;
    }

    public int getCharger() {
        return charger;
    }

    public int getStop() {
        return stop;
    }

    public double getChargingSpeed() {
        return chargingSpeed;
    }

    @Override
    public int getStartTime() {
        return startTime;
    }

    @Override
    public int getEndTime() {
        return endTime;
    }

    @Override
    public int getDuration() {
        return endTime - startTime;
    }

    @Override
    public double getEnergy() {
        return (double) getDuration() * chargingSpeed;
    }

    @Override
    public String toString() {
        return "ChargingEvent{" +
                "id=" + id +
                ", charger=" + charger +
                ", stop=" + stop +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", chargingSpeed=" + chargingSpeed +
                '}';
    }
}
