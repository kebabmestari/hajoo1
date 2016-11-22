package numservice;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test service
 * Created by samlinz on 22.11.2016.
 */
public class NetworkCommunicationServiceTest {

    private NetworkCommunicationService service = null;

    @Before
    public void setUp() throws Exception {
        service = new NetworkCommunicationService("localhost", NumberService.UDP_CLIENT_PORT);
    }

    @After
    public void tearDown() throws Exception {
        if(service != null)
            service.closeConnection();
    }

    // test establishing connection with the server
    // the server must be created separately
    @Test
    public void initConnection() throws Exception {
        final int port = NumberService.UDP_CLIENT_PORT;
        System.out.println("Connecting to test server at LOCALHOST:" + port);
        service.initConnection();
        Assert.assertTrue(service.isConnected());
    }



}