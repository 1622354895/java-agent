package com.atguigu.java.ai.langchain4j.controller;


import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import com.atguigu.java.ai.langchain4j.service.SafetyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 小智智能体控制器
 * 提供 RESTful API 接口，接收前端聊天请求并返回 AI 回复
 */

/**
 * Swagger/OpenAPI 文档标签注解
 * 在 API 文档中将所有接口归类到"小智智能体"分组下，便于前端开发者查阅
 */
@Tag(name = "小智智能体")
/**
 * Spring MVC RESTful 控制器注解
 * 标识该类为 REST API 控制器，自动将所有方法的返回值序列化为 JSON 格式响应
 * 等价于 @Controller + @ResponseBody 的组合
 */
@RestController
/**
 * URL 路径映射注解
 * 定义该控制器下所有接口的统一前缀路径为 /xiaozhi
 * 例如：chat 方法的完整访问路径为 POST http://localhost:8080/xiaozhi/chat
 */
@RequestMapping("/xiaozhi")
public class XiaozhiController {

    /**
     * 自动注入小智智能体服务实例
     * 由 Spring 容器管理，负责处理具体的 AI 对话逻辑
     */
    @Autowired
    private XiaozhiAgent xiaozhiagent;

    @Autowired
    private SafetyService safetyService; // 注入安全服务

    /**
     * 处理聊天请求的 API 端点
     * 接收前端发送的 ChatForm 对象，提取会话 ID 和用户消息，调用智能体生成回复
     *
     * @param chatForm 包含 memoryId（会话ID）和 message（用户消息）的请求体对象
     * @return AI 生成的回复内容字符串
     */


    /**
     * Swagger/OpenAPI 操作描述注解
     * summary = "对话"：在 API 文档中显示该接口的简要功能说明为"对话"
     * 前端开发者查看文档时能快速理解接口用途
     */
    @Operation(summary = "对话")
    /**
     * Spring MVC POST 请求映射注解
     * 定义该方法处理 POST 类型的 HTTP 请求，完整路径为 /xiaozhi/chat
     * POST 适用于提交数据（如发送聊天消息），相比 GET 更安全且无 URL 长度限制
     */
    @PostMapping(value = "/chat",produces = "text/stream;charset=utf-8") // POST 请求,
//    public String chat(@RequestBody ChatForm chatForm){
//        // 从请求表单中提取会话 ID 和用户消息，调用智能体的 chat 方法获取 AI 回复
//        return xiaozhiagent.chat(chatForm.getMemoryId(), chatForm.getMessage());
//    }
    public Flux<String> chat(@RequestBody ChatForm chatForm  ){           //改为返回 Flux<String>，流式输出

        /*
        *
        * 急症问题 -> 不进大模型，直接急诊提醒
            Prompt 注入 -> 不进大模型，直接拒绝
            明显非医疗问题 -> 不进大模型，直接拒答
            正常导诊/预约/RAG -> 继续走原来的 Agent
        * */

        // 1. 前置安全校验：在调用大模型之前，对用户输入做确定性规则检查
        // 覆盖三类拦截：Prompt 注入攻击、急症高风险、非医疗无关内容
        SafetyService.SafetyCheckResult safetyResult =
                safetyService.checkInput(chatForm.getMessage());

        // 2. 命中拦截规则：直接返回预设的安全回复，不调用大模型、不执行 RAG 检索
        // 优势：毫秒级响应、零 API 成本、结果100%可控，从源头规避安全风险
        if (safetyResult.blocked()) {
            // Flux.just 将单条文本包装为响应式流，与接口返回类型保持一致
            return Flux.just(safetyResult.reply());
        }

        // 3. 正常业务请求：提取会话ID和用户消息，调用智能体执行完整对话流程
        // 内部包含多轮记忆、RAG 知识库检索、工具调用、大模型生成全链路，流式返回结果
        return xiaozhiagent.chat(chatForm.getMemoryId(), chatForm.getMessage());
    }
}