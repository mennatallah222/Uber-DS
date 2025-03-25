import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

public class Customer extends User {
    private Socket socket;
    private String pickupLocation;
    private String destination;
    private BufferedReader reader;

    public Customer(int id, String username, String password, Socket socket) {
        super(id, username, password, "Customer");
        this.socket = socket;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error initializing input stream: " + e.getMessage());
        }
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


    public BufferedReader getReader() {
        return reader;
    }
}
