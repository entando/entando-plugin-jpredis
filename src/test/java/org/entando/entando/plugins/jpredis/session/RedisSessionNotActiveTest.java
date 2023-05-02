package org.entando.entando.plugins.jpredis.session;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import javax.servlet.Filter;
import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.plugins.jpredis.utils.RedisTestExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testcontainers.containers.GenericContainer;

@ExtendWith(RedisTestExtension.class)
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
class RedisSessionNotActiveTest {

    @BeforeAll
    static void setUp() {
        TestEntandoJndiUtils.setupJndi();
    }

    @Autowired
    private Filter springSessionRepositoryFilter;

    @Autowired
    private RedisClient redisClient;

    @Test
    void testRedisSessionNotActive(GenericContainer redisContainer) throws Exception {
        Assertions.assertFalse(springSessionRepositoryFilter instanceof SessionRepositoryFilter);
        Assertions.assertTrue(springSessionRepositoryFilter.getClass().getName().contains("DefaultSessionConfig"));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            Assertions.assertFalse(connection.sync().keys("Entando_*").isEmpty());  // Redis is used as cache
            Assertions.assertTrue(
                    connection.sync().keys("spring:session:*").isEmpty()); // Redis is not used for session
        }
    }
}
