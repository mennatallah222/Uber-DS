public class Ride {
    private static int nextId = 1;
    private int rideId;
    private int customerId;
    private int driverId;
    private String pickupLocation;
    private String destination;
    private String status;

    public Ride(int cid, String pickupLocation, String destination) {
        this.rideId = nextId++;
        this.customerId = cid;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.status = "on the way";
    }

    public void setDriverID(int did) {
        this.driverId = did;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public int getRideId() {
        return rideId;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("Ride ID: %d | Customer: %d | Driver: %d | Pickup location: %s | Destination: %s | Status: %s", rideId, customerId, driverId, pickupLocation, destination, status);
    }

    public int getCustomerId() {
        return customerId;
    }
}
