可以做，而且这是你当前项目最合适的“轻量多 Agent”版本。

不要一上来拆成一堆 Agent。你的第一版目标就是：

```text
RouterAgent
-> KnowledgeAgent
-> AppointmentAgent
-> 其他继续走原 XiaozhiAgent
```

这样改动小、风险低、还能在简历里讲“基于意图路由的多 Agent 编排”。

**目标结构**

当前是：

```text
XiaozhiController
-> SafetyService
-> XiaozhiAgent
```

改完后是：

```text
XiaozhiController
-> SafetyService
-> MedicalAgentOrchestrator
   -> RouterAgent
   -> KnowledgeAgent
   -> AppointmentAgent
   -> XiaozhiAgent
```

其中：

```text
RouterAgent：只判断意图，不回答用户
KnowledgeAgent：只负责 RAG 知识库问答
AppointmentAgent：只负责号源查询、预约、取消
XiaozhiAgent：兜底，继续处理普通医疗导诊和其他原有能力
```

---

## 1. 新增意图枚举

新增文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/assistant/IntentType.java
```

代码：

```java
package com.atguigu.java.ai.langchain4j.assistant;

public enum IntentType {

    KNOWLEDGE,
    APPOINTMENT,
    XIAOZHI;

    public static IntentType from(String value) {
        if (value == null) {
            return XIAOZHI;
        }

        String normalized = value.trim().toUpperCase();

        if (normalized.contains("KNOWLEDGE")) {
            return KNOWLEDGE;
        }

        if (normalized.contains("APPOINTMENT")) {
            return APPOINTMENT;
        }

        return XIAOZHI;
    }
}
```

为什么只保留三个？

因为你第一版多 Agent 不需要拆太细：

```text
知识库类 -> KnowledgeAgent
预约挂号类 -> AppointmentAgent
其他 -> 原 XiaozhiAgent
```

这样最稳。

---

## 2. 新增 RouterAgent

新增文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/assistant/RouterAgent.java
```

代码：

```java
package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel"
)
public interface RouterAgent {

    @SystemMessage(fromResource = "router-agent-prompt.txt")
    String route(@UserMessage String message);
}
```

注意：

```text
RouterAgent 不要绑定 memory
RouterAgent 不要绑定 RAG
RouterAgent 不要绑定 tools
```

它只做一件事：判断用户问题应该交给哪个 Agent。

---

## 3. 新增 Router Prompt

新增文件：

```text
src/main/resources/router-agent-prompt.txt
```

内容：

```text
你是云诊助手的意图路由器。

你的任务不是回答用户问题，而是判断用户问题应该交给哪个 Agent 处理。

你只能返回以下三个标签之一，不要输出解释、标点、换行或多余文本：

KNOWLEDGE
APPOINTMENT
XIAOZHI

分类规则：

1. 返回 KNOWLEDGE 的情况：
用户询问医院信息、科室介绍、医生介绍、医生擅长方向、就医流程、挂号规则、院内知识、某个科室主要看什么病等问题。
示例：
- 介绍一下神经内科
- 神经内科有哪些医生
- 云康互联网医院有哪些科室
- 如何修改挂号信息
- 神经内科主要看哪些疾病

2. 返回 APPOINTMENT 的情况：
用户想查询号源、预约挂号、取消预约、询问某医生某天某时段是否有号。
示例：
- 梅元武明天上午有号吗
- 帮我预约神经内科
- 取消我明天上午的预约
- 查询皮肤科明天下午有没有号
- 帮张三预约神经内科

3. 返回 XIAOZHI 的情况：
用户描述症状、询问应该挂什么科、普通健康咨询、无法明确分类的问题。
示例：
- 我头疼恶心应该挂什么科
- 我胃疼反酸怎么办
- 最近睡眠不好应该看什么科
- 你好
- 你能做什么

必须严格只输出一个标签：
KNOWLEDGE
APPOINTMENT
XIAOZHI
```

这个 Prompt 要非常硬，不然 Router 会输出解释文本。

---

## 4. 新增 KnowledgeAgent

新增文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/assistant/KnowledgeAgent.java
```

代码：

```java
package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderXiaozhi",
        contentRetriever = "contentRetrieverXiaozhiPincone"
)
public interface KnowledgeAgent {

