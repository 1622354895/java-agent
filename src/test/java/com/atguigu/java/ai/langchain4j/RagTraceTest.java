package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.entity.RagTrace;
import com.atguigu.java.ai.langchain4j.mapper.RagTraceMapper;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RagTraceTest {

    @Autowired
    private ContentRetriever contentRetrieverXiaozhiPincone;

    @Autowired
    private RagTraceMapper ragTraceMapper;

    @Test
    public void shouldSaveRagTraceWhenRetrieveKnowledge() {
        Long beforeCount = ragTraceMapper.selectCount(null);

        List<Content> contents = contentRetrieverXiaozhiPincone.retrieve(
                Query.from("介绍一下神经内科，并告诉我有哪些医生")
        );

        Long afterCount = ragTraceMapper.selectCount(null);

        assertFalse(contents.isEmpty());
        assertTrue(afterCount > beforeCount);

        List<RagTrace> traces = ragTraceMapper.selectList(null);
        assertFalse(traces.isEmpty());
    }
}