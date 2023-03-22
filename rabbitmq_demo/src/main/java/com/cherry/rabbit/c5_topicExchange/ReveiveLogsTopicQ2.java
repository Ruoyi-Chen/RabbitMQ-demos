package com.cherry.rabbit.c5_topicExchange;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class ReveiveLogsTopicQ2 {
    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws Exception {
        // 建立channel
        Channel channel = RabbitUtil.getChannel();

        // 声明交换
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        // 声明Q1队列与绑定关系
        String queueName = "Q2";
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "*.*.rabbit");
        channel.queueBind(queueName, EXCHANGE_NAME, "lazy.#");

        System.out.println("等待接收消息。。匹配模式为：\"*.*.rabbit\"或\"lazy.#\"");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("接收队列：" + queueName +
                    " --> 路由键：" + delivery.getEnvelope().getRoutingKey() +
                    " -- 消息：" + message);
        };
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("接收失败。。");
        };
        channel.basicConsume(queueName, true, deliverCallback, cancelCallback);
    }
}
