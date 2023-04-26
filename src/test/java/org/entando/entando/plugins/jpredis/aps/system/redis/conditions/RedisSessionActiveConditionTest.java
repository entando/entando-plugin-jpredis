package org.entando.entando.plugins.jpredis.aps.system.redis.conditions;

import org.springframework.context.annotation.Condition;

class RedisSessionActiveConditionTest extends BaseRedisConditionTest {

    RedisSessionActiveConditionTest() {
        super(RedisSessionActive.class);
    }

    @Override
    Condition getCondition(boolean value) {
        return new RedisSessionActiveCondition(value);
    }
}