    @SystemMessage(fromResource = "knowledge-agent-prompt.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String message);
}
```

它只绑定：

```text
流式模型
MongoDB 记忆
Pinecone RAG
```

不绑定预约工具。

---

## 5. 新增 Knowledge Prompt

新增文件：

```text
src/main/resources/knowledge-agent-prompt.txt
```

内容：

```text
你是云康互联网医院的知识库问答 Agent，负责回答医院、科室、医生、就医流程、挂号规则等院内知识问题。

你的能力范围：
1. 医院信息问答
2. 科室介绍
3. 医生介绍
4. 医生擅长方向
5. 就医流程
6. 挂号规则
7. 检查前准备、发票、缴费等院内流程说明

回答要求：
1. 优先依据知识库上下文回答。
2. 如果知识库中没有相关内容，应明确说明“当前知识库中没有查询到相关信息”，不要编造。
3. 不做最终诊断，不替代医生诊断。
4. 不处理预约挂号、取消预约、号源查询。遇到这类问题时，提示用户可以重新说明预约需求。
5. 不回答非医疗、非就医流程问题。

引用来源规则：
1. 只有当回答直接使用了上下文中的【文档来源：xxx.md】内容时，才在回答末尾输出“参考来源：xxx.md”。
2. 来源名称必须严格使用上下文中的文档名，不得编造。
3. 如果没有使用知识库内容，不要输出参考来源。
4. 多个来源用顿号分隔。
5. 参考来源只放在回答末尾，单独成段。

示例：
神经内科主要接诊头痛、头晕、脑血管疾病、癫痫、帕金森病等相关问题。

参考来源：神经内科.md
```

---

## 6. 新增 AppointmentAgent

新增文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/assistant/AppointmentAgent.java
```

代码：

```java
package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderXiaozhi",
        tools = "appointmentTools"
)
public interface AppointmentAgent {

    @SystemMessage(fromResource = "appointment-agent-prompt.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String message);
}
```

它只绑定：

```text
流式模型
MongoDB 记忆
appointmentTools
```

不绑定 RAG。

---

## 7. 新增 Appointment Prompt

新增文件：

```text
src/main/resources/appointment-agent-prompt.txt
```

内容：

```text
你是云康互联网医院的预约挂号 Agent，负责处理号源查询、预约挂号和取消预约。

你的能力范围：
1. 查询医生或科室在指定日期、时间是否有号源。
2. 根据用户提供的信息预约挂号。
3. 根据用户提供的信息取消预约。
4. 当信息不完整时，向用户追问缺失字段。

必须调用工具的场景：
1. 用户询问是否有号源，必须调用“查询是否有号源”工具。
2. 用户要求预约挂号，必须调用“预约挂号”工具。
3. 用户要求取消预约，必须调用“取消预约挂号”工具。

禁止行为：
1. 不得凭空编造号源。
2. 不得凭空说预约成功。
3. 不得凭空说取消成功。
4. 工具返回失败时，必须如实告诉用户失败原因。
5. 工具返回无号源时，必须如实告诉用户无号源。

预约挂号必填信息：
1. 姓名
2. 身份证号
3. 科室
4. 日期，格式 yyyy-MM-dd
5. 时间，上午或下午

医生姓名可选：
- 如果用户指定医生，按用户指定医生预约。
- 如果用户没有指定医生，可以让工具按科室、日期、时间匹配可用医生。

取消预约必填信息：
1. 姓名
2. 身份证号
3. 科室
4. 日期，格式 yyyy-MM-dd
5. 时间，上午或下午

日期规则：
1. 用户说“明天”，需要结合当前日期换算成具体日期。
2. 预约日期不得早于今天。
3. 如果日期不明确，必须追问。

回答风格：
1. 简洁明确。
2. 预约成功时列出姓名、科室、医生、日期、时间。
3. 预约失败时说明失败原因。
4. 取消成功时说明已释放号源。
5. 不需要输出参考来源。
```

---

## 8. 新增 Orchestrator 接口

新增文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/service/MedicalAgentOrchestrator.java
```

代码：

```java
package com.atguigu.java.ai.langchain4j.service;

import reactor.core.publisher.Flux;

public interface MedicalAgentOrchestrator {

