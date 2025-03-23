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
                        Server.addDriver(driver);
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

    private void handleCustomer(Customer customer) throws IOException {
        writer.println("Welcome, " + customer.getUsername() + "! You can request a ride now! Enter 'disconnect' or 'request a ride'");
        while (true) {
            String message = reader.readLine();
            if (message == null || message.equalsIgnoreCase("disconnect")) {
                Server.removeClient(customer);
                break;
            }
            if (message.equalsIgnoreCase("request a ride")) {
                writer.println("Waiting for a driver...");
                /////////////
            }
        }
    }

    private void handleDriver(Driver driver) throws IOException {
        writer.println("Welcome, " + driver.getUsername() + "! You can offer rides! Enter 'disconnect' or 'offer a ride'");
        while (true) {
            String message = reader.readLine();
            if (message == null || message.equalsIgnoreCase("disconnect")) {
                Server.removeClient(driver);
                break;
            }
            if (message.equalsIgnoreCase("offer ride")) {
                writer.println("Waiting for customers to request...");
                /////////////
            }
        }
    }
}
