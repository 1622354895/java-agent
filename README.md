# 云诊助手 Agent 后端

云诊助手 Agent 是一个基于 Spring Boot 和 LangChain4j 的医疗导诊智能体后端项目，面向模拟互联网医院的咨询导诊、知识库问答、号源查询、预约挂号和取消预约场景。

本仓库只包含后端代码。前端页面已完成本地联调，前端代码位于：

```text
D:\Java-AI\xiaozhi-ui
```

前端基于 Vue 3 + Vite，开发环境通过 Vite 代理访问后端接口：

```text
POST /api/xiaozhi/chat -> http://localhost:8080/xiaozhi/chat
```

## 核心功能

- 医疗导诊问答：根据用户症状和就医需求，提供科室推荐、就医流程和常见医疗咨询回答。
- 多轮会话记忆：基于 `memoryId` 隔离不同会话，并使用 MongoDB 持久化聊天记录。
- RAG 知识库增强：将医院信息、科室信息、神经内科等知识文档向量化写入 Pinecone，回答时进行语义检索增强。
- RAG 引用来源：知识库片段带有文档来源标记，回答使用知识库内容时输出参考来源。
- 真实号源查询：基于 `doctor_schedule` 医生排班表查询科室、医生、日期、时间对应的剩余号源。
- 预约挂号：通过 LangChain4j Tool Calling 抽取姓名、身份证号、科室、日期、时间、医生等参数，查询真实排班并扣减号源。
- 取消预约：查询预约记录，取消成功后释放对应医生排班号源。
- 医疗安全边界：支持急症提醒、非医疗问题拒答、Prompt 注入防护和敏感信息识别。
- 流式输出：基于 Spring WebFlux 返回 `Flux<String>`，支持前端逐段展示大模型回答。
- Agent 自动评测：提供回答质量评测和 Tool Calling 数据库状态评测。

## 技术栈

后端：

- Java 17
- Spring Boot 3.2.6
- LangChain4j 1.0.0-beta3
- DashScope / Qwen
- Pinecone
- MongoDB
- MySQL
- MyBatis-Plus
- Spring WebFlux
- Knife4j
- JUnit 5 / Mockito

前端联调环境：

- Vue 3
- Vite
- Element Plus
- Axios

## 项目结构

```text
src/main/java/com/atguigu/java/ai/langchain4j
├── XiaozhiApp.java
├── assistant/XiaozhiAgent.java
├── config/XiaozhiAgentConfig.java
├── config/EmbeddingStoreConfig.java
├── controller/XiaozhiController.java
├── store/MongoChatMemoryStore.java
├── tools/appointmentTools.java
├── entity/Appointment.java
├── entity/DoctorSchedule.java
├── mapper/AppointmentMapper.java
├── mapper/DoctorScheduleMapper.java
├── service/AppointmentService.java
├── service/DoctorScheduleService.java
├── service/SafetyService.java
└── service/impl
    ├── AppointmentServiceImpl.java
    ├── DoctorScheduleServiceImpl.java
    └── SafetyServiceImpl.java
```

## 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8.x
- MongoDB 6.x 或 7.x
- DashScope API Key
- Pinecone API Key

## 环境变量

项目从环境变量读取大模型和向量库 Key：

```powershell
setx DASHSCOPE_API_KEY "你的 DashScope API Key"
setx PINECONE_API_KEY "你的 Pinecone API Key"
```

`setx` 设置后需要重启 IDEA 或终端才会生效。

## 数据库配置

默认 MongoDB：

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/chat_memory_db
```

默认 MySQL：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/guiguixiaozhi?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false
spring.datasource.username=root
spring.datasource.password=root
```

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS guiguixiaozhi
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

预约表：

```sql
USE guiguixiaozhi;

CREATE TABLE IF NOT EXISTS appointment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64),
  id_card VARCHAR(32),
  department VARCHAR(64),
  date VARCHAR(32),
  time VARCHAR(32),
  doctor_name VARCHAR(64)
);
```

医生排班/号源表：

```sql
CREATE TABLE IF NOT EXISTS doctor_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department VARCHAR(64) NOT NULL,
  doctor_name VARCHAR(64) NOT NULL,
  work_date VARCHAR(32) NOT NULL,
  time_slot VARCHAR(32) NOT NULL,
  total_quota INT NOT NULL,
  remaining_quota INT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_schedule (department, doctor_name, work_date, time_slot)
);
```

