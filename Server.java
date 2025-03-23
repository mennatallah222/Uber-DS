import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = Config.PORT;
    private static final List<Customer> customers = new ArrayList<>();
    private static final List<Driver> drivers = new ArrayList<>();
    private static final Map<String, User> registeredUsers = new HashMap<>();
    private static int id = 1;

    private static final String ADMIN_USERNAME = "Admin";
    private static final String ADMIN_PASSWORD = "123";

    public static void main(String[] args) {
        registeredUsers.put(ADMIN_USERNAME, new User(incrementID(), ADMIN_USERNAME, ADMIN_PASSWORD, "Admin"));
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized int incrementID() {
        return id++;
    }

    public static synchronized void addCustomer(Customer customer) {
        customers.add(customer);
    }

    public static synchronized void addDriver(Driver driver) {
        drivers.add(driver);
    }

    public static synchronized void removeClient(Object client) {
        if (client instanceof Customer) customers.remove(client);
        if (client instanceof Driver) drivers.remove(client);
    }

    public static synchronized boolean register(String username, String password, String type) {
        if (registeredUsers.containsKey(username)) return false;
        int userId = incrementID();
        registeredUsers.put(username, new User(userId, username, password, type));
        return true;
    }

    public static synchronized User login(String username, String password) {
        User user = registeredUsers.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public static synchronized int getUserID(String username) {
        User user = registeredUsers.get(username);
        if (user != null) {
            return user.getId();
        }
        return -1;
    }

    public static List<Driver> getAvailableDrivers() {
        return new ArrayList<>(drivers);
    }
}