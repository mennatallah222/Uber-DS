import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Customer assignedCustomer; 
    private boolean inRide = false;   

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
                String choice = reader.readLine();

                if (choice == null) break;

                if (choice.equalsIgnoreCase("register")) {
                    Authentication.register(reader, writer);
                } else if (choice.equalsIgnoreCase("login")) {
                    User user = Authentication.login(reader, writer);
                    if (user == null) {
                        writer.println("User not registered or invalid username/password, please try again.");
                        continue;
                    }
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
                } else {
                    writer.println("Invalid choice! Please enter 'register' or 'login'");
                }
            }
        } catch (IOException e) {
            System.out.println("Connection issue: " + e.getMessage());
        }
    }

    private void handleCustomer(Customer c) {
        try {
            writer.println("Welcome, " + c.getUsername() + "! Enter 'disconnect' to exit or 'request a ride' to begin.");

            while (true) {
                String message = reader.readLine();
                if (message == null) break;

                if (message.equalsIgnoreCase("disconnect")) {
                    if (inRide) {
                        writer.println("You cannot disconnect during an ongoing ride!");
                    } else {
                        Server.removeClient(c);
                        Server.removeWaitingCustomer(c);
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
                        writer.println("No available drivers. Try again later.");
                    } else {
                        Server.addWaitingCustomer(c, writer);
                        Server.broadcast("Customer '" + c.getUsername() + "' requests a ride from '" +
                                c.getPickupLocation() + "' to '" + c.getDestination() + "'.");
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
            writer.println("Welcome, " + driver.getUsername() + "! Enter 'disconnect' to exit or 'offer a ride' to begin.");

            while (true) {
                String message = reader.readLine();
                if (message == null) break;

                if (message.equalsIgnoreCase("disconnect")) {
                    if (inRide) {
                        writer.println("You cannot disconnect during an ongoing ride!");
                    } else {
                        Server.removeClient(driver);
                        Server.removeAvailableDrivers(driver.getUsername());
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
                        inRide = true;
                        Server.handleRideOffer(chosenCustomer, driver);
                        notifyCustomerRideStatus("Driver '" + driver.getUsername() + "' is on the way!");
                        handleRideUpdates(driver, chosenCustomer);
                    } else {
                        writer.println("Customer not found or already assigned.");
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
                writer.println("Enter ride status ('on the way', 'ride started', 'ride completed'):");
                String status = reader.readLine();

                if (status == null) break;

                switch (status.toLowerCase()) {
                    case "on the way":
                        notifyCustomerRideStatus("Your driver '" + driver.getUsername() + "' is on the way.");
                        break;
                    case "ride started":
                        notifyCustomerRideStatus("Your ride has started.");
                        break;
                    case "ride completed":
                        notifyCustomerRideStatus("Your ride is completed. Thank you!");
                        inRide = false; // Allow disconnection
                        Server.removeWaitingCustomer(customer);
                        Server.removeAvailableDrivers(driver.getUsername());
                        return; // End ride
                    default:
                        writer.println("Invalid status! Please use 'on the way', 'ride started', or 'ride completed'.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling ride updates: " + e.getMessage());
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
