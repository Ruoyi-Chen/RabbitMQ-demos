package com.cherry.rabbit.c1_helloWorld;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * @author Chen Ruoyi
 * @date 2022/5/13 19:00
 */
public class Producer {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("120.24.235.36");
        factory.setUsername("cherry");
        factory.setPassword("brfsyfsdhl+rabbit");
        // channel实现了close接口自动关闭，不需要显式关闭
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        /**
         * 生成一个队列，
         * 1。队列名称
         * 2。队列里的消息是否持久化，默认消息存储在内存中
         * 3。该队列是否只供一个消费者进行消费，是否进行共享，true 可以多个消费者消费
         * 4。是否自动删除，最后一个消费者断开连接后，该队列是否自动删除，true则自动删除
         * 5。其他参数
         */
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        String message = "hello, world";

        /**
         * 发送一个消息
         * 1。发送到哪个交换机
         * 2。路由的key是哪个
         * 3。其他的参数信息
         * 4。发送消息的消息体
         */
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
        System.out.println("消息发送完毕");

        channel.close();
        connection.close();
    }
}
