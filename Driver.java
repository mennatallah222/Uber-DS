import java.net.Socket;

public class Driver extends User {
    private Socket socket;

    public Driver(int id, String username, String password, Socket socket) {
        super(id, username, password, "Driver");
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }
}