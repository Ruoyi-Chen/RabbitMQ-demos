package com.cherry.rabbit.c6_dead._2maxLength;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

/**
 * @author Chen Ruoyi
 * @date 2022/5/15 16:35
 * Consumer02: 消费死信队列里的信息
 */
public class Consumer02 {
    private static final String DEAD_EXCHANGE = "dead_exchange";

    public static void main(String[] args) throws Exception {
        // 建立channel
        Channel channel = RabbitUtil.getChannel();
        // 声明交换机
        channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);

        // 死信队列声明及绑定交换机
        String deadQueue = "dead-queue";
        channel.queueDeclare(deadQueue, false, false, false, null);
        channel.queueBind(deadQueue, DEAD_EXCHANGE, "lisi");

        System.out.println("等待接收死信队列信息。。。。");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Consumer02 接收到死信队列中的信息： " + message);
        };
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("接收失败");
        };

        channel.basicConsume(deadQueue, true, deliverCallback, cancelCallback);
    }
}
