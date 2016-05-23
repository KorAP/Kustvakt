package de.ids_mannheim.korap.suites;

import de.ids_mannheim.korap.handlers.ResourceDaoTest;
import de.ids_mannheim.korap.handlers.UserDaoTest;
import de.ids_mannheim.korap.security.PolicyDaoTest;
import de.ids_mannheim.korap.user.UserdataTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author hanl
 * @date 26/02/2016
 */

@Deprecated
@RunWith(Suite.class)
@Suite.SuiteClasses({ PolicyDaoTest.class, UserdataTest.class,
        UserDaoTest.class, ResourceDaoTest.class })
public class DaoTestSuite {}
