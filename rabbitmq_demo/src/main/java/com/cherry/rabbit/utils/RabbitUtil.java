package com.cherry.rabbit.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Chen Ruoyi
 * @date 2022/5/13 19:04
 */
public class RabbitUtil {

    @Test
    public void test() throws Exception {
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(new File("src/main/resources/rabbit-user.properties"));
        properties.load(inputStream);
        System.out.println(properties.getProperty("rabbit.host"));
    }

    public static Channel getChannel() throws Exception {
        // 引入配置文件
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(new File("src/main/resources/rabbit-user.properties"));
        properties.load(inputStream);
        String host = properties.getProperty("rabbit.host");
        String username = properties.getProperty("rabbit.username");
        String password = properties.getProperty("rabbit.password");

        // 连接工厂
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        // 建立连接和信道
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();
        return channel;
    }
}
