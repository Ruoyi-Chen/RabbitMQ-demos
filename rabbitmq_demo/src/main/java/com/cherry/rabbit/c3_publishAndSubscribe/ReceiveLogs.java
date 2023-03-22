package com.cherry.rabbit.c3_publishAndSubscribe;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

/**
 * @author Chen Ruoyi
 * @date 2022/5/14 20:46
 */
public class ReceiveLogs {
    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception {
        // 获得一个channel
        Channel channel = RabbitUtil.getChannel();
        // 声明交换模式
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        // 获得队列名称
        String queueName = channel.queueDeclare().getQueue();
        // 绑定队列和交换机
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println(" [*] 等待信息. To exit press CTRL+C");

        // 收到消息的回调接口
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] 收到信息 '" + message + "'");
        };
        // 取消发送的回调接口
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息消费被中断");
        };
        // 接收信息
        channel.basicConsume(queueName,true,deliverCallback, cancelCallback);
    }
}
