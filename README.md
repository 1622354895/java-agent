# 小智医疗 Agent

小智医疗 Agent 是一个基于 Spring Boot 和 LangChain4j 的医疗导诊智能体后端项目，面向模拟医院客服、科室导诊、医疗知识问答和预约挂号场景。项目核心是后端 Agent 能力，前端页面不是本项目重点。

## 核心功能

- 医疗导诊问答：根据用户描述提供就医流程、科室推荐和常见医疗咨询回答。
- 多轮对话记忆：基于 `memoryId` 区分不同会话，并使用 MongoDB 持久化聊天记录。
- RAG 知识库增强：将医院信息、科室信息、神经内科等知识文档向量化后写入 Pinecone，回答时进行语义检索增强。
- 预约挂号工具调用：通过 LangChain4j Tool Calling 抽取预约参数，并调用后端业务逻辑完成预约、取消预约和号源查询。
- 流式输出：基于 Spring WebFlux 返回 `Flux<String>`，实现大模型回答的流式响应。

## 技术栈

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

## 项目结构

```text
src/main/java/com/atguigu/java/ai/langchain4j
├── XiaozhiApp.java                         # Spring Boot 启动类
├── assistant/XiaozhiAgent.java             # 小智医疗 Agent 编排入口
├── config/XiaozhiAgentConfig.java          # 会话记忆和 RAG 检索器配置
├── config/EmbeddingStoreConfig.java        # Pinecone 向量库配置
├── controller/XiaozhiController.java       # 对话接口
├── store/MongoChatMemoryStore.java         # MongoDB 聊天记忆持久化
├── tools/appointmentTools.java             # 预约挂号工具
├── entity/Appointment.java                 # 预约实体
├── mapper/AppointmentMapper.java           # MyBatis-Plus Mapper
└── service/AppointmentService.java         # 预约业务服务
```

## 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8.x
- MongoDB 6.x 或 7.x
- Pinecone 账号和 API Key
- DashScope API Key

## 环境变量

项目从环境变量读取大模型和向量库 Key：

```powershell
setx DASHSCOPE_API_KEY "你的 DashScope API Key"
setx PINECONE_API_KEY "你的 Pinecone API Key"
```

`setx` 设置后需要重启 IDEA 或终端才能生效。

## 数据库配置

默认 MongoDB：

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/chat_memory_db
```

默认 MySQL：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/guiguixiaozhi
spring.datasource.username=root
spring.datasource.password=root
```

创建 MySQL 数据库：

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

## Pinecone 知识库

当前 Pinecone 配置在 `EmbeddingStoreConfig` 中：

```text
index: xiaozhi-index
namespace: xiaozhi-namespace
```

知识库上传入口在测试类：

```text
src/test/java/com/atguigu/java/ai/langchain4j/EmbeddingTest.java
```

运行方法：

```text
testUploadKnowledgeLibrary()
```

该方法会读取以下文档并写入 Pinecone：

```text
D:/Java-AI/医院信息.md
D:/Java-AI/科室信息.md
D:/Java-AI/神经内科.md
```

如果 Pinecone 中已经有这批数据，不需要重复上传。

## 启动项目

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

## 对话接口

接口地址：

```text
POST /xiaozhi/chat
```

请求示例：

```powershell
curl.exe -N -X POST http://localhost:8080/xiaozhi/chat `
  -H "Content-Type: application/json" `
  -d "{\"memoryId\":1,\"message\":\"我最近头疼、恶心，应该挂什么科？\"}"
```

请求体格式：

```json
{
  "memoryId": 1,
  "message": "我想预约明天上午神经内科"
}
```

响应为流式文本。

## 当前限制

- `queryDepartment` 目前是占位实现，默认返回有号源，还没有接入真实医生排班和库存扣减。
- 项目是医疗导诊和预约场景原型，回答不能替代医生诊断。
- 运行前需要确保 MongoDB、MySQL、DashScope Key、Pinecone Key 都可用。
- 不要把真实 API Key 写入代码或提交到仓库。
