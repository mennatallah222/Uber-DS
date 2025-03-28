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
    private static final Map<Integer, Ride> rides = new HashMap<>();
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

    public static int getTotalCustomers() {
        return customers.size();
    }
    public static int getTotalDrivers() {
        return drivers.size();
    }
    public static String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Total Customers: ").append(customers.size()).append("\n");
        stats.append("Total Drivers: ").append(drivers.size()).append("\n");
        for (Driver driver : drivers) {
            stats.append("Driver: ").append(driver.getUsername()).append(" | Avg Rating: ").append(driver.getAvgRating()).append("\n");
            if (!driver.getReviews().isEmpty()) {
                stats.append("Reviews:\n");
                for (String review : driver.getReviews()) {
                    stats.append(" - ").append(review).append("\n");
                }
            }
            else {
                stats.append("No reviews available\n");
            }
        }

        return stats.toString();
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

    public static void addRide(Ride ride) {
        rides.putIfAbsent(ride.getRideId(), ride);
    }


    public static String getAllRides() {
        if (rides.isEmpty()) return "No rides available.\nEND";

        StringBuilder sb = new StringBuilder();
        for (Ride ride : rides.values()) {
            sb.append(ride.toString()).append("\n");
        }
        sb.append("END\n");
        return sb.toString();
    }
    public static Ride getRideById(int rideId) {
        return rides.get(rideId);
    }
    public static Ride getRideByCustomer(int customerId) {
        for (Ride ride : rides.values()) {
            if (ride.getCustomerId() == customerId) {
                return ride;
            }
        }
        return null;
    }
    public static void updateRideStatus(int rideId, String status, int did) {
        for (Ride ride : rides.values()){
            if (ride.getRideId() == rideId) {
                ride.updateStatus(status);
                break;
            }
        }
    }
    public static synchronized void handleRideOffer(Customer customer, Driver driver) {
        PrintWriter customerWriter = waitingCustomers.get(customer);
        if (customerWriter == null) return;

        try {
            customerWriter.println("Driver " + driver.getUsername() + " has accepted your ride. ");
            customerWriter.println("Do you accept? (yes/no)  please confrim 2 timed on the next line after this message");
            BufferedReader customerReader = new BufferedReader(new InputStreamReader(customer.getSocket().getInputStream()));
            String response = customerReader.readLine();

            PrintWriter driverWriter = new PrintWriter(driver.getSocket().getOutputStream(), true);

            if (response == null) {
                customerWriter.println("No response received. Ride canceled");
                driverWriter.println("Customer did not respond, try again please");
                return;
            }
            if (response.trim().equalsIgnoreCase("yes")) {
                // rides.put(r.getRideId(), r);
                customerWriter.println("Ride is confirmed! The driver is on the way.");
                driverWriter.println("Ride accepted! Proceed to pick up the customer");
                driverWriter.println("Head to: " + customer.getPickupLocation() + " to pick up: " + customer.getUsername());

                driver.setInRide(true);
                driver.setAssignedCustomer(customer);
                Server.removeAvailableDrivers(driver.getUsername());
                Server.removeWaitingCustomer(customer);
            }
            else {
                customerWriter.println("Ride declined. Searching for another driver...");
                driverWriter.println("The customer declined. Try another request");

                driver.setInRide(false);
                driver.setAssignedCustomer(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final Map<Integer, List<Integer>> driverRatings = new HashMap<>();

    public static synchronized void addDriverRating(int driverId, int rating) {
        driverRatings.putIfAbsent(driverId, new ArrayList<>());
        driverRatings.get(driverId).add(rating);
    }

    public static synchronized double getDriverAverageRating(int driverId) {
        List<Integer> ratings = driverRatings.get(driverId);
        if (ratings == null || ratings.isEmpty()) return 0.0;
        
        return ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

}