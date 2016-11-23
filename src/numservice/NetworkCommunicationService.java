package numservice;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;

/**
 * Encloses details about one connection and the
 * methods for creating, handling the connection and
 * sending and receiving messages
 * UDP for sending TCP port
 * TCP for general communication
 *
 * @author Samuel Lindqvist
 *         Created by samlinz on 22.11.2016.
 */
public class NetworkCommunicationService {

    // port range
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    // udp connection timeout in the beginning
    public static final int UDP_CONNECT_RETRIES = 5;
    public static final int UDP_CONNECT_TIMEOUT = 5000;

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

    // client host and port
    private String clientHostString;
    private int clientUDPPort;

    /**
     * Constructor for the service
     */
    public NetworkCommunicationService(String client, int udpPort) {
        // init insatnce variables
        clientHostString = client;
        clientUDPPort = udpPort;
        serverSocket = null;
        clientSocket = null;
        LOG.info("Creating new Communications service object");
    }

    /**
     * Constructor for a worker connection
     * Does not connect anywhere by default, listens to client connection
     */
    public NetworkCommunicationService() {
        LOG.info("Creating new worker Communications service object");
    }

    /**
     * Initiate the connection for the main service
     *
     * @throws Exception if something went wrong and connection could not be established
     */
    public void initServiceConnection() throws Exception {

        LOG.info("Initializing connection to " + clientHostString + ":" + clientUDPPort);

        if (serverSocket != null) {
            LOG.warning("Socket is already open");
            return;
        }

        // resolve client address
        try {
            resolveHost();
        } catch (Exception e) {
            throw new Exception("Could not resolve host");
        }

        LOG.info("Host resolved");

        try {
            establishConnection();
        } catch (Exception e) {
            throw new Exception("Could not establish connection");
        }

        try {
            getStreams();
        } catch (Exception e) {
            throw new Exception("Could not get streams");
        }

        // check if connection is established
        if (clientSocket == null) {
            throw new Exception("Client did not connect in time");
        }

        LOG.info("Connection established");
    }

    /**
     * Initialize the worker connection
     *
     * @throws Exception
     */
    public ServerSocket initWorkerConnection(int workerId) throws Exception {

        LOG.info("Initializing socket for worker " + workerId);

        if (serverSocket != null) {
            LOG.warning("Socket is already open");
            return serverSocket;
        }
        // create TCP socket
        try {
            serverSocket = NetworkUtils.createServerSocket(MIN_PORT, MAX_PORT);
            // client connection timeout
            serverSocket.setSoTimeout(NumberService.QUERY_TIMEOUT);
        } catch (Exception e) {
            throw new Exception("Could not bind a worker to a port");
        }

        return serverSocket;

    }

    /**
     * Create a server socket and listen to a connection
     * Block until the client connects to the worker and then return
     */
    public void establishWorkerConnection() throws Exception {
        LOG.info("Establishing worker connection");

        try {
            clientSocket = serverSocket.accept();
        } catch (SocketTimeoutException e) {
            throw new Exception("Client did not connect to worker in time");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            establishWorkerConnection();
        } catch (Exception e) {
            throw new Exception("Could not establish connection");
        }

        try {
            getStreams();
        } catch (Exception e) {
            throw new Exception("Could not get streams");
        }

        // check if connection is established
        if (clientSocket == null) {
            throw new Exception("Client did not connect in time");
        }

        LOG.info("Worker connection established");
    }

    /**
     * @return port which the server is listening to
     */
    private int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Resolve client address and create INetAddress object
     *
     * @throws Exception if host could not be resolved
     */
    private void resolveHost() throws Exception {
        // resolve client address
        try {
            clientHost = InetAddress.getByName(clientHostString);
        } catch (UnknownHostException e) {
            throw new Exception("Can't resolve host");
        }
    }

    /**
     * Get input and output streams from the connection
     */
    private void getStreams() throws Exception {
        // get streams
        try {
            InputStream iS = clientSocket.getInputStream();
            OutputStream oS = clientSocket.getOutputStream();
            oS.flush();
            oOs = new ObjectOutputStream(oS);
            oIs = new ObjectInputStream(iS);
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("Can't create streams");
        }
    }

    /**
     * @param ms new timeout for the connection
     */
    public void setTimeout(int ms) {
        try {
            serverSocket.setSoTimeout(ms);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen to client connecting
     */
    private void establishConnection() {

        LOG.info("Establishing connection");

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
            LOG.info("Sending UDP, waiting for TCP.. " + (tries + 1) + "/" + UDP_CONNECT_RETRIES);
            try {
                // send the client an udp packet containing the tcp port to connect
                sendPort();
                // listen to connection to tcp port
                clientSocket = serverSocket.accept();
                LOG.info("Client " + clientSocket.getInetAddress().toString() +
                        " connected to port " + serverSocket.getLocalPort());
                break;
            } catch (SocketTimeoutException e) {
                // send udp packet again
                tries++;
                LOG.info("Timeout...");
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Listen to TCP messages from client
     *
     * @return integer value that is the received message
     * @throws SocketTimeoutException if connection timeouts
     */
    public int listenToTCPMessage() throws SocketTimeoutException {
        try {
            int msg = oIs.readInt();
            System.out.println("Received message: " + msg);
            return msg;
        } catch (IOException e) {
            LOG.warning("Error receiving TCP message: " + e.getMessage());
        }
        // close connection
        return 0;
    }

    /**
     * Send and integer value over TCP socket
     *
     * @param value the integer value to be sent
     */
    public void sendTCPMessage(int value) {
        try {
            System.out.println("Sending message: " + value);
            oOs.writeInt(value);
            oOs.flush();
            System.out.println("Message sent");
        } catch (IOException e) {
            LOG.warning("Error sending TCP message: " + e.getMessage());
        }
    }

    /**
     * Send the TCP port to the client
     * using UDP datagram packets
     */
    private void sendPort() throws Exception {
        // message, tcp port
        byte[] udpData = Integer.toString(serverSocket.getLocalPort()).getBytes();
        DatagramSocket udpSocket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(udpData,
                udpData.length,
                clientHost,
                clientUDPPort);
        udpSocket.send(packet);
        LOG.info("Sent UDP datagram");
    }

    /**
     * Close sockets and workers
     * gracefully
     */
    public void closeConnection() {
        LOG.info("Closing sockets");
        try {
            if (serverSocket != null) serverSocket.close();
            if (clientSocket != null) clientSocket.close();
            if (oIs != null) oIs.close();
            if (oOs != null) oOs.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOG.warning("Failed closing the sockets");
        }
    }

    /**
     * @return true if the TCP socket is connected to client
     */
    public boolean isConnected() {
        return clientSocket.isConnected();
    }

    // logger
    private Logger LOG = Logger.getLogger(NetworkCommunicationService.class.getName());
}
