package org.entando.entando.plugins.jpredis.aps.system.notify;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedisNotifyManagerTest {

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private RedisNotifyManager redisNotifyManager;

    @Test
    void testSubscribeListener_1() throws Exception {
        DefaultRedisPubSubListener listener = Mockito.mock(DefaultRedisPubSubListener.class);
        Mockito.when(redisClient.connectPubSub()).thenReturn(null);
        redisNotifyManager.addListener("testchannel", listener);
        Mockito.verify(redisClient, Mockito.times(1)).connectPubSub();
    }

    @Test
    void testSubscribeListener_2() throws Exception {
        DefaultRedisPubSubListener listener = Mockito.mock(DefaultRedisPubSubListener.class);
        StatefulRedisPubSubConnection<String, String> connection = Mockito.mock(StatefulRedisPubSubConnection.class);
        Mockito.when(redisClient.connectPubSub()).thenReturn(connection);
        RedisPubSubAsyncCommands commands = Mockito.mock(RedisPubSubAsyncCommands.class);
        Mockito.when(connection.async()).thenReturn(commands);
        redisNotifyManager.addListener("testchannel", listener);
        Mockito.verify(redisClient, Mockito.times(1)).connectPubSub();
        Mockito.verify(commands, Mockito.times(1)).subscribe(Mockito.anyString());
        Mockito.verify(commands, Mockito.times(0)).publish(Mockito.anyString(), Mockito.anyString());
        redisNotifyManager.destroy();
        Mockito.verify(connection, Mockito.times(1)).close();
    }

    @Test
    void testNotifyEvent_1() throws Exception {
        Mockito.when(redisClient.connectPubSub()).thenReturn(null);
        TestEvent event = new TestEvent("testchannel", new HashMap<>());
        redisNotifyManager.notify(event);
        Mockito.verify(redisClient, Mockito.times(1)).connectPubSub();
    }

    @Test
    void testNotifyEvent_2() throws Exception {
        StatefulRedisPubSubConnection<String, String> connection = Mockito.mock(StatefulRedisPubSubConnection.class);
        Mockito.when(redisClient.connectPubSub()).thenReturn(connection);
        RedisPubSubAsyncCommands commands = Mockito.mock(RedisPubSubAsyncCommands.class);
        Mockito.when(connection.async()).thenReturn(commands);
        TestEvent event = new TestEvent("testchannel", new HashMap<>());
        redisNotifyManager.notify(event);
        Mockito.verify(redisClient, Mockito.times(1)).connectPubSub();
        Mockito.verify(commands, Mockito.times(0)).subscribe(Mockito.anyString());
        Mockito.verify(commands, Mockito.times(1)).publish(Mockito.anyString(), Mockito.anyString());
        redisNotifyManager.destroy();
        Mockito.verify(connection, Mockito.times(1)).close();
    }

}
