import org.mariuszgromada.math.mxparser.Expression;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// Resources used as reference:
// https://www.tutorialspoint.com/javaexamples/net_multisoc.htm

public class Server implements Runnable {
    // The server's port
    // The client should connect to this port when initiating a connection
    static final int PORT = 1234;

    // Main function: The main entry point.
    // Spawns a new thread for each client that initiates a TCP connection.
    public static void main(String[] args) throws Exception {
        // The server socket is the socket that is always on and accepts client TCP initialization requests
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server socket opened.");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client arrived.");

            new Thread(new Server(clientSocket)).start();
        }
    }


    // Instance fields and constructors

    // The client socket associated with a thread
    Socket clientSocket;
    Server(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    // run function: call for each thread
    public void run(){
        try {
            System.out.println("Started thread to communicate with client.");
            System.out.println("Client ip:" + clientSocket.getInetAddress().toString());
            // sets up streams for communicating with the client
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(this.clientSocket.getOutputStream()), true);

            while (true) {
                String messageFromClient = in.readLine();
                if (messageFromClient.equals("END"))
                    break;
                Expression expression = new Expression(messageFromClient);

                System.out.println("Recieved message from client: " + messageFromClient);
                double answer = expression.calculate();
                System.out.println("Answer: " + answer);

                out.println(messageFromClient + " = " + answer);
            }


            System.out.println("Closing connection with client with ip " + clientSocket.getInetAddress().toString());
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
