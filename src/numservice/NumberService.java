package numservice;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Main class
 *
 * Provides number summing services for a remote server
 *
 * @author Samuel Lindqvist
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

    // worker targets
    private Map<NumberWorker, WorkerStatus> workerStatuses;
    private List<Thread> threadList;


    /**
     * Application entry point
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {

        // remove default handler
        Logger globalLogger = Logger.getLogger("");
        Handler[] handlers = globalLogger.getHandlers();
        for(Handler handler : handlers) {
            globalLogger.removeHandler(handler);
        }

        // add custom handler
        ConsoleHandler customHandler = new ConsoleHandler();
        customHandler.setFormatter(new CustomLogFormatter());
        globalLogger.addHandler(customHandler);

        // if no argument is specified, use localhost as client
        String client = args.length > 0 ? args[0] : "localhost";
        new NumberService().init(client);
    }

    /**
     * Constructor
     */
    public NumberService() {
        // init empty map
        workerStatuses = new HashMap<>();
        threadList = new ArrayList<>();
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

        // create workers
        int[] wPorts = createWorkers(getWorkerCount());

        // send worker ports to client
        sendWorkerPorts(wPorts);

        // listen to queries from client while workers are running
        listenToQueries();
    }

    /**
     * Send client the ports of the created workers
     */
    private void sendWorkerPorts(int[] ports) {
        LOG.info("Sending worker ports to client");
        Arrays.stream(ports).forEach((p) -> {
            netService.sendTCPMessage(p);
        });
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
     * Create the NumberWorkers
     *
     * @param count count of workers
     * @return array of integers which are the ports the workers are listening to
     */
    private int[] createWorkers(int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {

            // new thread safe status object
            WorkerStatus status = new WorkerStatus();
            NumberWorker worker = new NumberWorker(status);
            workerStatuses.put(worker, status);

            // create a new thread object for worker and add to list
            Thread newThread = new Thread(worker);
            threadList.add(newThread);

            // start the worker
            newThread.start();
            LOG.info("Worker " + i + " created and started");

            // poll for the port
            while(true) {
                int port = worker.getPort();
                if(port != 0) {
                    result[i] = port;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Main thread listens to queries and responds accordingly
     * while the workers do their jobs
     */
    private void listenToQueries() {
        // set new timeout
        netService.setTimeout(QUERY_TIMEOUT);
        while (true) {
            try {
                int msg = netService.listenToTCPMessage();
                if (handleQuery(msg)) break;
            } catch (SocketTimeoutException e) {
                LOG.warning("Main connection timed out, closing");
            } catch (Exception e) {
                e.printStackTrace();
            }
//            try {
//                Thread.sleep(5);
//            } catch (InterruptedException e) {
//                LOG.warning("Main thread interrupted");
//                exit();
//            }
        }
        closeConnection();
    }

    /**
     * Handle queries and other control messages from client
     *
     * @param msg deserialized message integer value
     * @return true if connection closing message was received
     */
    private boolean handleQuery(int msg) {
        if (msg == ControlMessage.QUERY_MAX_SUM_WORKER.getValue()) {
            int answer = getLargestIndividualSumWorker();
            LOG.info("Received query MAX_SUM_WORKER, answering " + answer);
            netService.sendTCPMessage(answer);
        } else if (msg == ControlMessage.QUERY_SUM_COMPLETE.getValue()) {
            int answer = getSumOfAllWorkers();
            LOG.info("Received query SUM_COMPLETE, answering " + answer);
            netService.sendTCPMessage(answer);
        } else if (msg == ControlMessage.QUERY_NUMBER_COUNT.getValue()) {
            int answer = getReceivedValuesCount();
            LOG.info("Received query NUMBER_COUNT, answering " + answer);
            netService.sendTCPMessage(answer);
        } else if (msg == ControlMessage.CLOSE_CONNECTION.getValue()) {
            return true;
        } else {
            LOG.info("Received invalid query " + msg + " answering INVALID_QUERY");
            netService.sendTCPMessage(ControlMessage.INVALID_QUERY.getValue());
        }
        return false;
    }

    /**
     * @return largest of the individual worker's sums
     */
    private int getLargestIndividualSumWorker() {
        int largest = Integer.MIN_VALUE;
        NumberWorker largestWorker = null;
        for(NumberWorker w : workerStatuses.keySet()) {
            int tempSum = w.getSum();
            if(tempSum > largest) {
                largest = tempSum;
                largestWorker = w;
            }
        }
        return largestWorker.getId();
    }

    /**
     * @return sum of all of the worker's sums
     */
    private int getSumOfAllWorkers() {
        int sum = 0;
        for(WorkerStatus e : workerStatuses.values()) {
            sum += e.getSum();
        }
        return sum;
    }

    /**
     * @return total count of received values to workers
     */
    private int getReceivedValuesCount() {
        int count = 0;
        for(WorkerStatus e : workerStatuses.values()) {
            count += e.getCount();
        }
        return count;
    }

    /**
     * Close connection
     */
    private void closeConnection() {

        // close X connection to client
        if (netService != null) netService.closeConnection();

//        threadList.stream().forEach((t) -> t.interrupt());

        // tell each worker to stop and quit
        workerStatuses.keySet().forEach((w) -> {
            w.closeWorker();
        });

        LOG.info("Waiting for workers to close");

        // wait until the workers are all closed and then quit
        boolean areWorkersClosed;
        do {
            areWorkersClosed = true;
            for (Thread t : threadList) {
                if(t.isAlive()) areWorkersClosed = false;
            }
        } while(!areWorkersClosed);

        LOG.info("Exiting..");

        // exit cleanly
        System.exit(0);
    }

    /**
     * Close connections and exit system in case of error
     * Signal an error with status 1
     */
    public void exit() {
        closeConnection();
        System.err.println("Exiting..");
        System.exit(1);
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
