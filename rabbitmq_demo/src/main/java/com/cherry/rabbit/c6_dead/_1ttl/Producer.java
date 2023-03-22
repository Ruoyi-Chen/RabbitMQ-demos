package com.cherry.rabbit.c6_dead._1ttl;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;

/**
 * @author Chen Ruoyi
 * @date 2022/5/15 16:18
 */
public class Producer {
    private static final String NORMAL_EXCHANGE = "normal_exchange";

    public static void main(String[] args) throws Exception{
        // 获取连接
        Channel channel = RabbitUtil.getChannel();
        // 建立一个direct模式的交换
        channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
        // 设置消息的TTL时间
        AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().expiration("10000").build();

        // 该消息是用作演示队列的个数限制
        for (int i = 0; i < 11; i++) {
            String message = "info" + i;
            channel.basicPublish(NORMAL_EXCHANGE, "zhangsan",properties,message.getBytes(StandardCharsets.UTF_8));
            System.out.println("生产者发送信息" + message);
        }

        channel.close();
        channel.getConnection().close();
    }
}
