import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket(Config.IP_ADDRESS, Config.PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                String serverMsg = reader.readLine();
                if (serverMsg == null) break;
                System.out.println(serverMsg);

                if (serverMsg.contains("Do you want to 'register' or 'login'?")) {
                    String choice = readInput(scanner);
                    writer.println(choice);
                    writer.flush();

                    if (!choice.equalsIgnoreCase("register") && !choice.equalsIgnoreCase("login")) {
                        System.out.println("Invalid option. Please type 'register' or 'login'");
                    }
                }
                else if (serverMsg.contains("Do you accept? (yes/no)")) {
                    String response = scanner.nextLine();
                    writer.println(response);
                    writer.flush();

                }
                else if (serverMsg.contains("Enter your username:") ||
                         serverMsg.contains("Enter your password:") ||
                         serverMsg.contains("Enter your type [Customer/Driver]:")) {

                    String userInput = readInput(scanner);
                    writer.println(userInput);
                    writer.flush();

                } 
                else if (serverMsg.contains("Registered successfully!") || serverMsg.contains("You're logged in!")) {
                    break;
                }
            }

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while (!socket.isClosed() && (serverMsg = reader.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("Connection lost: " + e.getMessage());
                    }
                }
            });
            
            listenerThread.start();
            
            while (true) {
                String clientMessage = readInput(scanner);
                writer.println(clientMessage);
                writer.flush();
            
                if (clientMessage.equalsIgnoreCase("disconnect")) {
                    // Wait for the server's response
                    String response = reader.readLine();
            
                    // Ensure valid server response
                    if (response == null) {
                        System.out.println("Server connection closed unexpectedly.");
                        break; // Exit if the server terminates
                    }
            
                    System.out.println(response);
            
                    // Handle disconnection confirmation properly
                    if (response.equalsIgnoreCase("Disconnected successfully.")) {
                        try {
                            socket.close(); // Close the socket
                        } catch (IOException e) {
                            System.err.println("Error while closing socket: " + e.getMessage());
                        }
                        break; // Exit the loop
                    }
                }
            
                if (socket.isClosed()) break; // Exit if socket is closed
            }
            
            // Ensure listenerThread exits cleanly
            try {
                listenerThread.join(); // Wait for the listener thread to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.err.println("Thread interrupted: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }            
    

    private static synchronized String readInput(Scanner scanner) {
        try {
            return scanner.nextLine();
        }
        catch (IllegalStateException e) {
            System.err.println("Input error: " + e.getMessage());
            return "";
        }
    }
}
