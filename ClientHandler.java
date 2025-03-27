import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Customer assignedCustomer;
    private boolean inRide = false;
    private Ride currRide;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            while (true) {
                writer.println("Do you want to 'register' or 'login'?");
                writer.flush();
                String choice = reader.readLine();

                if (choice == null) break;

                if (choice.equalsIgnoreCase("register")) {
                    Authentication.register(reader, writer);
                }
                else if (choice.equalsIgnoreCase("login")) {
                    User user = Authentication.login(reader, writer);
                    if (user == null) {
                        writer.println("User not registered or invalid username/password, please try again");
                        writer.flush();
                        continue;
                    }
                    if(user.getUsername().equalsIgnoreCase("Admin")&&user.getPassword().equals("123")){
                        handleAdmin();
                    }
                    else{
                        int id = Server.getUserID(user.getUsername());

                        if (user.getType().equalsIgnoreCase("Customer")) {
                            Customer customer = new Customer(id, user.getUsername(), user.getPassword(), socket);
                            Server.addCustomer(customer);
                            handleCustomer(customer);
                        } else if (user.getType().equalsIgnoreCase("Driver")) {
                            Driver driver = new Driver(id, user.getUsername(), user.getPassword(), socket);
                            Server.addDriver(driver, socket);
                            handleDriver(driver);
                        }
                    }
            }
            else {
                    writer.println("Invalid choice! Please enter 'register' or 'login'");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection issue: " + e.getMessage());
        }
    }

    private void handleAdmin() {
        try {
            writer.println("Welcome, Admin!");
            while (true) {
                writer.println("Enter 'stats' to view statistics, or 'disconnect' to exit");
                writer.flush();
                String message = reader.readLine();
                if (message == null) break;
                if (message.equalsIgnoreCase("disconnect")) {
                    writer.println("Disconnected successfully");
                    socket.close();
                    break;
                }

                if (message.equalsIgnoreCase("stats")) {
                    String statistics = Server.getStatistics();
                    
                    if (statistics.isEmpty()) {
                        writer.println("No statistics available");
                        writer.flush();
                    }
                    else {
                        writer.println(statistics);
                        writer.flush();
                    }
                    String rides = Server.getAllRides();
                    writer.println(rides);
                    writer.flush();
                }
                else {
                    writer.println("Invalid command. Enter 'stats', 'rides', or 'disconnect'");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Admin disconnected: " + e.getMessage());
        }
    }


    private void handleCustomer(Customer c) {
        try {
            writer.println("Welcome, " + c.getUsername() + "! Enter 'disconnect' to exit or 'request a ride'");
            writer.flush();
            while (true) {
                String message = reader.readLine();
                if (message == null) break;
    
                if (message.equalsIgnoreCase("disconnect")) {
                    if (c.isInRide()) {
                        writer.println("You cannot disconnect during an ongoing ride!");
                    }
                    else {
                        Server.removeClient(c);
                        Server.removeWaitingCustomer(c);
                        writer.println("Disconnected successfully");
                        socket.close();
                        break;
                    }
                }
    
                if (message.equalsIgnoreCase("request a ride")) {
                    writer.println("Enter pickup location:");
                    String pickupLocation = reader.readLine();
                    writer.println("Enter destination:");
                    String destination = reader.readLine();

                    c.setPickupLocation(pickupLocation);
                    c.setDestination(destination);
                    
                    if (Server.availableDrivers.isEmpty()) {
                        writer.println("No available drivers. Try again later");
                    }
                    else {
                        Server.addWaitingCustomer(c, writer);
                        Server.broadcast("Customer '" + c.getUsername() + "' requests a ride from '" + c.getPickupLocation() + "' to '" +c.getDestination()+"'");
                        writer.println("We notified drivers. Please wait...");
                    }
                }

            }
        } catch (IOException e) {
            System.out.println("Error handling customer: " + e.getMessage());
        }
    }
    
    private void handleDriver(Driver driver) {
        try {
            writer.println("Welcome, " + driver.getUsername() + "! Enter 'disconnect' to exit or 'offer a ride'");

            while (true) {
                String message = reader.readLine();
                if (message == null) break;

                if (message.equalsIgnoreCase("disconnect")) {
                    if (inRide) {
                        writer.println("You cannot disconnect during an ongoing ride!");
                    } else {
                        Server.removeClient(driver);
                        Server.removeAvailableDrivers(driver.getUsername());
                        writer.println("Disconnected successfully");
                        socket.close();
                        break;
                    }
                }

                if (message.equalsIgnoreCase("offer a ride")) {
                    Server.addAvailableDrivers(driver, socket);
                    writer.println("You're now available for rides.");

                    if (Server.waitingCustomers.isEmpty()) {
                        writer.println("No ride requests available.");
                        continue;
                    }

                    for (Customer c : Server.waitingCustomers.keySet()) {
                        writer.println("Customer: " + c.getUsername() +
                                " - Pickup: " + c.getPickupLocation() +
                                " - Destination: " + c.getDestination());
                    }

                    writer.println("Enter the username of the customer you want to accept:");
                    String selectedCustomer = reader.readLine();

                    Customer chosenCustomer = null;
                    for (Customer c : Server.waitingCustomers.keySet()) {
                        if (c.getUsername().equalsIgnoreCase(selectedCustomer)) {
                            chosenCustomer = c;
                            break;
                        }
                    }

                    if (chosenCustomer != null) {
                        assignedCustomer = chosenCustomer;
                        inRide=true;
                        assignedCustomer.setInRide(true);
                        currRide=new Ride(chosenCustomer.getId(), chosenCustomer.getPickupLocation(), chosenCustomer.getDestination());
                        currRide.setDriverID(driver.getId());
                        Server.addRide(currRide);
                        Server.updateRideStatus(currRide.getRideId(), "on the way", driver.getId());
                        Server.handleRideOffer(chosenCustomer, driver);
                        notifyCustomerRideStatus("Driver '" + driver.getUsername() + "' is on the way!");
                        handleRideUpdates(driver, chosenCustomer);
                    }
                    else{
                        writer.println("Customer not found or already assigned");
                    }
                    
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling driver: " + e.getMessage());
        }
    }


    private void handleRideUpdates(Driver driver, Customer customer) {
        try {
            while (true) {
                writer.println("Enter ride status ('started', 'completed') or 'disconnect' to exit:");
                String status = reader.readLine();

                if (status == null) break;

                if (status.equalsIgnoreCase("disconnect")) {
                    writer.println("You cannot disconnect during an ongoing ride!");
                    continue;
                }

                Server.updateRideStatus(currRide.getRideId(), status, driver.getId());
                writer.println("Ride status updated: " + status);
                customer.getWriter().println("Your ride status: " + status);

                if (status.equalsIgnoreCase("completed")) {
                    inRide = false;
                    assignedCustomer.setInRide(false);
                    Server.addAvailableDrivers(driver, socket);
                    writer.println("Ride completed! You are now available for new rides.");
                    //review
                    customer.getWriter().println("Please provide your rating for the driver:");
                    int rideComfort = getValidRating(customer, "Ride Comfort (1-5):");
                    int attitude = getValidRating(customer, "Driver Attitude (1-5):");
                    int cleanliness = getValidRating(customer, "Car Cleanliness (1-5):");
                    int safety = getValidRating(customer, "Safety (1-5):");

                    customer.getWriter().println("Enter any additional comments:");
                    String comment = customer.getReader().readLine();

                    driver.addReview(rideComfort, attitude, cleanliness, safety, comment);
                    customer.getWriter().println("Thank you for your rating! Driver's new average rating: " + driver.getAvgRating());
                    writer.println("Your updated average rating: " + driver.getAvgRating());

                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private int getValidRating(Customer customer, String message) throws IOException {
        while (true) {
            customer.getWriter().println(message);
            String input = customer.getReader().readLine().trim();

            if (input.matches("[1-5]")) {
                return Integer.parseInt(input);
            }
            else{
                customer.getWriter().println("Please enter a single number between 1 and 5");
            }
        }
    }
    

    private void notifyCustomerRideStatus(String message) {
        if (assignedCustomer != null) {
            PrintWriter customerWriter = Server.waitingCustomers.get(assignedCustomer);
            if (customerWriter != null) {
                customerWriter.println(message);
            }
        }
    }
}
