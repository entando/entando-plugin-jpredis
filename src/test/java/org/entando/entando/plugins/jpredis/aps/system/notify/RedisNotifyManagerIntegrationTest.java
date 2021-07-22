package org.entando.entando.plugins.jpredis.aps.system.notify;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.services.lang.events.LangsChangedEvent;
import io.lettuce.core.internal.LettuceFactories;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedisNotifyManagerIntegrationTest extends BaseTestCase {

    private RedisNotifyManager redisNotifyManager = null;

    @BeforeEach
    public void init() throws Exception {
        try {
            redisNotifyManager = BaseTestCase.getApplicationContext().getBean(RedisNotifyManager.class);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }

    @Test
    void testNotifyEvent() throws Exception {
        DefaultRedisPubSubListener listener = this.createListener();
        Assertions.assertNotNull(this.redisNotifyManager);
        this.redisNotifyManager.addListener("testchannel", listener);
        LangsChangedEvent event = new LangsChangedEvent();
        redisNotifyManager.notify(event);
        synchronized (this) {
            wait(1000);
        }
        Assertions.assertEquals(1, listener.getCounts().size());
        Assertions.assertEquals(0, listener.getMessages().size());
    }

    @Test
    void testNotifyCustomEvent() throws Exception {
        DefaultRedisPubSubListener listener = this.createListener();
        Assertions.assertEquals(0, listener.getMessages().size());
        Assertions.assertEquals(0, listener.getCounts().size());

        Assertions.assertNotNull(this.redisNotifyManager);
        redisNotifyManager.addListener("testchannel", listener);
        Map<String, String> properties = new HashMap<>();
        properties.put("aaa", "111");
        properties.put("bbb", "222");
        properties.put("ccc", "333");
        TestEvent event = new TestEvent("testchannel", properties);

        redisNotifyManager.notify(event);
        synchronized (this) {
            wait(2000);
        }
        Assertions.assertEquals(1, listener.getCounts().size());
        Assertions.assertEquals(1, listener.getMessages().size());
        String received = listener.getMessages().take();
        Assertions.assertNotNull(received);
        Map<String, String> extractedProperties = TestEvent.getProperties(received);
        Assertions.assertEquals(3, extractedProperties.size());
        Assertions.assertEquals("111", extractedProperties.get("aaa"));
        Assertions.assertEquals("222", extractedProperties.get("bbb"));
        Assertions.assertEquals("333", extractedProperties.get("ccc"));
    }

    private DefaultRedisPubSubListener createListener() {
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
        BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();
        BlockingQueue<Long> counts = LettuceFactories.newBlockingQueue();
        return new DefaultRedisPubSubListener(messages, channels, counts);
    }

}
