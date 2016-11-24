package numservice;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static numservice.ControlMessage.TERMINATE_STREAM;

/**
 * Created by samlinz on 21.11.2016.
 */
public class NumberWorker implements Runnable {

    // target accumulated integer
    private WorkerStatus target;

    // server socket for this worker
    private ServerSocket socket;
    private Socket clientSocket;

    private NetworkCommunicationService netService;

    // binded port, main thread polls
    private AtomicInteger port;

    // worker id
    private int id;

    public NumberWorker(int id, WorkerStatus target) {
        this.target = target;
        this.port = new AtomicInteger(0);
        this.id = workerId++;

        LOG.info("Worker " + id + " created, not yet connected");
    }

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

            if(!netService.isConnected()) {
                LOG.severe("Worker " + id + "was not connected, closing");
                return;
            }

            while (true) {
                // listen to messages
                int msg = netService.listenToTCPMessage();
                if(handleMessage(msg)) break;

                Thread.sleep(5);
            }
        } catch (SocketTimeoutException e) {
            LOG.warning("Worker " + id + " timeout, closing");
        } catch (InterruptedException e) {
            LOG.info("Worker " + id + " thread " + Thread.currentThread().getName() + " interrupted");
        } finally {
            closeWorker();
        }
        LOG.info("Worker " + id + " exiting");
    }

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

        LOG.info("Worker " + id + " received " + msg);

        return false;
    }

    /**
     * @return binded port, 0 if not set
     */
    public int getPort() {
        return port.get();
    }

    /**
     * Close the connection
     */
    public void closeWorker() {
        netService.closeConnection();
    }

    // logger
    private Logger LOG = Logger.getLogger(NumberWorker.class.getName());
    private static int workerId = 0;
}
