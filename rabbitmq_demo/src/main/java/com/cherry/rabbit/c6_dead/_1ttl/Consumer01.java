package com.cherry.rabbit.c6_dead._1ttl;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chen Ruoyi
 * @date 2022/5/15 16:24
 */
public class Consumer01 {
    private final static String NORMAL_EXCHANGE = "normal_exchange";
    private final static String DEAD_EXCHANGE = "dead_exchange";

    public static void main(String[] args) throws Exception{
        // 建立channel
        Channel channel = RabbitUtil.getChannel();

        // 声明死信和普通交换机，类型为direct
        channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);

        // 声明死信队列
        String deadQueue = "dead-queue";
        channel.queueDeclare(deadQueue, false, false, false, null);
        // 死信队列绑定死信交换机与routingKey
        channel.queueBind(deadQueue, DEAD_EXCHANGE, "lisi");

        // 正常队列绑定死信队列信息
        Map<String, Object> params = new HashMap<>();
        // 正常队列设置死信交换机，key是固定值
        params.put("x-dead-letter-exchange", DEAD_EXCHANGE);
        // 正常队列设置死信routing-key，key是固定值
        params.put("x-dead-letter-routing-key", "lisi");

        String normalQueue = "normal-queue";
        // 将设置死信的参数params放进正常队列声明中
        channel.queueDeclare(normalQueue,false,false, false, params);
        channel.queueBind(normalQueue, NORMAL_EXCHANGE, "zhangsan");

        System.out.println("等待接收信息。。。");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Consumer01 接收到信息： " + message);
        };
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("接收失败");
        };

        channel.basicConsume(normalQueue, true, deliverCallback, cancelCallback);
    }
}
