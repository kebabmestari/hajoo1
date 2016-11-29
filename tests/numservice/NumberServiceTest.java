package numservice;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Test service
 */
public class NumberServiceTest {

    private static NumberService service = null;

    @BeforeClass
    public static void setUp() throws Exception {
        service = new NumberService();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(service != null)
            service.closeConnection();
    }

    // test establishing connection with the server
    // the server must be created separately
    @Test
    public void initService() throws Exception {
        final int port = NumberService.UDP_CLIENT_PORT;
        System.out.println("Connecting to test server at LOCALHOST:" + port);
        Assert.assertNotNull(service);
        service.init("localhost");
        Assert.assertTrue(service.getNetworkService().isConnected());
    }

    @Test
    public void getWorkerCount() throws Exception {
        Assert.assertNotNull(service);
        Assert.assertTrue(service.getWorkerCount() > 0);
    }
}