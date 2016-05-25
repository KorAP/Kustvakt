package de.ids_mannheim.korap.suites;

/**
 * @author hanl
 * @date 09/03/2016
 */

import de.ids_mannheim.korap.config.CollectionLoaderTest;
import de.ids_mannheim.korap.config.PolicyLoaderTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ PolicyLoaderTest.class, CollectionLoaderTest.class })
public class LoaderTestSuite {}
