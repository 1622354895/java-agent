package com.atguigu.java.ai.langchain4j;

// 导入 LangChain4j 的核心文档对象，代表一个被加载的知识片段
import dev.langchain4j.data.document.Document;
// 导入文件系统文档加载器，用于从本地磁盘读取文件
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
// 导入文本解析器，专门用于处理 .txt 等纯文本格式的文件内容
import dev.langchain4j.data.document.parser.TextDocumentParser;
// 导入 JUnit 5 的测试注解，用于标记这是一个测试方法
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.community.model.dashscope.QwenTokenizer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// 导入 Spring Boot 测试注解，用于启动 Spring 容器环境
import org.springframework.boot.test.context.SpringBootTest;

// 导入 Java NIO 文件系统的匹配器工厂，用于创建文件路径匹配规则
import java.nio.file.FileSystems;
// 导入路径匹配器接口，用于定义如 "*.txt" 这样的过滤规则
import java.nio.file.PathMatcher;
// 导入 List 集合接口，用于存储批量加载的 Document 对象
import java.util.List;

/**
 * RAG 测试类
 * @SpringBootTest 表示这是一个 Spring Boot 集成测试，会加载完整的 ApplicationContext
 *
 * 关键点总结
 * loadDocument vs loadDocuments: 前者返回单个 Document 对象，后者返回 List<Document> 集合。
 * TextDocumentParser: 这是 LangChain4j 提供的默认解析器，适合处理纯文本。如果你需要处理 PDF 或 Word，
 * 通常需要引入额外的依赖（如 langchain4j-document-parser-apache-pdfbox）并使用对应的 Parser。
 *
 * PathMatcher: 利用 Java NIO 的 Glob 语法（如 *.txt, data-?.csv）可以非常灵活地筛选特定类型的文件作为知识库来源。
 * loadDocumentsRecursively: 在处理深层级目录结构的知识库时非常有用，避免手动遍历文件夹。
 * 以上是对该段代码中核心方法的总结说明。
 *
 */
