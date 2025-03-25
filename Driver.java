import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;

public class Driver extends User {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Driver(int id, String username, String password, Socket socket) {
        super(id, username, password, "Driver");
        this.socket = socket;
        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e) {
            System.out.println("Error initializing streams: " + e.getMessage());
        }
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public Socket getSocket() {
        return socket;
    }
}
