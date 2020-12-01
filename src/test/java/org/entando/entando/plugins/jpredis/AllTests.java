package org.entando.entando.plugins.jpredis;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.entando.entando.plugins.jpredis.aps.system.redis.CacheConfig;
import org.entando.entando.plugins.jpredis.aps.system.services.CacheInfoManagerIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for Redis connector");

        System.out.println("Test for Redis connector");
        
        //
        suite.addTestSuite(CacheInfoManagerIntegrationTest.class);

        return suite;
    }

}
