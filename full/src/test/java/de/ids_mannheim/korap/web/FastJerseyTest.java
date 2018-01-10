package de.ids_mannheim.korap.web;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;

import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.TestHelper;
import de.ids_mannheim.korap.web.service.FastJerseyBaseTest;

public abstract class FastJerseyTest extends FastJerseyBaseTest {

    private static String[] classPackages =
            new String[] { "de.ids_mannheim.korap.web.service.full",
                    "de.ids_mannheim.korap.web.filter",
                    "de.ids_mannheim.korap.web.utils" };

    protected TestHelper helper () {
        try {
            return TestHelper.newInstance(this.context);
        }
        catch (Exception e) {
            return null;
        }
    }


    @Override
    protected ContextHolder getContext () {
        return helper().getContext();
    }


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
    public void startServerBeforeFirstTestRun () {
        if (testContainer == null) {
            int port = ThreadLocalRandom.current().nextInt(5000, 8000 + 1);
            initServer(port, classPackages);
            startServer();
        }
    }

}
