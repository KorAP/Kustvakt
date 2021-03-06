package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author hanl
 * @date 09/03/2016
 */
@NotThreadSafe
@RunWith(BeanConfigTest.SpringExtendedSetupListener.class)
@ContextConfiguration("classpath:test-config.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BeanConfigTest {

    private static Logger jlog = LogManager.getLogger(BeanConfigTest.class);
    @Autowired
    protected ApplicationContext context;

    public void init () throws Exception {
        assertNotNull("Application context must not be null!", this.context);
        jlog.debug("running one-time before init for class "
                + this.getClass().getSimpleName() + " ...");
//        BeansFactory.setKustvaktContext(getContext());
        assertNotNull(BeansFactory.getKustvaktContext());
    }

    public void close () {
        BeansFactory.closeApplication();
    }

    public static class SpringExtendedSetupListener
            extends SpringJUnit4ClassRunner {

        private BeanConfigTest instanceSetupListener;


        public SpringExtendedSetupListener (Class<?> clazz)
                throws InitializationError {
            super(clazz);
        }


        @Override
        protected Object createTest () throws Exception {
            Object test = super.createTest();
            // Note that JUnit4 will call this createTest() multiple times for each
            // test method, so we need to ensure to call "beforeClassSetup" only once.
            if (test instanceof BeanConfigTest
                    && instanceSetupListener == null) {
                instanceSetupListener = (BeanConfigTest) test;
                instanceSetupListener.init();
            }
            return test;
        }


        @Override
        public void run (RunNotifier notifier) {
            super.run(notifier);
            if (instanceSetupListener != null) {
                instanceSetupListener.close();
            }
        }
    }

//    protected ContextHolder getContext () {
//        return helper().getContext();
//    }
//
//    protected TestHelper helper () {
//        try {
//            return TestHelper.newInstance(this.context);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }
}
