package de.ids_mannheim.korap.config;

import static org.junit.Assert.assertNotNull;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
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
@RunWith(BeanConfigBaseTest.SpringExtendedSetupListener.class)
@ContextConfiguration(classes = AppTestConfigBase.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BeanConfigBaseTest {

    private static Logger jlog = Logger.getLogger(BeanConfigBaseTest.class);
//    @Autowired
    protected ApplicationContext context;

    @Before
    public void init () throws Exception {
        context = new ClassPathXmlApplicationContext("test-default-config.xml");
        assertNotNull("Application context must not be null!", this.context);
        jlog.debug("running one-time before init for class "
                + this.getClass().getSimpleName() + " ...");
        BeansFactory.setKustvaktContext(getContext());
        assertNotNull(BeansFactory.getKustvaktContext());
        initMethod();
    }


    protected abstract ContextHolder getContext ();


    public void close () {
        BeansFactory.closeApplication();
    }

    public abstract void initMethod () throws KustvaktException;


//    protected TestHelper helper () {
//        try {
//            return TestHelper.newInstance(this.context);
//        }
//        catch (Exception e) {
//            return null;
//        }
//    }


    public static class SpringExtendedSetupListener extends
            SpringJUnit4ClassRunner {

        private BeanConfigBaseTest instanceSetupListener;


        public SpringExtendedSetupListener (Class<?> clazz)
                throws InitializationError {
            super(clazz);
        }


        @Override
        protected Object createTest () throws Exception {
            Object test = super.createTest();
            // Note that JUnit4 will call this createTest() multiple times for each
            // test method, so we need to ensure to call "beforeClassSetup" only once.
            if (test instanceof BeanConfigBaseTest && instanceSetupListener == null) {
                instanceSetupListener = (BeanConfigBaseTest) test;
                instanceSetupListener.init();
            }
            return test;
        }


        @Override
        public void run (RunNotifier notifier) {
            super.run(notifier);
            if (instanceSetupListener != null)
                instanceSetupListener.close();
        }


    }
}
