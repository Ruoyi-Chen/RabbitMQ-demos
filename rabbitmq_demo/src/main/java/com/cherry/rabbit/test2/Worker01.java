package com.cherry.rabbit.test2;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

/**
 * @author Chen Ruoyi
 * @date 2022/5/13 20:04
 */
public class Worker01 {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] args) throws Exception{
        Channel channel = RabbitUtil.getChannel();

        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("接受到的消息： " + new String(message.getBody()));
        };

        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("接收取消");
        };

        channel.basicConsume(QUEUE_NAME,true, deliverCallback, cancelCallback);
    }
}
