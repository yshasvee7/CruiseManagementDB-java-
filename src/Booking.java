public class Booking {
    private int bookingId;
    private int passengerId;
    private int cruiseId;
    private String cabinClass;
    private double price;

    public Booking(int passengerId, int cruiseId, String cabinClass, double price) {
        this.passengerId = passengerId;
        this.cruiseId = cruiseId;
        this.cabinClass = cabinClass;
        this.price = price;
    }

    public int getPassengerId() { return passengerId; }
    public int getCruiseId() { return cruiseId; }
    public String getCabinClass() { return cabinClass; }
    public double getPrice() { return price; }
}
