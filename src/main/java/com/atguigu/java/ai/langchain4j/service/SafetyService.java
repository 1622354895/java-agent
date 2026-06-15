package com.atguigu.java.ai.langchain4j.service;

public interface SafetyService {

    /**
     * 检查用户输入消息的安全性
     * 用于过滤敏感、违规或不适当的内容
     *
     * @param message 用户输入的待检查消息
     * @return 返回安全检查结果，包含是否被拦截、拦截原因和建议回复
     */
    SafetyCheckResult checkInput(String message);

    /**
     * 定义安全检查结果的记录类
     * 封装了安全检查的完整信息
     *
     * @param blocked 表示该消息是否被拦截阻止
     * @param reason 如果被拦截，说明拦截的具体原因
     * @param reply 如果被拦截，返回给用户的友好提示消息
     */
    record SafetyCheckResult(
            boolean blocked,
            String reason,
            String reply
    ) {
        /**
         * 创建一个通过安全检查的结果对象
         * 表示消息内容安全，无需拦截
         *
         * @return 返回一个blocked为false的安全检查结果
         */
        public static SafetyCheckResult pass() {
            return new SafetyCheckResult(false, null, null);
        }

        /**
         * 创建一个被拦截的安全检查结果对象
         * 表示消息存在安全问题，需要阻止
         *
         * @param reason 拦截的原因说明
         * @param reply 返回给用户的提示信息
         * @return 返回一个blocked为true的安全检查结果
         */
        public static SafetyCheckResult block(String reason, String reply) {
            return new SafetyCheckResult(true, reason, reply);
        }
    }
}
