package com.cong.fishisland.datasource.ai;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BigFishMemoryClassifierTest {
    private static final String FOOD_PREFERENCE = "我喜欢吃板烧鸡腿堡";
    private final SiliconFlowDataSource assistant = mock(SiliconFlowDataSource.class);
    private final BigFishMemoryClassifier classifier = new BigFishMemoryClassifier(assistant);

    @ParameterizedTest
    @ValueSource(strings = {"", "记住了吗大肥鱼"})
    void extractsAStatedPreferenceWithOrWithoutAnExplicitRequest(String suffix) {
        String statement = FOOD_PREFERENCE + suffix;
        List<String> current = Collections.singletonList(statement);
        when(assistant.classifyForMoyuAssistant(anyList())).thenAnswer(call -> {
            List<SiliconFlowRequest.Message> messages = call.getArgument(0);
            assertEquals(2, messages.size());
            assertEquals("system", messages.get(0).getRole());
            assertEquals("user", messages.get(1).getRole());
            assertTrue(messages.get(0).getContent().contains("‘" + statement + "’"));
            assertEquals(current, JSON.parseObject(messages.get(1).getContent()).getJSONArray("本批"));
            return "[\"" + FOOD_PREFERENCE + "\"]\n";
        });
        assertEquals(Collections.singletonList(FOOD_PREFERENCE),
                classifier.requestedMemories(Collections.emptyList(), current));
    }

    @Test
    void aReferenceCanSelectEarlierTextInTheSameConversation() {
        List<String> earlier = Collections.singletonList("我在练习画帆船");
        when(assistant.classifyForMoyuAssistant(anyList())).thenReturn("[\"我在练习画帆船\"]");
        assertEquals(earlier, classifier.requestedMemories(earlier, Collections.singletonList("下次别忘了这个")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"[]", "", "YES", "[", "[true]", "[\"它喜欢吃鱼\"]"})
    void emptyMalformedAndInventedExcerptsDoNotAllowWriting(String output) {
        when(assistant.classifyForMoyuAssistant(anyList())).thenReturn(output);
        assertTrue(classifier.requestedMemories(Collections.emptyList(), Collections.singletonList(FOOD_PREFERENCE)).isEmpty());
    }
}
