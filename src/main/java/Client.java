import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Connecting to socket...");
            // Use IP address supplied by command line argument if possible,
            // else default to localhost
            InetAddress serverInetAddress;
            if (args.length == 1) {
                serverInetAddress = InetAddress.getByName(args[0]);
            } else {
                serverInetAddress = InetAddress.getLocalHost();
            }
            Socket socket = new Socket(serverInetAddress.getHostName(), Server.PORT);
            System.out.println("Connected to socket.");
            // sets up streams for communicating with the server
            BufferedReader streamFromServer = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter streamToServer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            // object for reading from/writing to console
            Scanner console = new Scanner(System.in);

            // Prompt the user's name and send it over the network
            // Prompt again if the server did not acccept the name
            do {
                System.out.println("Enter your name: ");
                String name = console.nextLine();
                streamToServer.println(MessageType.SET_NAME + name);
            } while (streamFromServer.readLine().charAt(0) != MessageType.NAME_ACK);

            String userInput;
            while (true) {
                System.out.println("Enter a calculation, or enter a blank line to quit. ");
                userInput = console.nextLine();
                if (userInput.equals(""))
                    break;
                streamToServer.println(MessageType.CALCULATION_REQUEST + userInput);
                String serverResponse = streamFromServer.readLine();
                if (serverResponse.charAt(0) == MessageType.CALCULATION_RESULT) {
                    // substring(1) to remove the message header
                    System.out.println("Result from server: " + serverResponse.substring(1));
                } else {
                    System.out.println("Error: unexpected response from server: " + serverResponse);
                    break;
                }
            }
            System.out.println("Closing connection...");
            // Send close message
            streamToServer.println(MessageType.END);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
    }
}
