package numservice;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds a single worker's sum and count
 *
 * @author Samuel Lindqvist
 */
public class WorkerStatus {

    // sum and count, thread safe
    private AtomicInteger sum = new AtomicInteger(0);
    private AtomicInteger count = new AtomicInteger(0);

    public int getSum() {
        return sum.get();
    }

    public void addSum(int count) {
        sum.addAndGet(count);
    }

    public int getCount() {
        return count.get();
    }

    public void incrementCount() {
        count.addAndGet(1);
    }
}
