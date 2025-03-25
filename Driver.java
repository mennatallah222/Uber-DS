import java.io.PrintWriter;
import java.net.Socket;

public class Driver extends User {
    private Socket socket;
    private PrintWriter writer;

    public Driver(int id, String username, String password, Socket socket) {
        super(id, username, password, "Driver");
        this.socket = socket;
        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public Socket getSocket() {
        return socket;
    }
}
