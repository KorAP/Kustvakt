package de.ids_mannheim.korap.web.service;

import org.junit.Before;

import de.ids_mannheim.korap.config.ContextHolder;

public abstract class FastJerseyLightTest extends FastJerseyBaseTest {

    private static String[] classPackages =
            new String[] { "de.ids_mannheim.korap.web.service.light" };


    public static void startServer () {
        try {
            if (testContainer != null) {
                testContainer.start();
            }
        }
        catch (Exception e) {
            initServer(PORT + PORT_IT++, classPackages);
            startServer();
        }
    }

    @Before
    public static void startServerBeforeFirstTestRun () {
        if (testContainer == null) {
            initServer(PORT, classPackages);
            startServer();
        }
    }
    
    @Override
    protected ContextHolder getContext () {
        return new ContextHolder(this.context) {};
    }
}
