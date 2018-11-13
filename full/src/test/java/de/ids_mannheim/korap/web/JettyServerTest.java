package de.ids_mannheim.korap.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.junit.BeforeClass;
import org.junit.Test;

public class JettyServerTest {

    @BeforeClass
    public static void testServerStarts () throws Exception {
        Server server = new Server(8000);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(
                new Handler[] { new ShutdownHandler("secret", true, true) });
        server.setHandler(handlers);
        server.start();
    }
    
    @Test
    public void testShutdown () throws IOException {
        URL url = new URL(
                "http://localhost:8000/shutdown?token=secret");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        assertEquals(200, connection.getResponseCode());
    }
}
