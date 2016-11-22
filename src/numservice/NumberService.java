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

    // timeout to end the server-client connection if no queries are made
    public static final int QUERY_TIMEOUT = 60000;
    // worker constraints
    public static final int MAX_WORKERS = 10;
    public static final int MIN_WORKERS = 2;
    // server UDP connection port
    public static final int UDP_CLIENT_PORT = 3126;

    // service for the client server communication
    private NetworkCommunicationService netService;


    /**
     * Application entry point
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new NumberService().init(args[0]);
    }

    /**
     * Constructor
     */
    public NumberService() {
        LOG.info("Initializing new service server object");
    }

    /**
     * Initialize and start the service
     */
    public void init(String client) {
        netService = new NetworkCommunicationService(client, UDP_CLIENT_PORT);
        try {
            netService.initServiceConnection();
        } catch (Exception e) {
            e.printStackTrace();
            exit();
        }
    }

    /**
     * Receive to initial message from client
     * Which encloses the worker count
     */
    public int getWorkerCount() {
        // listen to the initial message from client
        // which equals the number of worker threads needed
        int numWorkers = 0;
        try {
            numWorkers = netService.listenToTCPMessage(); // BLOCKS
        } catch (SocketTimeoutException e) {
            LOG.warning("Failed to receive the initial message from client, exiting");
            exit();
        }

        // check validness
        if (!(numWorkers >= MIN_WORKERS) && (numWorkers <= MAX_WORKERS)) {
            LOG.severe("Invalid number of worker threads received from client" +
                    "\nreceived: " + numWorkers + " legal: " + MIN_WORKERS + "-" + MAX_WORKERS
            );
        }

        return numWorkers;
    }

    /**
     * Close connection
     */
    public void closeConnection() {
        if(netService != null) netService.closeConnection();
    }

    /**
     * Close connections and exit system
     */
    public void exit() {
        closeConnection();
        System.err.println("Exiting..");
        System.exit(0);
    }

    /**
     * @return NetworkCommunicationService object
     */
    public NetworkCommunicationService getNetworkService() {
        return netService;
    }

    // logger
    private Logger LOG = Logger.getLogger(NumberService.class.getName());

}
