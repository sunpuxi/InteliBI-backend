package com.yupi.springbootinit.MQ;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 生产者方发送消息
 */
@Component
public class BIMessageProducer {
    //注入对象，可直接读取配置文件中的配置信息
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message){
        rabbitTemplate.convertAndSend(BI_CONSTANT.BI_EXCHANGE,BI_CONSTANT.BI_ROUTINGKEY,message);
    }
}
