package org.entando.entando.plugins.jpredis.aps.system.redis.conditions;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.LinkedMultiValueMap;

@ExtendWith(MockitoExtension.class)
abstract class BaseRedisConditionTest {

    @Mock
    ConditionContext context;

    @Mock
    AnnotatedTypeMetadata metadata;

    private final Class conditionalAnnotation;

    BaseRedisConditionTest(Class conditionalAnnotation) {
        this.conditionalAnnotation = conditionalAnnotation;
    }

    abstract Condition getCondition(boolean value);

    @Test
    void testConditionalTrueEnvTrue() {
        mockConditionalAnnotation(true);
        Assertions.assertTrue(getCondition(true).matches(context, metadata));
    }

    @Test
    void testConditionalFalseEnvTrue() {
        mockConditionalAnnotation(false);
        Assertions.assertTrue(getCondition(true).matches(context, metadata));
    }

    @Test
    void testConditionalTrueEnvFalse() {
        mockConditionalAnnotation(true);
        Assertions.assertFalse(getCondition(false).matches(context, metadata));
    }

    @Test
    void testConditionalFalseEnvFalse() {
        mockConditionalAnnotation(false);
        Assertions.assertFalse(getCondition(false).matches(context, metadata));
    }

    @Test
    void testEmptyAnnotatedTypeMetadataEnvTrue() {
        Assertions.assertFalse(getCondition(true).matches(context, metadata));
    }

    @Test
    void testEmptyAnnotatedTypeMetadataEnvFalse() {
        Assertions.assertTrue(getCondition(false).matches(context, metadata));
    }

    void mockConditionalAnnotation(boolean value) {
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.put("value", List.of(true));
        Mockito.when(metadata.getAllAnnotationAttributes(conditionalAnnotation.getName())).thenReturn(map);
    }
}
