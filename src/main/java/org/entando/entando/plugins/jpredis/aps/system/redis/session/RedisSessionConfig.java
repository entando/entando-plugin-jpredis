package org.entando.entando.plugins.jpredis.aps.system.redis.session;

import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSessionActive;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
@RedisSessionActive(true)
public class RedisSessionConfig {

}
