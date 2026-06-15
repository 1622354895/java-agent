package com.atguigu.java.ai.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 这是一个文本嵌入测试类，功能如下：
 *
 * 1. 自动注入LangChain4j的EmbeddingModel模型
 * 2. 将中文文本"你好"转换为向量表示
 * 3. 输出向量的维度和具体内容
 *
 * 用于验证嵌入模型是否正常工作，将自然文本映射为数值向量供后续相似度计算使用。
 */
@SpringBootTest
public class EmbeddingTest {

    /*
    * 声明一个EmbeddingModel类型的私有成员变量，用于注入LangChain4j的文本嵌入模型实例。
    * 该模型负责将文本转换为向量表示，是RAG系统中实现语义检索的核心组件。
    *
    * Spring容器会自动找到由Starter创建的DashScope EmbeddingModel实例并注入。
    * */
    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    public void testEmbeddingModel() {
        Response<Embedding> embed = embeddingModel.embed("你好");

        System.out.println("向量维度: " + embed.content().vector().length);
        System.out.println("向量输出: " + embed.toString());
    }


    // 自动注入之前配置好的 EmbeddingStore Bean (即连接到 Pinecone 的实例)
    @Autowired
    private EmbeddingStore embeddingStore;

    /**
     * 将文本转换成向量，然后存储到 pinecone 中
     *
     * 参考:
     * https://docs.langchain4j.dev/tutorials/embedding-stores
     */
    @Test
    public void testPineconeEmbedded() {

        // --- 处理第一段文本 ---

        // 1. 创建第一个文本片段对象，内容为 "我喜欢羽毛球"
        //TextSegment.from() 是LangChain4j的静态工厂方法，用于创建文本片段对象。它将原始字符串（如"我喜欢羽毛球"）封装为TextSegment实例，
        // 作为向量化的输入单元，后续可通过EmbeddingModel转换为向量并存储到向量数据库中。
        TextSegment segment1 = TextSegment.from("我喜欢羽毛球");

        // 2. 调用 embeddingModel 将文本转换为向量数据
        // .content() 获取转换后的具体向量数值对象
        Embedding embedding1 = embeddingModel.embed(segment1).content();

        // 3. 将生成的向量(embedding1)和原始文本(segment1)一起存入向量数据库(embeddingStore就是配置的EmbeddingStoreConfig)
        // 这样以后就可以通过语义搜索找到这段文本
        embeddingStore.add(embedding1, segment1);


        // --- 处理第二段文本 ---

        // 4. 创建第二个文本片段对象，内容为 "今天天气很好"
        TextSegment segment2 = TextSegment.from("今天天气很好");

        // 5. 同样地，将第二段文本转换为向量
        Embedding embedding2 = embeddingModel.embed(segment2).content();

        // 6. 将第二段数据的向量和文本存入数据库
        embeddingStore.add(embedding2, segment2);
    }

