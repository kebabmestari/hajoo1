package numservice;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds worker's sum and count
 * Created by samlinz on 23.11.2016.
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
