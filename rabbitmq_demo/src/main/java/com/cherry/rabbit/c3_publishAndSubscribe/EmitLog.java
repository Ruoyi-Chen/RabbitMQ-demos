package com.cherry.rabbit.c3_publishAndSubscribe;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author Chen Ruoyi
 * @date 2022/5/14 20:40
 */
public class EmitLog {
    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception{
        Channel channel = RabbitUtil.getChannel();
        // 声明交换名称和方式
        channel.exchangeDeclare(EXCHANGE_NAME,"fanout");

        String message = new Scanner(System.in).nextLine();

        channel.basicPublish(EXCHANGE_NAME, "",null,message.getBytes(StandardCharsets.UTF_8));

        System.out.println(" [x] 发送信息 '" + message + "'");
        channel.close();
        channel.getConnection().close();
    }
}
