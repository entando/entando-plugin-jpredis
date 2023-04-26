package org.entando.entando.plugins.jpredis.session;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.List;
import javax.servlet.Filter;
import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.plugins.jpredis.aps.system.redis.RedisEnvironmentVariables;
import org.entando.entando.plugins.jpredis.utils.RedisTestExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
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
class RedisSessionActiveTest {

    @BeforeAll
    static void setUp(MockedStatic<RedisEnvironmentVariables> mockedEnv) {
        mockedEnv.when(() -> RedisEnvironmentVariables.redisSessionActive()).thenReturn(true);
        TestEntandoJndiUtils.setupJndi();
    }

    @Autowired
    private Filter springSessionRepositoryFilter;

    @Autowired
    private RedisClient redisClient;

    @Test
    void testRedisSessionActive(GenericContainer redisContainer) throws Exception {
        Assertions.assertTrue(springSessionRepositoryFilter instanceof SessionRepositoryFilter);
        SessionRepositoryFilter sessionRepositoryFilter = ((SessionRepositoryFilter) springSessionRepositoryFilter);
        SessionRepository sessionRepository = (SessionRepository) ReflectionTestUtils.getField(sessionRepositoryFilter,
                "sessionRepository");
        Assertions.assertTrue(sessionRepository instanceof RedisIndexedSessionRepository);
        sessionRepository.save(sessionRepository.createSession());

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            Assertions.assertFalse(connection.sync().keys("Entando_*").isEmpty());  // Redis is used as cache
            List<String> sessionKeys = connection.sync().keys("spring:session:*");
            Assertions.assertFalse(sessionKeys.isEmpty());  // Redis is used as session
            for (String key : sessionKeys) { // cleanup session data for other tests
                connection.sync().del(key);
            }
        }
    }
}