@SpringBootTest
public class RAGTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private QwenTokenizer qwenTokenizer;

    /**
     * 测试读取文档的方法
     * @Test 标记该方法为单元测试入口
     */
    @Test
    public void testReadDocument() {

        // --- 场景一：加载单个指定文档 ---
        // FileSystemDocumentLoader.loadDocument: 静态方法，用于加载单个文件
        // 参数1 "E:/knowledge/file.txt": 目标文件的绝对路径
        // 参数2 new TextDocumentParser(): 指定使用文本解析器来提取文件内容
        // 返回结果赋值给 document 变量
        Document document = FileSystemDocumentLoader.loadDocument("E:/knowledge/file.txt", new TextDocumentParser());

        // （可选）打印文档内容验证加载是否成功
        // System.out.println(document.text());


        // --- 场景二：加载目录下所有文档 ---
        // FileSystemDocumentLoader.loadDocuments: 静态方法，用于批量加载
        // 参数1 "E:/knowledge": 目标文件夹的路径
        // 参数2 new TextDocumentParser(): 同样指定文本解析器
        // 注意：此方法默认只扫描当前目录，不会进入子文件夹
        // 返回结果是一个 Document 列表
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("E:/knowledge", new TextDocumentParser());


        // --- 场景三：按文件名模式过滤加载（例如只加载 .txt） ---
        // FileSystems.getDefault().getPathMatcher("glob:*.txt"): 创建一个通配符匹配器
        // "glob:*.txt" 表示匹配所有以 .txt 结尾的文件
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.txt");

        // 调用 loadDocuments 的重载方法，传入 pathMatcher 进行过滤
        // 这样只会加载 E:/knowledge 目录下符合 *.txt 规则的文件
//        List<Document> documents = FileSystemDocumentLoader.loadDocuments("E:/knowledge", pathMatcher, new TextDocumentParser());


        // --- 场景四：递归加载（包含子目录） ---
        // FileSystemDocumentLoader.loadDocumentsRecursively: 专门用于递归遍历的方法
        // 它会深入 "E:/knowledge" 下的所有子文件夹查找文档
        // 参数1: 根目录路径
        // 参数2: 解析器实例
//        List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively("E:/knowledge", new TextDocumentParser());


    }



    /**
     * 解析PDF文档的测试方法
     * 该方法用于验证系统能否正确读取指定路径下的PDF文件并将其转换为Document对象
     */
    @Test // JUnit5注解，标记该方法为单元测试用例，运行时会独立执行
    public void testParsePDF() {
        // 调用FileSystemDocumentLoader的静态方法loadDocument来加载单个文档
        // 参数1："E:/knowledge/医院信息.pdf" -> 指定本地磁盘上PDF文件的绝对路径
        // 参数2：new ApachePdfBoxDocumentParser() -> 实例化Apache PDFBox解析器
        //       这是关键步骤，因为默认的文本解析器无法处理PDF二进制格式，必须显式指定PDF解析器
        Document document = FileSystemDocumentLoader.loadDocument(
                "E:/knowledge/医院信息.pdf",
                new ApachePdfBoxDocumentParser()
        );

        // 将解析后的Document对象打印到控制台
        // 输出内容通常包含文档的元数据（Metadata）以及提取出的文本内容（Text）
        System.out.println(document);
    }


    /**
     * 加载文档并存入向量数据库
     *
     * 这段代码实现RAG知识库构建流程：
     *
     * 1. **加载文档**：从指定路径读取Markdown文件
     * 2. **创建向量库**：初始化内存型向量存储
     * 3. **自动处理**：通过`EmbeddingStoreIngestor`完成文本分块、向量化、存储三步操作
     * 4. **输出验证**：打印向量库内容确认存储成功
     */
    @Test
    public void testReadDocumentAndStore() {

        // 使用FileSystemDocumentLoader读取指定目录下的知识库文档
        // 并使用默认的文档解析器对文档进行解析 (TextDocumentParser)
        Document document = FileSystemDocumentLoader.loadDocument("D:/Java-AI/测试.md");

        // 为了简单起见，我们暂时使用基于内存的向量存储
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();//embeddingStore是存储向量的数据库

        // ingest
        // 1、分割文档：默认使用递归分割器，将文档分割为多个文本片段，每个片段包含不超过 300个token，并且有 30个token的重叠部分保证连贯性
        // DocumentByParagraphSplitter(DocumentByLineSplitter(DocumentBySentenceSplitter(DocumentByWordSplitter)))
        // 2、文本向量化：使用一个LangChain4j内置的轻量化向量模型对每个文本片段进行向量化
        // 3、将原始文本和向量存储到向量数据库中(InMemoryEmbeddingStore)
        EmbeddingStoreIngestor
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document); //ingest是批量向量存储的入口点



        // 查看向量数据库内容
        System.out.println(embeddingStore);
    }


    /**
     * 文档分割
     *
     * 这段代码演示自定义文档分割流程：
     *
     * 1. **加载文档**：读取Markdown文件
     * 2. **创建向量库**：初始化内存存储
     * 3. **自定义分割器**：按段落切分，每段最多300 token，重叠30 token保证语义连贯
     * 4. **构建Ingestor**：通过Builder模式配置分割器和存储
     * 5. **执行摄入**：完成分块、向量化、存储一体化操作
     */
    @Test
    public void testDocumentSplitter() {

        // 使用FileSystemDocumentLoader读取指定目录下的知识库文档
        // 并使用默认的文档解析器对文档进行解析 (TextDocumentParser)
        Document document = FileSystemDocumentLoader.loadDocument("E:/knowledge/人工智能.md");

        // 为了简单起见，我们暂时使用基于内存的向量存储
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 自定义文档分割器
        // 按段落分割文档：每个片段包含不超过 300个token，并且有 30个token的重叠部分保证连贯性
        // 注意：当段落长度总和小于设定的最大长度时，就不会有重叠的必要。
        DocumentByParagraphSplitter documentSplitter = new DocumentByParagraphSplitter(
                300,
                30,
                // token分词器：按token计算
                qwenTokenizer);

        // 按字符计算
        // DocumentByParagraphSplitter documentSplitter = new DocumentByParagraphSplitter(300, 30);

        // 构建 Ingestor 并执行摄入操作
        EmbeddingStoreIngestor
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(documentSplitter)
                .build()
                .ingest(document);
    }

    @Test
    public void testTokenCount() {
        String text = "这是一个示例文本，用于测试 token 长度的计算。";
        UserMessage userMessage = UserMessage.userMessage(text);
//计算 token 长度
        int count = qwenTokenizer.estimateTokenCountInMessage(userMessage);
        System.out.println("token长度：" + count);
    }
}