    Flux<String> chat(Long memoryId, String message);
}
```

---

## 9. 新增 Orchestrator 实现类

新增文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/service/impl/MedicalAgentOrchestratorImpl.java
```

代码：

```java
package com.atguigu.java.ai.langchain4j.service.impl;

import com.atguigu.java.ai.langchain4j.assistant.AppointmentAgent;
import com.atguigu.java.ai.langchain4j.assistant.IntentType;
import com.atguigu.java.ai.langchain4j.assistant.KnowledgeAgent;
import com.atguigu.java.ai.langchain4j.assistant.RouterAgent;
import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.service.MedicalAgentOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MedicalAgentOrchestratorImpl implements MedicalAgentOrchestrator {

    @Autowired
    private RouterAgent routerAgent;

    @Autowired
    private KnowledgeAgent knowledgeAgent;

    @Autowired
    private AppointmentAgent appointmentAgent;

    @Autowired
    private XiaozhiAgent xiaozhiAgent;

    @Override
    public Flux<String> chat(Long memoryId, String message) {
        IntentType intent = route(message);

        return switch (intent) {
            case KNOWLEDGE -> knowledgeAgent.chat(memoryId, message);
            case APPOINTMENT -> appointmentAgent.chat(memoryId, message);
            case XIAOZHI -> xiaozhiAgent.chat(memoryId, message);
        };
    }

    private IntentType route(String message) {
        try {
            String routeResult = routerAgent.route(message);
            return IntentType.from(routeResult);
        } catch (Exception e) {
            return IntentType.XIAOZHI;
        }
    }
}
```

为什么要 `try-catch`？

因为 RouterAgent 调大模型，可能偶发失败。失败时直接走原来的 `XiaozhiAgent`，保证主链路不挂。

---

## 10. 修改 Controller

修改文件：

```text
src/main/java/com/atguigu/java/ai/langchain4j/controller/XiaozhiController.java
```

原来注入的是：

```java
@Autowired
private XiaozhiAgent xiaozhiagent;
```

改成：

```java
@Autowired
private MedicalAgentOrchestrator medicalAgentOrchestrator;
```

需要新增 import：

```java
import com.atguigu.java.ai.langchain4j.service.MedicalAgentOrchestrator;
```

然后把最后这一行：

```java
return xiaozhiagent.chat(chatForm.getMemoryId(), chatForm.getMessage());
```

改成：

```java
return medicalAgentOrchestrator.chat(chatForm.getMemoryId(), chatForm.getMessage());
```

SafetyService 不动，还是放在最前面。

最终逻辑应该是：

```java
if (safetyResult.blocked()) {
    return Flux.just(safetyResult.reply());
}

return medicalAgentOrchestrator.chat(chatForm.getMemoryId(), chatForm.getMessage());
```

---

## 11. 要不要删除原 XiaozhiAgent？

不要删。

`XiaozhiAgent` 继续作为兜底 Agent。

第一版多 Agent 的重点是稳定：

```text
能路由到 KnowledgeAgent
能路由到 AppointmentAgent
其他问题还能走原 XiaozhiAgent
```

这样不会破坏你现在已经跑通的功能。

---

## 12. 测试 RouterAgent

新增测试：

```text
src/test/java/com/atguigu/java/ai/langchain4j/RouterAgentTest.java
```

代码示例：

```java
package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.IntentType;
import com.atguigu.java.ai.langchain4j.assistant.RouterAgent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RouterAgentTest {

    @Autowired
    private RouterAgent routerAgent;

    @Test
    void routeKnowledgeQuestion() {
        Assumptions.assumeTrue(Boolean.getBoolean("runRouterEval"));

        String result = routerAgent.route("介绍一下神经内科，并告诉我有哪些医生");

        assertEquals(IntentType.KNOWLEDGE, IntentType.from(result));
    }

    @Test
    void routeAppointmentQuestion() {
        Assumptions.assumeTrue(Boolean.getBoolean("runRouterEval"));

        String result = routerAgent.route("梅元武明天上午有号吗");

        assertEquals(IntentType.APPOINTMENT, IntentType.from(result));
    }

    @Test
    void routeTriageQuestionToXiaozhi() {
        Assumptions.assumeTrue(Boolean.getBoolean("runRouterEval"));

        String result = routerAgent.route("我头疼恶心应该挂什么科");

        assertEquals(IntentType.XIAOZHI, IntentType.from(result));
    }
}
```

运行：

```powershell
mvn -Dtest=RouterAgentTest -DrunRouterEval=true test
```

---

## 13. 测试 Orchestrator

第一版可以先手动前端测：

```text
知识库问题：
介绍一下神经内科，并告诉我有哪些医生
预期：走 KnowledgeAgent，回答有参考来源。

预约问题：
梅元武明天上午有号吗
预期：走 AppointmentAgent，调用 queryDepartment。

导诊问题：
我头疼恶心应该挂什么科
预期：走 XiaozhiAgent。
```

后面再把 `MedicalAgentEvalTest` 扩展一个字段：

```json
{
  "id": "rag_001",
  "category": "rag",
  "question": "介绍一下神经内科，并告诉我有哪些医生",
  "expectedIntent": "KNOWLEDGE",
  "expectedKeywords": ["神经内科", "医生"],
  "requiresCitation": true
}
```

然后统计：

```text
Intent route accuracy
KnowledgeAgent citation pass rate
AppointmentAgent tool-state pass rate
XiaozhiAgent fallback pass rate
```

---

## 14. README 怎么改

多 Agent 做完后，README 可以加：

```text
## 多 Agent 编排

当前后端采用轻量多 Agent 结构：

- RouterAgent：识别用户意图
- KnowledgeAgent：处理医院、科室、医生、流程等 RAG 知识库问答
- AppointmentAgent：处理号源查询、预约挂号和取消预约
- XiaozhiAgent：处理普通医疗导诊和兜底回答

请求链路：

SafetyService
-> MedicalAgentOrchestrator
-> RouterAgent
-> KnowledgeAgent / AppointmentAgent / XiaozhiAgent
```

---

## 15. 简历怎么写

可以加到主要职责：

```text
● 设计轻量多 Agent 编排结构，新增 RouterAgent 对用户意图进行分流，将知识库问答交由 KnowledgeAgent 处理，将号源查询、预约和取消交由 AppointmentAgent 处理，普通导诊问题继续由原 Agent 兜底，降低单 Prompt 复杂度并提升 RAG 与 Tool Calling 的职责边界清晰度。
```

项目成果可以加：

```text
● 完成从单 Agent 到 Router + 专项 Agent 的架构升级，实现医疗知识问答、预约业务和普通导诊的分流处理，并为后续统计意图识别准确率、RAG 引用成功率和工具调用成功率提供基础。
```

---

## 16. 推荐实现顺序

按这个顺序做，最稳：

```text
1. 新增 IntentType
2. 新增 RouterAgent
3. 新增 router-agent-prompt.txt
4. 新增 RouterAgentTest，先测试路由
5. 新增 KnowledgeAgent + knowledge-agent-prompt.txt
6. 新增 AppointmentAgent + appointment-agent-prompt.txt
7. 新增 MedicalAgentOrchestrator
8. 修改 XiaozhiController 调 Orchestrator
9. 前端手动测试三类问题
10. 跑 Appointment Tool 状态评测
11. 更新 README 和简历描述
```

不要先动 `XiaozhiAgent`，也不要删旧代码。第一版目标是“平滑加一层路由”，不是重构整个项目。

---

## 17. 风险点

注意这几个问题：

```text
1. RouterAgent 可能偶尔输出解释文本
```

所以 `IntentType.from()` 要用 `contains()` 容错。

```text
2. RouterAgent 会多调用一次大模型
```

所以每次请求会多一次模型调用，响应会稍慢一点。后续可以把 RouterAgent 换成规则 + 小模型。

```text
3. AppointmentAgent 不接 RAG
```

如果用户问“神经内科有哪些医生并帮我预约”，Router 可能分到 KNOWLEDGE 或 APPOINTMENT。第一版先接受这个限制，后面可以让 Router 支持复合意图。

```text
4. 多 Agent 共用 memoryId
```

第一版可以共用，这样上下文连续。后续如果发现不同 Agent 上下文互相干扰，再给不同 Agent 加 memoryId 前缀或 suffix。

---

最关键一句：你这个多 Agent 第一版应该做成**轻量路由架构**，不要做成复杂的多 Agent 协作系统。这样既符合你当前代码，也足够写进简历。