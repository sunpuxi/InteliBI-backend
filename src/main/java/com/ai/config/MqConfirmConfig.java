package com.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * 调价生产者回调机制会导致多余的网络开销，一般不建议开启
 * 如果需要开启，那么关闭Publisher-return机制（一般路由失败都是自己的路由问题）
 */
@Slf4j
@Configuration
public class MqConfirmConfig implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RabbitTemplate bean = applicationContext.getBean(RabbitTemplate.class);

        //配置回调
        bean.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                log.debug("接收到的消息的returnCallback，message:{},exchange:{},code:{},text:{},routingKey:{}",
                        returnedMessage.getMessage(),returnedMessage.getExchange(),returnedMessage.getReplyCode()
                ,returnedMessage.getReplyText(),returnedMessage.getRoutingKey());
            }
        });
    }
}
