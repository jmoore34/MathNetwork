import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Connecting to socket...");
            InetAddress host = InetAddress.getLocalHost();
            Socket socket = new Socket(host.getHostName(), Server.PORT);
            System.out.println("Connected to socket.");
            // sets up streams for communicating with the server
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()));

            // object for reading from/writing to console
            Scanner console = new Scanner(System.in);

            String msg;
            out.println("jojo is best anime");
            do {
                msg = console.nextLine();
                out.println(msg);
                String serverResponse = in.readLine();
                if (serverResponse == null) {
                    System.out.println("null server response");
                    break;
                }
                System.out.println("from server:" + serverResponse);
            } while (!msg.equals("END"));

            socket.close();
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
    }
}
