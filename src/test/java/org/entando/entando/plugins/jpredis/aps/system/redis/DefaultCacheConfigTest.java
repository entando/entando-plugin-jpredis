package org.entando.entando.plugins.jpredis.aps.system.redis;

import org.entando.entando.TestEntandoJndiUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath*:spring/testpropertyPlaceholder.xml",
        "classpath*:spring/baseSystemConfig.xml",
        "classpath*:spring/aps/**/**.xml",
        "classpath*:spring/plugins/**/aps/**/**.xml",
        "classpath*:spring/web/**.xml"
})
@WebAppConfiguration(value = "")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class DefaultCacheConfigTest {

    private static MockedStatic<RedisEnvironmentVariables> mockedRedisEnvironment;

    @BeforeAll
    static void setUp() {
        mockedRedisEnvironment = Mockito.mockStatic(RedisEnvironmentVariables.class);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.active()).thenReturn(false);
        TestEntandoJndiUtils.setupJndi();
    }

    @AfterAll
    static void tearDown() {
        mockedRedisEnvironment.close();
    }

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testDefaultEntandoCache() {
        Assertions.assertTrue(cacheManager instanceof DefaultEntandoCacheManager);
    }
}
