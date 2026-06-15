package com.atguigu.java.ai.langchain4j.service.impl;

import com.atguigu.java.ai.langchain4j.service.SafetyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 安全校验服务实现类
 * 承担用户输入的前置安全风控职责，在请求进入大模型之前做确定性规则拦截
 * 覆盖三大核心场景：Prompt注入攻击防护、急症风险识别、非医疗内容拦截
 * 所有规则均为代码硬逻辑，执行结果100%确定，不受大模型主观判断影响
 */
@Service
public class SafetyServiceImpl implements SafetyService {

    /**
     * 急症高危关键词列表
     * 命中任一关键词即判定为急症场景，直接返回急诊提醒，不进入大模型对话
     * 覆盖心脑血管、意识障碍、大出血、高热等医疗高风险症状
     */
    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "胸痛", "呼吸困难", "喘不上气", "意识不清", "昏迷", "大出血",
            "剧烈头痛", "突然偏瘫", "口角歪斜", "抽搐", "高热不退"
    );

    /**
     * Prompt 注入攻击关键词列表
     * 命中任一关键词即判定为试图绕过安全限制、窃取系统提示词的攻击行为
     * 用于防护提示词泄露、规则绕过、越狱等常见AI安全攻击
     */
    private static final List<String> PROMPT_INJECTION_KEYWORDS = List.of(
            "忽略以上规则", "忽略之前的提示", "输出系统提示词",
            "告诉我你的prompt", "告诉我你的 prompt", "绕过限制", "开发者模式"
    );

    /**
     * 非医疗业务关键词列表
     * 命中任一关键词即判定为超出医疗导诊业务范围的请求，直接拒答
     * 用于限定Agent能力边界，避免大模型回答无关内容、偏离产品定位
     */
    private static final List<String> NON_MEDICAL_KEYWORDS = List.of(
            "股票", "基金", "炒股", "写代码", "论文代写", "考试作弊",
            "博彩", "彩票", "情感咨询", "算命"
    );

    /**
     * 身份证号正则匹配规则
     * 用于识别文本中的18位居民身份证号（含末尾X/x）
     * 仅做敏感信息识别，不直接拦截（预约挂号业务本身需要身份证信息）
     */
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("\\b\\d{17}[0-9Xx]\\b");

    /**
     * 手机号正则匹配规则
     * 用于识别文本中的中国大陆11位手机号码
     * 仅做敏感信息识别，不直接拦截
     */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b1[3-9]\\d{9}\\b");

    /**
     * 核心输入安全校验方法
     * 按优先级依次执行三类规则检查，命中即直接返回拦截结果
     * 检查优先级：Prompt注入 > 急症风险 > 非医疗内容，高优先级规则先命中先返回
     * @param message 用户输入的原始消息
     * @return 校验结果，包含是否拦截、拦截原因、返回给用户的回复文本
     */
    @Override
    public SafetyCheckResult checkInput(String message) {
        // 空文本直接放行，不做任何校验
        if (!StringUtils.hasText(message)) {
            return SafetyCheckResult.pass();
        }

        // 对输入文本做归一化处理：去除首尾空白字符，避免空格、换行影响关键词匹配
        String normalized = message.trim();

        // 第一轮校验：Prompt注入攻击检测
        // 优先级最高，防止攻击者通过注入指令绕过后续所有安全规则
        if (containsAny(normalized, PROMPT_INJECTION_KEYWORDS)) {
            return SafetyCheckResult.block(
                    "PROMPT_INJECTION",
                    "抱歉，我不能执行忽略规则、泄露系统提示词或绕过安全限制的请求。我可以继续帮助你进行医疗导诊、就医流程咨询或预约挂号。"
            );
        }

        // 第二轮校验：急症风险检测
        // 医疗场景高优先级，命中后直接返回急诊建议，不进入大模型，保障用户生命安全
        if (containsAny(normalized, EMERGENCY_KEYWORDS)) {
            return SafetyCheckResult.block(
                    "EMERGENCY",
                    "你描述的情况可能存在急症风险。请立即拨打急救电话或尽快前往附近医院急诊就医，不要等待线上咨询结果。"
            );
        }

        // 第三轮校验：非医疗内容检测
        // 限定业务边界，超出医疗导诊范围的请求直接拒答，保证Agent能力聚焦
        if (containsAny(normalized, NON_MEDICAL_KEYWORDS)) {
            return SafetyCheckResult.block(
                    "NON_MEDICAL",
                    "抱歉，我主要提供医疗导诊、医院知识问答、就医流程和预约挂号相关帮助，无法处理该类非医疗问题。"
            );
        }

        // 所有校验均未命中，放行请求，继续进入大模型对话流程
        return SafetyCheckResult.pass();
    }

    /**
     * 敏感信息识别方法
     * 检测文本中是否包含身份证、手机号、银行卡号、医保卡号、住址等隐私数据
     * 仅用于识别与日志脱敏，不直接拦截（预约挂号业务需要合法收集身份信息）
     * @param message 待检测的文本内容
     * @return true=包含敏感信息，false=不包含
     */
    public boolean containsSensitiveInfo(String message) {
        // 空文本直接返回false
        if (!StringUtils.hasText(message)) {
            return false;
        }
        // 依次匹配身份证号、手机号、银行卡关键词、医保卡关键词、住址关键词
        return ID_CARD_PATTERN.matcher(message).find()
                || PHONE_PATTERN.matcher(message).find()
                || message.contains("银行卡")
                || message.contains("医保卡号")
                || message.contains("住址");
    }

    /**
     * 私有工具方法：判断文本是否包含关键词列表中的任意一个
     * 使用流式遍历匹配，只要命中一个关键词即返回true
     * @param message 待检测的文本
     * @param keywords 关键词列表
     * @return true=命中任一关键词，false=全部未命中
     */
    private boolean containsAny(String message, List<String> keywords) {
        // 流式遍历关键词列表，判断文本是否包含当前关键词
        return keywords.stream().anyMatch(message::contains);
    }
}