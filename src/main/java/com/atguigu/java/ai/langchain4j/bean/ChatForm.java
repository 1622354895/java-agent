// 定义包路径，属于 langchain4j 的数据传输对象模块
package com.atguigu.java.ai.langchain4j.bean;

// 导入 Lombok 的 Data 注解，自动生成 getter、setter、toString、equals、hashCode 方法
import lombok.Data;

/**
 * 聊天请求表单类
 * 用于接收前端发送的聊天请求参数，封装用户消息和会话标识
 */
@Data
public class ChatForm {
    /**
     * 对话会话标识ID
     * 用于区分不同用户或不同对话窗口的独立会话历史
     * 对应 XiaozhiAgent 中的 @MemoryId 参数
     */
    private Long memoryId;//对话id
    
    /**
     * 用户发送的消息内容
     * 包含用户向 AI 提出的具体问题或指令
     * 对应 XiaozhiAgent 中的 @UserMessage 参数
     */
    private String message;//用户问题
}
