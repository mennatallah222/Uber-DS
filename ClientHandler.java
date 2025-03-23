import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

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
                }
                else if (choice.equalsIgnoreCase("login")) {
                    User user = Authentication.login(reader, writer);
                    if (user == null) {
                        writer.println("User not registered or invalid username/password, please try again");
                        continue;
                    }
                    int id = Server.getUserID(user.getUsername());

                    if (user.getType().equalsIgnoreCase("Customer")) {
                        Customer customer = new Customer(id, user.getUsername(), user.getPassword(), socket);
                        Server.addCustomer(customer);
                        handleCustomer(customer);
                    }
                    else if (user.getType().equalsIgnoreCase("Driver")) {
                        Driver driver = new Driver(id, user.getUsername(), user.getPassword(), socket);
                        Server.addDriver(driver, socket);
                        handleDriver(driver);
                    }
                }
                else {
                    writer.println("Invalid choice! Please enter 'register' or 'login'");
                }
            }
        }
        catch (IOException e) {
            System.out.println("Connection issue: " + e.getMessage());
        }
    }

    private void handleCustomer(Customer c) throws IOException {
        writer.println("Welcome, " + c.getUsername() + "! You can request a ride now! Enter 'disconnect' or 'request a ride'");
        while (true) {
            String message = reader.readLine();
            if (message == null || message.equalsIgnoreCase("disconnect")) {
                Server.removeClient(c);
                Server.removeWaitingCustomer(c);
                break;
            }
            if (message.equalsIgnoreCase("request a ride")) {
                writer.println("Enter pickup location:");
                String pickupLocation = reader.readLine();
                writer.println("Enter destination:");
                String destination = reader.readLine();

                c.setPickupLocation(pickupLocation);
                c.setDestination(destination);
                if(Server.availableDrivers.isEmpty())
                    writer.println("There are no available drivers!, try again later");
                    else{
                        Server.addWaitingCustomer(c, writer);
                        Server.broadcast("Customer '"+c.getUsername()+"' is requesting a ride from '"+c.getPickupLocation()+"' to '"+c.getDestination() + "', please enter 'accept' to accept the ride");
                        writer.println("We notified our drivers for your request! please wait for a driver to accept...");
                    }
            }
        }
    }

    private void handleDriver(Driver driver) throws IOException {
        writer.println("Welcome, " + driver.getUsername() + "! You can offer rides! Enter 'disconnect' or 'offer a ride'");
        while (true) {
            String message = reader.readLine();
            if (message == null || message.equalsIgnoreCase("disconnect")) {
                Server.removeClient(driver);
                Server.removeAvailableDrivers(driver.getUsername());
                break;
            }
            if (message.equalsIgnoreCase("offer a ride")) {
                Server.addAvailableDrivers(driver, socket);
                writer.println("You're now an available driver");
                for(Object c:Server.waitingCustomers.keySet()){
                    Customer c2=(Customer)c;
                    writer.println("Customer '"+c2.getUsername()+"' is requesting a ride from '"+c2.getPickupLocation()+"' to '"+c2.getDestination()+"' , enter 'accept' to accept it");
                }
            }
            if (message.equalsIgnoreCase("accept")) {
                Customer c=Server.getWaitingCustomer();
                if (c != null) {
                    PrintWriter customerWriter = Server.waitingCustomers.get(c);
                    if (customerWriter != null) {
                        customerWriter.println("A driver has accepted your ride request! The driver is on the way");
                        Server.removeWaitingCustomer(c);
                        writer.println("You have been assigned to a customer");
                    }
                }
                else{
                    writer.println("No ride requests available");
                }
            }
        }
    }
}