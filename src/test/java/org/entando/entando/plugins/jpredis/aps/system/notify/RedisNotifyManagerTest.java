package org.entando.entando.plugins.jpredis.aps.system.notify;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.common.notify.ApsEvent;
import io.lettuce.core.internal.LettuceFactories;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.neethi.Assertion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisNotifyManagerTest extends BaseTestCase {

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
    void testNotify() throws Exception {
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
        BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();
        BlockingQueue<Long> counts = LettuceFactories.newBlockingQueue();
        Assertions.assertEquals(0, channels.size());
        Assertions.assertEquals(0, counts.size());

        Assertions.assertNotNull(this.redisNotifyManager);
        redisNotifyManager.addListener("testchannel", new DefaultRedisPubSubListener(messages, channels, counts));
        Map<String, String> properties = new HashMap<>();
        properties.put("aaa", "111");
        properties.put("bbb", "222");
        properties.put("ccc", "333");
        TestEvent event = new TestEvent("testchannel", properties);

        redisNotifyManager.notify(event);
        synchronized (this) {
            wait(1000);
        }
        Assertions.assertEquals(1, counts.size());
        Assertions.assertEquals(1, messages.size());
        String received = messages.take();
        Assertions.assertNotNull(received);
        Map<String, String> extractedProperties = TestEvent.getProperties(received);
        Assertions.assertEquals(3, extractedProperties.size());
        Assertions.assertEquals("111", extractedProperties.get("aaa"));
        Assertions.assertEquals("222", extractedProperties.get("bbb"));
        Assertions.assertEquals("333", extractedProperties.get("ccc"));
    }

}
