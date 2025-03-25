import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws InterruptedException{
        try (Socket socket = new Socket(Config.IP_ADDRESS, Config.PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                String serverMsg = reader.readLine();
                if (serverMsg == null) break;
                System.out.println(serverMsg);

                if (serverMsg.contains("Do you want to 'register' or 'login'?")) {
                    String choice = scanner.nextLine();
                    writer.println(choice);

                    if (choice.equalsIgnoreCase("register") || choice.equalsIgnoreCase("login")) {
                        continue;
                    }
                    else{
                        System.out.println("Invalid option. Please type 'register' or 'login'");
                    }
                } 
                else if(serverMsg.contains("Enter your username:")||serverMsg.contains("Enter your password:")||serverMsg.contains("Enter your type [Customer/Driver]:")) {

                    String userInput = scanner.nextLine();
                    writer.println(userInput);
                }
                else if (serverMsg.contains("Registered successfully!")||serverMsg.contains("You're logged in!")){
                    break;
                }
            }

            Thread t=new Thread(() -> {
                try{
                    String serverMsg;
                    while(!socket.isClosed()&&(serverMsg = reader.readLine()) != null){
                        System.out.println(serverMsg);
                        if (serverMsg.contains("Do you accept? (yes/no)")) {
                            String response = scanner.nextLine();
                            writer.println(response);
                        }
                    }
                }
                catch (IOException e) {
                    if(!socket.isClosed()){
                        e.printStackTrace();
                    }
                }
            });
            t.start();

            while (true) {
                String clientMessage = scanner.nextLine();
                writer.println(clientMessage);
                if (clientMessage.equalsIgnoreCase("disconnect")){
                    socket.close();                    
                    break;
                }
            }
            t.join();
        }
        catch(IOException|InterruptedException e){
            e.printStackTrace();
        }
    }
}
