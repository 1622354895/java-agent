# 云诊助手 Agent

云诊助手 Agent 是一个基于 Spring Boot 和 LangChain4j 的医疗导诊智能体后端项目，面向模拟互联网医院的咨询导诊、知识库问答、号源查询、预约挂号和取消预约场景。项目重点是后端 Agent 编排、RAG 检索增强、Tool Calling 业务执行和多轮会话记忆。

## 核心功能

- 医疗导诊问答：根据用户症状和就医需求，提供科室推荐、就医流程和常见医疗咨询回答。
- 多轮对话记忆：基于 `memoryId` 隔离不同会话，并使用 MongoDB 持久化聊天记录。
- RAG 知识库增强：将医院信息、科室信息、神经内科等知识文档向量化后写入 Pinecone，回答时进行语义检索增强。
- 真实号源查询：基于 `doctor_schedule` 医生排班表查询科室、医生、日期、时间对应的剩余号源。
- 预约挂号：通过 LangChain4j Tool Calling 抽取姓名、身份证号、科室、日期、时间、医生等参数，查询真实排班并扣减号源。
- 取消预约：查询预约记录，取消成功后释放对应医生排班号源。
- 流式输出：基于 Spring WebFlux 返回 `Flux<String>`，支持大模型回答的流式响应。

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
- JUnit 5 / Mockito

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
└── service/impl
    ├── AppointmentServiceImpl.java
    └── DoctorScheduleServiceImpl.java
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

知识库上传入口在测试类：

```text
src/test/java/com/atguigu/java/ai/langchain4j/EmbeddingTest.java
```

运行方法：

```text
testUploadKnowledgeLibrary()
```

该方法会读取外部知识文档并写入 Pinecone。切换 index 或 namespace 后，需要重新上传知识库。

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

请求体格式：

```json
{
  "memoryId": 1,
  "message": "梅元武明天上午有号吗"
}
```

响应为流式文本。

## 测试

预约工具测试：

```powershell
mvn -Dtest=AppointmentToolsTest test
```

当前覆盖场景：

- 有号源查询
- 无号源查询
- 预约成功扣减号源
- 重复预约失败
- 取消预约释放号源

## 当前说明

- 项目已经接入真实排班表，`queryDepartment` 不再是固定返回有号源的占位实现。
- 号源扣减使用数据库条件更新 `remaining_quota > 0`，避免并发下超卖。
- 取消预约会先查预约和排班，再删除预约并释放号源。
- API Key 不应写入代码或提交到仓库。
- 本项目用于医疗导诊和预约场景原型，回答不能替代医生诊断。
