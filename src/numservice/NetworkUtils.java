package numservice;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Networking related utilities for the other classes
 *
 * @author Samuel Lindqvist
 */
public class NetworkUtils {

    /**
     * Find an open port from the given range and open a new serversocket
     * @param minPort min port
     * @param maxPort max port
     * @return ServerSocket object
     */
    public static ServerSocket createServerSocket(int minPort, int maxPort) throws Exception {
        for (int i = minPort; i < maxPort; i++) {
            try {
                return new ServerSocket(i);
            } catch (IOException e) {
                continue;
            }
        }
        throw new Exception("No free port found to be bound");
    }

}
