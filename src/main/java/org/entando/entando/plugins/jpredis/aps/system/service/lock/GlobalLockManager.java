package org.entando.entando.plugins.jpredis.aps.system.service.lock;

import com.agiletec.aps.system.ApsSystemUtils.ApsDeepDebug;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.sync.IGlobalLockManager;
import org.entando.entando.aps.system.services.sync.exception.GlobalLockEntException;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("GlobalLockManager")
@RedisActive(true)
@Order(10)
public class GlobalLockManager implements IGlobalLockManager {

    private static final Logger log = LoggerFactory.getLogger(GlobalLockManager.class);

    public static final String ENTANDO_GLOBAL_LOCK = "Entando_GlobalLock::";
    public static final int LOCK_TOKEN_SIZE = 16;

    private final RedisClient redisClient;
    private final SecureRandom rnd = new SecureRandom();

    private static final String UNLOCK_LUA =
            "if redis.call('hget', KEYS[1], 'token') == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else return 0 end";


    public GlobalLockManager(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        return redisClient;
    }

    private StatefulRedisConnection<String, String> redisSyncConnection() {
        return redisClient.connect();
    }

    private RedisCommands<String, String> redisCommands(StatefulRedisConnection<String, String> conn) {
        return conn.sync();
    }

    private RedisCommands<String, String> getSyncCommand() {
        StatefulRedisConnection<String, String> conn = redisSyncConnection();
        return redisCommands(conn);
    }

    @Override
    public String tryLock(String key, String property, Duration duration) throws GlobalLockEntException {
        try {
            if (StringUtils.isNotBlank(key)) {
                final String token = generateToken();
                final RedisCommands<String, String> cmd = getSyncCommand();

                key = formatLockName(key);
                Boolean created = cmd.hsetnx(key, LOCK_PROP_TOKEN, token);

                if (created) {
                    cmd.hset(key, Map.of(
                            LOCk_PROP_CREATED_AT, Instant.now().toString(),
                            LOCK_PROP_DATA, property));
                    cmd.pexpire(key, duration.toMillis());
                    return token;
                }
            }
            return null;
        } catch (RedisException e ) {
            throw new GlobalLockEntException("communication error with Redis detected while locking", e.getCause());
        }
    }

    @Override
    public String lock(String key, String property, Duration duration, Duration maxWait) throws GlobalLockEntException {
        long end = System.nanoTime() + maxWait.toNanos();

        while (System.nanoTime() < end) {
            final String token = tryLock(key, property, duration);

            if (StringUtils.isNotBlank(token)) {
                return token;
            }
            try {
                // retry with jitter
                Thread.sleep(ThreadLocalRandom.current().nextLong(20, 61));
            } catch (InterruptedException e) {
                log.warn("Thread interrupted exception!", e);
            }
        }
        return null;
    }

    @Override
    public boolean unlock(String key, String token) throws GlobalLockEntException {
        try {
            if (StringUtils.isNotBlank(key)
                    && StringUtils.isNotBlank(token)) {
                final RedisCommands<String, String> cmd = getSyncCommand();

                key = formatLockName(key);
                Long res = cmd.eval(UNLOCK_LUA, ScriptOutputType.INTEGER, new String[]{key}, token);
                ApsDeepDebug.print("global-lock", "releasing lock " + key + ": status " + (res != null && res == 1L));
                return (res != null && res == 1L);
            }
            return false;
        } catch(RedisException e ) {
            throw new GlobalLockEntException("communication error with Redis detected while unlocking", e.getCause());
        }
    }

    @Override
    public Optional<Map<String, String>> verifyLock(String key) throws GlobalLockEntException {
        try {
            if (StringUtils.isNotBlank(key)) {
                key = formatLockName(key);

                final RedisCommands<String, String> cmd = getSyncCommand();
                final Map<String, String> fields = cmd.hgetall(key);

                if (fields == null || fields.isEmpty()) {
                    return Optional.empty();
                }
                fields.remove(LOCK_PROP_TOKEN);

                ApsDeepDebug.print("global-lock-verify", "verifying lock " + key + ": has payload  " + !fields.isEmpty());
                return Optional.of(fields);
            }
            return Optional.empty();
        } catch (RedisException e ) {
            throw new GlobalLockEntException("communication error with Redis detected while verifying lock", e.getCause());
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[LOCK_TOKEN_SIZE];

        rnd.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte x : bytes) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private String formatLockName(String key) {
        return ENTANDO_GLOBAL_LOCK + key;
    }

}
