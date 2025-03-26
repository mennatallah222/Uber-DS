import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = Config.PORT;
    private static final List<Customer> customers = new ArrayList<>();
    private static final List<Driver> drivers = new ArrayList<>();
    private static final Map<String, User> registeredUsers = new HashMap<>();
    public static final Map<String, Socket> availableDrivers=new HashMap<>();
    public static final Map<Customer, PrintWriter> waitingCustomers=new HashMap<>();
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

    public static synchronized void addDriver(Driver driver, Socket s) {
        drivers.add(driver);
        availableDrivers.put(driver.getUsername(), s);
    }

    public static synchronized void removeClient(Object client) {
        if (client instanceof Customer) customers.remove(client);
        if (client instanceof Driver){
             drivers.remove(client);
            availableDrivers.remove(((Driver)client).getUsername()); 
        }
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
    public static synchronized void addAvailableDrivers(Driver d, Socket s){
        availableDrivers.put(d.getUsername(), s);
    }
    public static synchronized void removeAvailableDrivers(String driver){
        availableDrivers.remove(driver);
    }
    public static synchronized void addWaitingCustomer(Customer c, PrintWriter w){
        waitingCustomers.put(c, w);
    }
    public static synchronized Customer getWaitingCustomer(){
        if(!waitingCustomers.isEmpty()) return waitingCustomers.keySet().iterator().next();
        return null;
    }
    public static synchronized void removeWaitingCustomer(Customer customer){
        waitingCustomers.remove(customer);
    }
    public static synchronized Socket getavailableDriver(){
        if(!availableDrivers.isEmpty()){
            String d=availableDrivers.keySet().iterator().next();
            return availableDrivers.remove(d);
        }
        return null;
    }
    public static synchronized void broadcast(String msg){
        for(Map.Entry<String, Socket> val:availableDrivers.entrySet()){
            try{
                PrintWriter w=new PrintWriter(val.getValue().getOutputStream(), true);
                w.println(msg);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static synchronized void handleRideOffer(Customer customer, Driver driver) {
        PrintWriter customerWriter = waitingCustomers.get(customer);
        PrintWriter driverWriter = null;
        if (customerWriter == null) return;
        try {
            customerWriter.println("Driver " + driver.getUsername() + " has accepted your ride. Do you accept? (yes/no)");

            BufferedReader customerReader = new BufferedReader(new InputStreamReader(customer.getSocket().getInputStream()));
            String response = customerReader.readLine();

            driverWriter = new PrintWriter(driver.getSocket().getOutputStream(), true);

            if (response == null) {
                customerWriter.println("No response received. Ride canceled.");
                driver.getWriter().println("Customer did not respond. Try another request.");
                return;
            }

            if (response.trim().equalsIgnoreCase("yes")) {
                customerWriter.println("Ride confirmed! The driver is on the way");
                customerWriter.flush();

                driver.getWriter().println("Ride accepted! Proceed to pick up the customer");
                Server.removeWaitingCustomer(customer);

                driverWriter.println("Head to: " + customer.getPickupLocation() + " to pick up: " + customer.getUsername());
                driverWriter.flush();
            } else {
                customerWriter.println("Ride declined. Searching for another driver...");
                driver.getWriter().println("The customer declined. Try another request.");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


}