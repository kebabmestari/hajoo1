package numservice;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static numservice.ControlMessage.TERMINATE_STREAM;

/**
 * A worker that runs as a separate thread, listening to it's own socket
 * and sum incoming integer values to a location delivered by the creating
 * class
 *
 * @author Samuel Lindqvist
 */
public class NumberWorker implements Runnable {

    // target accumulated integer
    private WorkerStatus target;

    // server socket for this worker
    private ServerSocket socket;
    private Socket clientSocket;

    // network service object which encloses all low level communication details
    private NetworkCommunicationService netService;

    // binded port, main thread polls
    private AtomicInteger port;

    // worker id
    private int id;

    // running flag
    private AtomicBoolean running;

    /**
     * Constructor
     *
     * @param target target WorkerStatus object which will be used to store the count and sum
     */
    public NumberWorker(WorkerStatus target) {
        this.target = target;
        this.port = new AtomicInteger(0);
        this.id = workerId++;
        this.running = new AtomicBoolean(true);

        LOG.info("Worker " + id + " created, not yet connected");
    }

    /**
     * Thread body
     * Create the network service object, give out the port and create connection
     * After this listen, decrypt and handle incoming messages until the client
     * or X terminates the worker
     */
    @Override
    public void run() {
        try {
            // listen to client connection
            netService = new NetworkCommunicationService();
            try {
                // open socket and get the port
                port.set(netService.initWorkerConnection(id).getLocalPort());
                // listen for the connection
                netService.establishWorkerConnection();
                LOG.info("Worker " + this.id + " instantiated and connected");
            } catch (Exception e) {
                LOG.warning("Worker " + this.id + " could not create establish connection");
                closeWorker();
            }

            if (!netService.isConnected()) {
                LOG.severe("Worker " + id + "was not connected, closing");
                return;
            }

            // loop
            while (this.running.get() == true) {
                // listen to messages
                int msg = netService.listenToTCPMessage(id);
                if (handleMessage(msg)) break;
            }
        } catch (SocketTimeoutException e) {
            LOG.warning("Worker " + id + " timeout, closing");
//        } catch (InterruptedException e) {
//            LOG.info("Worker " + id + " thread " + Thread.currentThread().getName() + " interrupted");
        } finally {
            // close tcp socket and related objects
            netService.closeConnection();
        }
        LOG.info("Worker " + id + " exiting");
    }

    /**
     * Handle an incoming message,
     * edit the state accordingly
     *
     * @param msg decrypted integer message
     * @return true if a end of communication message was received
     */
    private boolean handleMessage(int msg) {
        // when the client wishes to terminate the number stream
        if (msg == TERMINATE_STREAM.getValue()) {
            LOG.info("Worker " + id + " received END OF STREAM");
            closeWorker();
            return true;
        }

        // otherwise
        // add to sum
        target.addSum(msg);
        // increment number count
        target.incrementCount();

        LOG.info("Worker " + id + " received " + msg + " Sum now: " + target.getSum() +
                " Count " + target.getCount());

        return false;
    }

    /**
     * @return binded port, 0 if not set
     */
    public int getPort() {
        return port.get();
    }

    /**
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * @return worker sum
     */
    public int getSum() {
        return target.getSum();
    }

    /**
     * Close the connection
     */
    public void closeWorker() {
        running.set(false);
    }

    // logger
    private Logger LOG = Logger.getLogger(NumberWorker.class.getName());
    // logger ids
    private static int workerId = 1;
}
