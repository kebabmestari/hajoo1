package numservice;

import java.net.ServerSocket;
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

    // worker id
    private int id;

    public NumberWorker(int id, AtomicInteger target) {
        this.target = target;
        this.id = workerId++;
        this.count = 0;

        // TODO: 21.11.2016 SERVER SOCKET IMPLEMENTATION 

        LOG.info("Worker " + this.id + " instatiated");
    }

    @Override
    public void run() {
        // TODO: 21.11.2016 IMPLEMENT LISTENING
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

    }

    /**
     * @return count of added values
     */
    public int getCount() {

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
