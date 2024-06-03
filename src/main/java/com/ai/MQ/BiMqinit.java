package com.yupi.springbootinit.MQ;

import com.rabbitmq.client.Channel;

/**
 * 需要本地先运行该方法，才能创建队列和交换机
 * 不推荐，推荐使用注解开发，一步到位，从创建队列到绑定routingKey
 */
public class BiMqinit {
    public static void main(String[] args) {
        try {
            Channel channel = RabbitMQUtils.createChannel();

            /**
             *使用ExchangeDeclare方法创建交换机，第一个参数是交换机名字，第二个参数是交换机类型，
             * 第三个参数是是否持久化，第四个参数是是否自动删除，第五个参数是是否内部交换机，第六个参数是创建参数。
             */
            channel.exchangeDeclare(BI_CONSTANT.BI_EXCHANGE, "direct",false);

            // 创建队列，随机分配一个队列名称
            channel.queueDeclare(BI_CONSTANT.BI_QUEUE, true, false, false, null);
            channel.queueBind(BI_CONSTANT.BI_QUEUE, BI_CONSTANT.BI_EXCHANGE, BI_CONSTANT.BI_ROUTINGKEY);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}
