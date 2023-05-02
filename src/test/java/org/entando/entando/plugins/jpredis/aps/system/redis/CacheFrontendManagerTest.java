package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheFrontendManagerTest {

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private CacheFrontendManager cacheFrontendManager;

    @Test
    void testRebuildCacheFrontend() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());

            CacheFrontend<String, Object> cacheFrontend = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(cacheFrontend);

            cacheFrontendManager.getCacheFrontend();
            cacheFrontendManager.rebuildCacheFrontend();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(2));
            Mockito.verify(cacheFrontend, Mockito.times(1)).close();
        }
    }
}
