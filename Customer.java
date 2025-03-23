import java.net.Socket;

public class Customer extends User {
    private Socket socket;
    private String pickupLocation;
    private String destination;

    public Customer(int id, String username, String password, Socket socket) {
        super(id, username, password, "Customer");
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
