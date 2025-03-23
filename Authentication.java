import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Authentication{

    public static void register(BufferedReader reader, PrintWriter writer) throws IOException {
        while (true) {
            writer.println("Enter your username:");
            String username = reader.readLine();
            if (username == null || username.trim().isEmpty()) {
                writer.println("Invalid username, please try again");
                continue;
            }

            writer.println("Enter your password:");
            String password = reader.readLine();
            if (password == null || password.trim().isEmpty()) {
                writer.println("Invalid password, please try again");
                continue;
            }

            writer.println("Enter your type [Customer/Driver]:");
            String type = reader.readLine();
            if (!(type.trim().equalsIgnoreCase("Customer") || type.trim().equalsIgnoreCase("Driver"))) {
                writer.println("Invalid type. Please enter 'Customer' or 'Driver'");
                continue;
            }

            boolean isSuccesseeded = Server.register(username, password, type);
            if (isSuccesseeded) {
                writer.println("Registered successfully! You can login now");
                break;
            }
            else{
                writer.println("Username already exists! please try again");
            }
        }
    }

    public static User login(BufferedReader reader, PrintWriter writer) throws IOException {
        while (true) {
            writer.println("Enter your username:");
            String username = reader.readLine();
            writer.println("Enter your password:");
            String password = reader.readLine();

            User user = Server.login(username, password);
            
            return user;
        }
    }
}