示例号源：

```sql
INSERT INTO doctor_schedule
  (department, doctor_name, work_date, time_slot, total_quota, remaining_quota)
VALUES
  ('神经内科', '胡波', '2026-06-16', '上午', 5, 5),
  ('神经内科', '孙圣刚', '2026-06-16', '下午', 3, 3),
  ('消化内科', '梅元武', '2026-06-16', '上午', 4, 4),
  ('皮肤科', '赵敏', '2026-06-16', '下午', 2, 2);
```

## Pinecone 知识库

当前 Pinecone 配置在 `EmbeddingStoreConfig` 中：

```text
index: yunzhen-index
namespace: yunzhen-namespace
```

知识库上传入口：

```text
src/test/java/com/atguigu/java/ai/langchain4j/EmbeddingTest.java
```

运行方法：

```text
testUploadKnowledgeLibrary()
```

该方法会读取外部知识文档并写入 Pinecone。每个入库片段会带有类似 `【文档来源：神经内科.md】` 的来源标记，用于回答末尾输出参考来源。

切换 Pinecone index 或 namespace 后，需要清空旧 namespace 或切换新 namespace，并重新上传知识库。

## 后端启动

进入后端目录：

```powershell
cd D:\Java-AI\java-ai-langchain4j
```

编译：

```powershell
mvn -DskipTests test-compile
```

启动：

```powershell
mvn spring-boot:run
```

默认服务端口：

```text
8080
```

## 前后端联调

先启动后端：

```powershell
cd D:\Java-AI\java-ai-langchain4j
mvn spring-boot:run
```

再启动前端：

```powershell
cd D:\Java-AI\xiaozhi-ui
npm install
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

联调链路：

```text
浏览器页面
-> Vue 前端 /api/xiaozhi/chat
-> Vite proxy
-> Spring Boot 后端 /xiaozhi/chat
-> LangChain4j Agent
-> SafetyService / RAG / Tool Calling / MongoDB / MySQL / Pinecone
```

## 对话接口

后端接口地址：

```text
POST /xiaozhi/chat
```

前端代理地址：

```text
POST /api/xiaozhi/chat
```

请求体格式：

```json
{
  "memoryId": 1,
  "message": "梅元武明天上午有号吗"
}
```

响应为流式文本。

## 测试与评测

预约工具单元测试：

```powershell
mvn -Dtest=AppointmentToolsTest test
```

覆盖场景：

- 有号源查询
- 无号源查询
- 预约成功扣减号源
- 重复预约失败
- 取消预约释放号源

医疗安全规则测试：

```powershell
mvn -Dtest=SafetyServiceTest test
```

Agent 回答质量评测：

```powershell
mvn -Dtest=MedicalAgentEvalTest -DrunAgentEval=true test
```

该评测读取：

```text
src/test/resources/eval/medical-agent-eval.json
```

评估维度：

- 预期关键词命中率
- 禁用词控制率
- RAG 参考来源输出率
- 急症、非医疗、Prompt 注入等安全拒答效果

Tool Calling 数据库状态评测：

```powershell
mvn -Dtest=MedicalAgentEvalTest#runAppointmentToolStateEval -DrunToolEval=true test
```

该评测会临时创建一条测试排班，真实调用 Agent 完成预约和取消，并通过 MySQL 前后状态验证：

- Tool Calling 是否产生真实业务效果
- 预约参数抽取是否正确
- 预约后 `remaining_quota` 是否扣减
- 取消预约后预约记录是否删除
- 取消后 `remaining_quota` 是否释放

普通 `mvn test` 默认不会调用大模型评测，避免每次测试都消耗 API 调用。

## 当前说明

- 本仓库只包含后端代码，前端代码在本地 `D:\Java-AI\xiaozhi-ui`。
- 项目已接入真实排班表，`queryDepartment` 不再是固定返回有号源的占位实现。
- 号源扣减使用数据库条件更新 `remaining_quota > 0`，避免并发下超卖。
- 取消预约会先查预约和排班，再删除预约并释放号源。
- RAG 回答会在使用知识库内容时输出参考来源。
- 医疗安全边界覆盖急症提醒、非医疗拒答、Prompt 注入防护和敏感字段识别。
- API Key 不应写入代码或提交到仓库。
- 本项目用于医疗导诊和预约场景原型，回答不能替代医生诊断。

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
