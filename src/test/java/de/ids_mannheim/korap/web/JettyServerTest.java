package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author margaretha
 */
public class JettyServerTest {

    static int selectedPort = 0;

    @BeforeAll
    static void testServerStarts () throws Exception {

        for (int port = 1000; port <= 2000; port++) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                selectedPort = port;
                break;
            }
            catch (IOException ignored) {
                // Port is already in use, try the next one
            }
        }

        Server server = new Server(selectedPort);
        ShutdownHandler shutdownHandler = new ShutdownHandler("secret");
        server.setHandler(shutdownHandler);
        server.start();
    }

    @Test
    public void testShutdown () throws IOException {
        URL url = new URL(
                "http://localhost:" + selectedPort + "/shutdown?token=secret");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        assertEquals(200, connection.getResponseCode());
    }
}
