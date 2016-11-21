package numservice;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Main class
 *
 * Provides number summing services for a remote server
 *
 * @author Samuel Lindqvist
 * Created by samlinz on 21.11.2016.
 */
public class NumberService {

    // udp connection timeout in the beginning
    public static final int UDP_CONNECT_RETRIES = 5;
    public static final int UDP_CONNECT_TIMEOUT = 5000;
    // timeout to end the server-client connection if no queries are made
    public static final int QUERY_TIMEOUT = 60000;
    // worker constraints
    public static final int MAX_WORKERS = 10;
    public static final int MIN_WORKERS = 2;
    // server UDP connection port
    public static final int UDP_CLIENT_PORT = 3126;
    // port range
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    // server client socket
    private ServerSocket serverSocket;
    // socket to client
    private Socket clientSocket;

    // TCP input stream
    private ObjectInputStream oIs;
    // TCP output stream
    private ObjectOutputStream oOs;

    // client host name or address
    private InetAddress clientHost;

    /**
     * Application netry point
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new NumberService().init(args[1]);
    }

    /**
     * Constructor
     */
    public NumberService() {
        serverSocket = null;
        clientSocket = null;
        LOG.info("Initializing new service server object");
    }

    /**
     * Initialize and start the service
     */
    public void init(String client) {

        // resolve client address
        try {
            clientHost = InetAddress.getByName(client);
        } catch (UnknownHostException e) {
            LOG.warning("Can't resolve host, exiting");
            exit();
        }

        // connect with client
        establishConnection();

        // check if connection is established
        if(clientSocket == null) {
            LOG.warning("Client did not connect in time, exiting");
            exit();
        }


        // get streams
        try {
            InputStream iS = clientSocket.getInputStream();
            OutputStream oS = clientSocket.getOutputStream();
            oS.flush();
            oOs = new ObjectOutputStream(oS);
            oIs = new ObjectInputStream(iS);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.warning("Failed to get streams, exiting");
            exit();
        }


    }

    /**
     * Close sockets and workers
     * gracefully
     */
    public void closeConnection() {
        LOG.info("Closing sockets");
        if(serverSocket != null) serverSocket.close();
        if(clientSocket != null) clientSocket.close();
        LOG.info("Closing workers");
        // TODO: 22.11.2016 CLOSING WORKERS
    }

    /**
     * Close connections and exit system
     */
    public void exit() {
        closeConnection();
        System.err.println("Closing..");
        System.exit(0);
    }

    /**
     * Listen to client connecting
     */
    public void establishConnection() {
        // create TCP socket
        try {
            serverSocket = NetworkUtils.createServerSocket(MIN_PORT, MAX_PORT);
            // client connection timeout
            serverSocket.setSoTimeout(UDP_CONNECT_TIMEOUT);
        } catch (Exception e) {
            LOG.warning("Could not bind a port, exiting");
            System.exit(0);
        }

        int tries = 0;
        while (tries < UDP_CONNECT_RETRIES) {
            try {
                // send the client an udp packet containing the tcp port to connect
                sendPort();
                // listen to connection to tcp port
                clientSocket = serverSocket.accept();
                LOG.info("Client " + clientSocket.getInetAddress().toString() +
                        " connected to port " + serverSocket.getLocalPort());
            } catch (SocketTimeoutException e) {
                // send udp packet again
                tries++;
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Send the TCP port to the client
     * using UDP datagram packets
     */
    public void sendPort() throws Exception {
        // message, tcp port
        byte[] udpData = Integer.toString(serverSocket.getLocalPort()).getBytes();
        DatagramSocket udpSocket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(udpData,
                udpData.length,
                clientHost,
                UDP_CLIENT_PORT);
        udpSocket.send(packet);
    }

    // logger
    private Logger LOG = Logger.getLogger(NumberService.class.getName());

}
