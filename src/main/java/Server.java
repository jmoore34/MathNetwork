import org.mariuszgromada.math.mxparser.Expression;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

// Resources used as reference:
// https://www.baeldung.com/java-append-to-file
// https://www.tutorialspoint.com/javaexamples/net_multisoc.htm
// https://howtodoinjava.com/java/date-time/format-localdatetime-to-string/
// https://www.javatpoint.com/java-get-current-date
// https://stackoverflow.com/questions/3471397/how-can-i-pretty-print-a-duration-in-java

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
    Server(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
    }

    // Create log .txt files and corresponding reentrantLock
    // joinExitLog.txt only logs client's connect and disconnect timestamps, IP address, and duration of client's visit
    // calculationRequestLog.txt logs each client's calculation request with timestamps
    // ReentrantLocks are used to prevent multiple threads(clients) to write on the same file at the same time.
    FileWriter joinExitLogWriter = new FileWriter("joinExitLog.txt", true);
    ReentrantLock joinExitLogMutex = new ReentrantLock();
    FileWriter calculationRequestLogWriter = new FileWriter("calculationRequestLog.txt", true);
    ReentrantLock calculationRequestMutex = new ReentrantLock();

    // Defines the standard for formatting times in log files
    static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    // run function: call for each thread
    public void run(){
        try {
            String clientIP = clientSocket.getInetAddress().toString();
            System.out.println("Started thread to communicate with client with ip " + clientIP);
            // sets up streams for communicating with the client
            BufferedReader streamFromClient = new BufferedReader(
                    new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter streamToClient = new PrintWriter(
                    new OutputStreamWriter(this.clientSocket.getOutputStream()), true);

            // saves connection start time
            LocalDateTime connectionStartTime = LocalDateTime.now();

            // Set client's name by reading their SET_NAME message
            // Send ERROR message type if SET_NAME message invalid
            String clientName = "";
            do {
                String setNameMessage = streamFromClient.readLine();
                // message length should be at least 2, i.e. SET_NAME char + 1 character name + newline
                if (setNameMessage.charAt(0) != MessageType.SET_NAME || setNameMessage.length() < 3) {
                    System.out.println("Received invalid SET_NAME request from client: " + setNameMessage);
                    streamToClient.println(MessageType.ERROR + "Expected valid SET_NAME request.");
                } else {
                    // message was valid
                    // so extract payload, i.e. taking out the MessageType char
                    clientName = setNameMessage.substring(1);
                    System.out.println("Name of client with IP " + clientIP + " set to " + clientName);
                    // then send name set acknowledgement message
                    streamToClient.println(MessageType.NAME_ACK);
                }
            } while (clientName.length() == 0);

            while (true) {
                String messageFromClient = streamFromClient.readLine();
                System.out.println("Received message from client: " + messageFromClient);

                switch (messageFromClient.charAt(0)) {
                    case MessageType.CALCULATION_REQUEST:
                        // substring(1) removes the header character
                        String calculationRequest = messageFromClient.substring(1);
                        LocalDateTime requestTime = LocalDateTime.now();
                        Expression expression = new Expression(calculationRequest);
                        double answer = expression.calculate();
                        // Send the result back to the client
                        System.out.println("From calculation request message "
                                + messageFromClient
                                + ", sending result to client: " + MessageType.CALCULATION_RESULT + "" + answer);
                        streamToClient.println(MessageType.CALCULATION_RESULT + "" + answer);
                        // Log the transaction in the calculationRequestLog file
                        // Requires obtaining mutex in order to be thread safe
                        // Format: TIMESTAMP,CLIENT NAME,IP,CALCULATION REQUEST,CALCULATION RESULT
                        calculationRequestMutex.lock();
                        calculationRequestLogWriter.write(
                                requestTime.format(dateTimeFormatter)
                                + ","
                                + clientName
                                + ","
                                + clientIP
                                + ","
                                + calculationRequest
                                + ","
                                + answer
                                + "\n"
                        );
                        calculationRequestLogWriter.flush();
                        calculationRequestMutex.unlock(); // Release lock to other threads
                        break;
                    case MessageType.END:
                        System.out.println("Closing connection with client with ip " + clientIP);
                        clientSocket.close();
                        LocalDateTime connectionEndTime = LocalDateTime.now();

                        // Duration format: DAYS:HOURS:MINUTES:SECONDS
                        Duration connectionDuration = Duration.between(connectionStartTime, connectionEndTime);
                        String durationString = connectionDuration.toDaysPart() + ":"
                                + connectionDuration.toHoursPart() + ":"
                                + connectionDuration.toMinutesPart() + ":"
                                + connectionDuration.toSecondsPart();
                        // Log user's session information (e.g. connect timestamp, disconnect timestamp, connection duration) to
                        // the joinExit log
                        // Requires getting lock so that different threads don't overwrite each other
                        // Format: CONNECT TIMESTAMP,DISCONNECT TIMESTAMP,CLIENT NAME,IP,DURATION
                        joinExitLogMutex.lock();
                        joinExitLogWriter.write(
                                connectionStartTime.format(dateTimeFormatter)
                                + ","
                                + connectionEndTime.format(dateTimeFormatter)
                                + ","
                                + clientName
                                + ","
                                + clientIP
                                + ","
                                + durationString
                                + "\n"
                        );
                        joinExitLogWriter.flush();
                        joinExitLogMutex.unlock(); // Release lock to other threads
                        break;
                    default:
                        System.out.println("ERROR: Invalid request from client with name '" + clientName + "' and ip " + clientIP);
                }
                if (messageFromClient.equals("END"))
                    break;
            }
        } catch (SocketException e) {
            System.out.println("Connection with client closed.");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
    }
}
