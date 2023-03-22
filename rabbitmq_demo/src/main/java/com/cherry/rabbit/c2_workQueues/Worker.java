package com.cherry.rabbit.c2_workQueues;

import com.cherry.rabbit.utils.RabbitUtil;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

/**
 * @author Chen Ruoyi
 * @date 2022/5/14 15:43
 */
public class Worker {
    private final static String QUEUE_NAME = "task_queue";

    public static void main(String[] argv) throws Exception {
        // 建立连接和管道
        Channel channel = RabbitUtil.getChannel();
        // 声明从哪个队列接受消息
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] 等待消息. To exit press CTRL+C");

        // 平均分配
        channel.basicQos(1);

        // 接收到信息回调接口，目的是当接收到一条信息时，进行一些操作，比如可以在控制台里打印出来，以告诉程序员收到了信息。
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] 已收到 '" + message + "'");
            try {
                doWork(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(" [x] 工作任务完成！");
                // 使用basicAck方法
                /**
                 * 消息应答的方法
                 * A.Channel.basicAck(用于肯定确认)
                 *      RabbitMQ 已知道该消息并且成功的处理消息，可以将其丢弃了
                 * B.Channel.basicNack(用于否定确认)
                 * C.Channel.basicReject(用于否定确认)
                 *      与 Channel.basicNack 相比少一个参数 不处理该消息了直接拒绝，可以将其丢弃了
                 *
                 * 第一个参数
                 * 获取发送内容的标签
                 *
                 * 第二个参数
                 * multiple 的 true 和 false 代表不同意思
                 *      true 代表批量应答【 channel 上未应答的消息】
                 *              比如说 channel 上有传送 tag 的消息 5,6,7,8 当前 tag 是 8
                 *              那么此时 5-8 的这些还未应答的消息都会被确认收到消息应答
                 *      false 同上面相比只会应答 tag=8 的消息 5,6,7 这三个消息依然不会被确认收到消息应答
                 *
                 */
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        // 取消接收的回调接口，目的是如在接收消息的时候队列被删除掉了，可以进行一些操作，例如告诉程序员接收被中断了。
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息消费被中断");
        };

        // 手动应答，应答方式见basicAck
        // 手动应答的好处是可以批量应答并且减少网络拥堵
        boolean autoAck = false;

        // 管道接收消息
        channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, cancelCallback);
    }

    // 用于模拟工作任务，输入一个点就停顿一秒
    private static void doWork(String task) throws InterruptedException {
        for (char ch : task.toCharArray()) {
            if (ch == '.') Thread.sleep(1000);
        }
    }
}

