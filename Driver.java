import java.net.Socket;

public class Driver {
    private int id;
    private String username;
    private String password;
    private Socket socket;

    public Driver(int id, String username, String password, Socket socket){
        this.id=id;
        this.username = username;
        this.password = password;
        this.socket = socket;
    }
    public int getID(){
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public Socket getSocket() {
        return socket;
    }
}
