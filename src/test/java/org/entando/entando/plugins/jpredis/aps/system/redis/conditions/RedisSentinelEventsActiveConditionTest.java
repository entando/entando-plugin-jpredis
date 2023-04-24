package org.entando.entando.plugins.jpredis.aps.system.redis.conditions;

import org.springframework.context.annotation.Condition;

class RedisSentinelEventsActiveConditionTest extends BaseRedisConditionTest {

    RedisSentinelEventsActiveConditionTest() {
        super(RedisSentinelEventsActive.class);
    }

    @Override
    Condition getCondition(boolean value) {
        return new RedisSentinelEventsActiveCondition(value);
    }
}