    /**
     * Pinecone-相似度匹配测试方法
     * 功能：演示如何将用户的问题转换为向量，并在向量数据库中检索最相关的知识片段。
     *
     * 这段代码实现了向量语义搜索功能：
     *
     * 1. **向量化查询**：将问题文本转换为向量
     * 2. **构建搜索请求**：设置查询向量和返回数量
     * 3. **执行搜索**：在向量数据库中查找最相似的记录
     * 4. **输出结果**：打印相似度得分和匹配的原始文本
     *
     * 核心是通过余弦相似度进行语义匹配检索。
     */
    @Test
    public void embeddingSearch() {

        // --- 第一步：将自然语言问题向量化 ---
        // 1. 定义一个查询问题："你最喜欢的运动是什么？"
        // 2. 调用 embeddingModel.embed() 方法，利用嵌入模型（如 OpenAI Embedding）将该文本转换为高维向量数据。
        // 3. 调用 .content() 获取转换后的具体向量对象 (Embedding)。
        Embedding queryEmbedding = embeddingModel.embed("你最喜欢的运动是什么？").content();

        // --- 第二步：构建搜索请求配置 ---
        // 使用构建者模式 (Builder Pattern) 创建搜索请求对象
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                // 设置查询向量：告诉数据库我们要找与这个向量相似的内容
                .queryEmbedding(queryEmbedding)
                // 设置最大返回结果数：这里设置为 1，表示只返回最相似的那一条记录（Top-1）
                .maxResults(1)
                // (可选) 设置最小相似度阈值：例如设为 0.8，低于此分数的结果将被过滤掉。
                // 当前被注释掉了，意味着无论相似度多低都会返回结果。
                //.minScore(0.8)
                .build();

        // --- 第三步：执行搜索 ---
        // 调用 embeddingStore.search() 方法，将请求发送给 Pinecone 数据库。
        // 数据库会计算 queryEmbedding 与库中所有向量的距离（通常是余弦相似度），并返回结果集。
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // --- 第四步：解析搜索结果 ---
        // searchResult.matches() 返回一个包含所有匹配项的列表 (List<EmbeddingMatch>)。
        // .get(0) 获取列表中的第一个元素，即相似度最高的那一条匹配记录。
        EmbeddingMatch<TextSegment> embeddingMatch = searchResult.matches().get(0);

        // --- 第五步：输出相似度得分 ---
        // embeddingMatch.score() 返回该匹配项与查询问题的相似度分数（通常在 0 到 1 之间）。
        // 分数越接近 1，表示语义越相似。
        System.out.println(embeddingMatch.score()); // 控制台输出示例: 0.8144288515898701

        // --- 第六步：输出匹配的原始文本 ---
        // embeddingMatch.embedded() 获取存储在数据库中的完整对象（类型为 TextSegment）。
        // .text() 提取出原始的文本内容字符串。
        // 这通常用于后续的 RAG（检索增强生成）流程，作为上下文输入给大模型。
        System.out.println(embeddingMatch.embedded().text());
    }


//    @Test
//    public void testUploadKnowledgeLibrary() {
//
//        //使用FileSystemDocumentLoader读取指定目录下的知识库文档
//        //并使用默认的文档解析器对文档进行解析
//        Document document1 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/医院信息.md");
//        Document document2 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/科室信息.md");
//        Document document3 = FileSystemDocumentLoader.loadDocument("D:/Java-AI/神经内科.md");
//        List<Document> documents = Arrays.asList(document1, document2, document3);
//
//        //文本向量化并存入向量数据库：将每个片段进行向量化，得到一个嵌入向量
//        EmbeddingStoreIngestor
//                .builder()
//                .embeddingStore(embeddingStore)
//                .embeddingModel(embeddingModel)
//                .build()
//                .ingest(documents);
//    }

    /**
     * 这段代码实现知识库上传功能：
     *
     * 1. **加载文档**：读取三个Markdown文件并分段处理
     * 2. **文本分段**：按段落分割，每段最多700字符，超长文本进一步切分
     * 3. **添加元数据**：为每个文本片段添加文件名标记和来源标识
     * 4. **向量化存储**：使用嵌入模型生成向量，批量存入向量数据库
     */

    @Test
    public void testUploadKnowledgeLibrary() {
        List<TextSegment> segments = new ArrayList<>();

        segments.addAll(loadSegmentsWithSource("D:/Java-AI/医院信息.md", "医院信息.md"));
        segments.addAll(loadSegmentsWithSource("D:/Java-AI/科室信息.md", "科室信息.md"));
        segments.addAll(loadSegmentsWithSource("D:/Java-AI/神经内科.md", "神经内科.md"));

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.addAll(embeddings, segments);
    }

    private List<TextSegment> loadSegmentsWithSource(String path, String fileName) {
        Document document = FileSystemDocumentLoader.loadDocument(path);

        List<TextSegment> segments = new ArrayList<>();
        String[] paragraphs = document.text().split("\\R\\s*\\R");

        StringBuilder current = new StringBuilder();
        int maxChars = 700;

        for (String paragraph : paragraphs) {
            String text = paragraph.trim();
            if (text.isEmpty()) {
                continue;
            }

            if (current.length() + text.length() + 2 > maxChars && current.length() > 0) {
                segments.add(buildSegment(fileName, current.toString()));
                current.setLength(0);
            }

            if (text.length() > maxChars) {
                for (int start = 0; start < text.length(); start += maxChars) {
                    int end = Math.min(start + maxChars, text.length());
                    segments.add(buildSegment(fileName, text.substring(start, end)));
                }
            } else {
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(text);
            }
        }

        if (current.length() > 0) {
            segments.add(buildSegment(fileName, current.toString()));
        }

        return segments;
    }

    private TextSegment buildSegment(String fileName, String text) {
        Metadata metadata = Metadata.from("file_name", fileName);

        return TextSegment.from(
                "【文档来源：" + fileName + "】\n" + text,
                metadata
        );
    }
}