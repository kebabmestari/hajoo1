package numservice;

/**
 * Maps control messages to corresponding values
 * Created by samlinz on 21.11.2016.
 */
public enum ControlMessage {
    // time out when waiting the worker count
    CLIENT_TIMEOUT(-1),
    // query total sum of all sent numbers
    QUERY_SUM_COMPLETE(1),
    // query the max sum of numbers for a specific worker
    QUERY_MAX_SUM_WORKER(2),
    // query number of total sent numbers
    QUERY_NUMBER_COUNT(3),
    // client wants to close connection
    CLOSE_CONNECTION(0),
    // indicates the end of number strean from client
    TERMINATE_STREAM(0),
    // return if client sent invalid query id
    INVALID_QUERY(-1);

    // the corresponding integer sent over socket
    private final int value;

    // constructor
    ControlMessage(int value) {
        this.value = value;
    }

    /**
     * @return The integer value of the message
     */
    public int getValue() {
        return value;
    }
}
