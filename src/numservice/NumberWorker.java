package numservice;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Created by samlinz on 21.11.2016.
 */
public class NumberWorker implements Runnable {

    // target accumulated integer
    private AtomicInteger target;
    // number count
    private AtomicInteger count;

    // server socket for this worker
    private ServerSocket socket;
    private Socket clientSocket;

    private NetworkCommunicationService netService;

    // worker id
    private int id;

    public NumberWorker(int id, AtomicInteger target) {
        this.target = target;
        this.id = workerId++;
        this.count.set(0);

        LOG.info("Worker " + id + " created, not yet connected");
    }

    @Override
    public void run() {
        // listen to client connection
        netService = new NetworkCommunicationService();
        try {
            netService.initWorkerConnection();
            LOG.info("Worker " + this.id + " instantiated and connected");
        } catch (Exception e) {
            LOG.warning("Worker " + this.id + " could not create establish connection");
            closeWorker();
        }

        // listen to messages
        try {
            while(true) {
                int msg = netService.listenToTCPMessage();
                Thread.sleep(5);
            }
        } catch (SocketTimeoutException e) {
            LOG.warning("Worker " + id + " timeout, closing");
            closeWorker();
        } catch (InterruptedException e) {
            LOG.info("Worker " + id + " thread " + Thread.currentThread().getName() + " interrupted");
        }
    }

    /**
     * @return current sum as integer
     */
    public int getSum() {
        return target.get();
    }

    /**
     * @return socket port
     */
    public int getPort() {

        return 0;
    }

    /**
     * @return count of added values
     */
    public int getCount() {

        return 0;
    }

    /**
     * Close the connection
     */
    public void closeWorker() {

    }

    /**
     * Add to the sum and increment the counter
     * @param number addition to the sum
     */
    private void add(int number) {

        // add to the sum
        target.addAndGet(number);
        // increment the counter
        count.incrementAndGet();

        LOG.info("Worker " + id + " received increment of " + number);
    }

    // logger
    private Logger LOG = Logger.getLogger(NumberWorker.class.getName());
    private static int workerId = 0;
}
