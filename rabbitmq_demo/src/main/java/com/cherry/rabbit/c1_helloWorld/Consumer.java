package com.cherry.rabbit.c1_helloWorld;

import com.rabbitmq.client.*;

/**
 * @author Chen Ruoyi
 * @date 2022/5/13 19:01
 */
public class Consumer {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception {
//        ConnectionFactory factory = RabbitUtils.getRabbit();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("120.24.235.36");
        factory.setUsername("cherry");
        factory.setPassword("brfsyfsdhl+rabbit");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        System.out.println("等待接收消息。。。");
        // 推送的消息如何进行消费的接口回调
        DeliverCallback deliverCallback =
                (consumerTag, delivery) -> {
                    String msg = new String(delivery.getBody());
                    System.out.println(msg);
                };
        // 取消消费的一个回调接口，如在消费的时候队列被删除掉了
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息消费被中断");
        };

        /**
         * 消费者消费信息
         * 1。 消费哪个队列
         * 2。 消费成功后是否要自动应答，true自动应答，false手动应答
         * 3。 消费者未成功消费的回调
         */
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback);
        channel.close();
        connection.close();
    }

}
