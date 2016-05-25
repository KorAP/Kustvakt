package de.ids_mannheim.korap.suites;

import de.ids_mannheim.korap.security.PermissionBufferTest;
import de.ids_mannheim.korap.security.PolicyBuilderTest;
import de.ids_mannheim.korap.security.SecurityPolicyTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author hanl
 * @date 09/03/2016
 */
// test object serialization and I/O buffers
@RunWith(Suite.class)
@Suite.SuiteClasses({ PermissionBufferTest.class, PolicyBuilderTest.class,
        SecurityPolicyTest.class })
public class SecurityEntityTestSuite {}
