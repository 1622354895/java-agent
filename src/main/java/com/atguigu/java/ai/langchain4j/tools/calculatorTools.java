package com.atguigu.java.ai.langchain4j.tools;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.MemoryId;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Component;

/*
* `@Component` 是 Spring 框架的注解，作用：

1. **标识组件**：标记 `CalculatorTools` 类为 Spring 管理的 Bean 组件
2. **自动注册**：Spring 启动时自动扫描并创建该类的实例
3. **依赖注入**：其他类可通过 `@Autowired` 注入此工具类使用计算器功能

简言之：让 Spring 容器管理这个工具类，实现自动装配和复用。
* */
@Component
public class calculatorTools {


    /*
    * @Tool 是 LangChain4j 的工具注解，作用：
        标识工具方法：标记该方法为 AI 可调用的工具（如计算器功能）
        自动注册：LangChain4j 扫描并注册该工具到 Agent 的工具列表中
        AI 自主调用：当用户提问涉及计算时，AI 会自动识别并调用此方法获取结果
        简言之：让大语言模型能够主动使用这个计算方法来完成数学运算任务。
        *
        *

        总结：大模型通过分析工具的名称、参数、描述，结合用户问题的语义，自主判断应该调用哪个工具来完成特定任务
    * */

    /*
    *
    * 参数说明
        name = "加法运算"
        作用：自定义工具的显示名称
        默认行为：如果不指定，使用方法名（如 sum）
        效果：AI 看到的工具名称是"加法运算"而不是"sum"
        优势：中文名称更符合中文用户的语义理解
        *
        value = "将两个参数a和b相加并返回运算结果"
        作用：提供工具的详细描述
        默认行为：如果不指定，LangChain4j 会尝试从方法签名和 JavaDoc 中提取描述
        效果：明确告诉 AI 这个工具的功能、参数含义和返回值
        优势：提高 AI 选择工具的准确性
    * */
    @Tool(name = "加法运算",value = "将两个参数a和b相加并返回运算结果")
    double sum(
            /*
            *
            @MemoryId → 用于 AI 服务接口，实现多用户会话隔离
            工具方法 → 纯函数，只需要业务参数（如 a、b），不需要 memoryId
            * */
            @MemoryId int memoryId,

            //@P 注解用于自定义工具参数的描述信息，其中 value 指定参数的中文名称（如"加数1"），required = true 标记该参数为必填项，帮助大模型更准确地理解每个参数的含义和使用要求。
            @P(value="加数1",required = true)double a,
            @P(value="加数2",required = true)double b
    ){
        System.out.println("调用加法运算"+"memoryId"+memoryId);
        return a+b;
    }

    @Tool(name = "平方根运算") //只有一个参数就不用写属性（name这个）的名字
    double squreRoot(
            @MemoryId int memoryId,
            double a
    ){
        System.out.println("调用平方根运算"+"memoryId"+memoryId);
        return Math.sqrt(a);
    }
}
