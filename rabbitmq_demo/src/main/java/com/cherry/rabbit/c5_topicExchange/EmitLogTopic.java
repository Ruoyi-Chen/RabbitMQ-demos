package com.cherry.rabbit.c5_topicExchange;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Chen Ruoyi
 * @date 2022/5/15 14:36
 */
public class EmitLogTopic {
    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws Exception {
        // 建立连接
        Channel channel = RabbitUtil.getChannel();

        // 声明topic交换模式的交换机
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        Map<String, String> bindingKeyMap = new HashMap<>();
        bindingKeyMap.put("quick.range.rabbit", "被队列Q1Q2接收到");
        bindingKeyMap.put("lazy.orange.elephant", "被队列Q1Q2接收到");
        bindingKeyMap.put("quick.orange.fox", "被队列Q1接收到");
        bindingKeyMap.put("lazy.brown.fox", "虽然满足两个绑定但只被队列Q2接收一次");
        bindingKeyMap.put("lazy.pink.rabbit", "虽然满足两个绑定但只被队列 Q2 接收一次");
        bindingKeyMap.put("quick.brown.fox", "不匹配任何绑定不会被任何队列接收到会被丢弃");
        bindingKeyMap.put("quick.orange.male.rabbit", "是四个单词不匹配任何绑定会被丢弃");
        bindingKeyMap.put("lazy.orange.male.rabbit", "是四个单词但匹配 Q2");

        Iterator<Map.Entry<String, String>> iterator =
                bindingKeyMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, String> next = iterator.next();
            String bindingKey = next.getKey();
            String message = next.getValue();

            channel.basicPublish(EXCHANGE_NAME, bindingKey, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("生产者发出消息： " + bindingKey + "---> " + message);
        }

        channel.close();
        channel.getConnection().close();
    }
}
