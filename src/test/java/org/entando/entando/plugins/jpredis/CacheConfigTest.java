package org.entando.entando.plugins.jpredis;

import static org.assertj.core.api.Assertions.assertThat;

import org.entando.entando.plugins.jpredis.aps.system.redis.CacheConfig;
import org.junit.Test;

public class CacheConfigTest {

    @Test
    public void testIt() {
        CacheConfig cacheConfig = new CacheConfig();
        assertThat(cacheConfig).isNotNull();
    }
}
