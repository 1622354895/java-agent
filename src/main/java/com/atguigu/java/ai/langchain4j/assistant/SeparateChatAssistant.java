package com.atguigu.java.ai.langchain4j.assistant;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/*
        这个 SeparateChatAssistant 类是一个支持多用户隔离的 AI 聊天助手接口。
        核心功能
        多用户并发对话支持
        通过 @MemoryId int memoryId 参数区分不同用户/会话
        每个用户拥有独立的对话历史，互不干扰
        动态记忆管理
        使用 chatMemoryProvider = "chatMemoryProvider" 为每个 memoryId 动态创建专属的记忆实例
        每个用户的对话历史独立存储（如 MongoDB），最多保留 10 条消息
        模型绑定
        明确指定使用 qwenChatModel（通义千问）作为底层大语言模型
        采用显式依赖注入模式（EXPLICIT），避免 Bean 冲突
        系统提示词
        预设系统指令："你是我的好朋友，请用东北话回答问题"
        所有回复都会遵循这个风格设定

*
*
* */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",    // 绑定模型
//        chatModel = "openAiChatModel",    // 绑定模型
        chatMemoryProvider = "chatMemoryProvider",// 绑定记忆组件, 支持多轮对话上下文
        tools = "calculatorTools" //配置自定义工具
)
public interface SeparateChatAssistant {

//    @SystemMessage("你是我的好朋友，请用东北话回答问题。") // 系统提示语
    /*
    *  核心机制：模板引擎与参数注入
        这段代码使用了 LangChain4j 的提示词模板（Prompt Template） 功能。
        占位符： {{current_date}} 是一个标准的占位符语法（类似于 Mustache 或 Jinja2 模板）。
        拦截与替换： 当你调用 separateChatAssistant.chat(3, "今天几号") 时，LangChain4j 的代理层会拦截这次调用。
        * 它会扫描 @SystemMessage 中的字符串，发现里面有 {{...}} 格式的内容。
        自动填充： 框架会自动查找名为 current_date 的值。在 LangChain4j 中，有一些内置的隐式参数。如果框架检测到你没有显式传入 current_date 参数，但模板里用了它，
        * 它通常会尝试从系统环境获取当前时间并自动填入。
    * */
//    @SystemMessage("你是我的好朋友，请用重庆话回答问题。今天是{{current_date}}") // 系统提示语
    @SystemMessage(fromResource = "my-prompt-template.txt") // 系统提示语
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);// 用户提问

    @UserMessage("你是我的好朋友，请用粤语回答问题,{{message}}") // 消息占位符：{{it}} 会被替换为用户实际发送的消息内容
    String chat2(@MemoryId int memoryId, @V("message")  String userMessage);// 用户提问,@V("meeage")是占位符


    /**
     * 基于外部模板文件的个性化聊天方法
     * 从 my-prompt-template3.txt 加载系统提示词模板，支持动态变量替换
     *
     * @param memoryId 会话标识ID，用于区分不同用户的独立对话历史
     * @param userMessage 用户发送的具体消息内容
     * @param username 用户名变量，会被注入到模板中的 {{username}} 占位符
     * @param age 年龄变量，会被注入到模板中的 {{age}} 占位符
     * @return AI 根据模板和上下文生成的回复内容
     */
    @SystemMessage(fromResource = "my-prompt-template3.txt")
    String chat3(
            @MemoryId int memoryId,
            @UserMessage String userMessage,
            @V("username") String username,
            @V("age") int age
    );


}
