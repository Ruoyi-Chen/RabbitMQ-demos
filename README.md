# RabbitMQ-demos

@[toc]

> 参考文献：
> 官方文档（英文）链接：https://www.rabbitmq.com/tutorials/tutorial-one-java.html
> RabbitMQ简介 英文教程（半小时）：https://www.youtube.com/results?search_query=rabbitmq
> 尚硅谷文档（中文）：链接：https://pan.baidu.com/s/1xhh5b02mC9FeOlgKkGCyvg 提取码：t8oh 

`看到没解释的参数不要着急，先不用管他，后面慢慢都会解释的，因为需要一些铺垫～`

# 零、 RabbitMQ安装

## 1. 在官网下载`rabbitmq-server`

官网链接：https://www.rabbitmq.com/download.html

## 2. 在GitHub上下载`erlang`

GitHub链接：https://github.com/rabbitmq/erlang-rpm

## 3. 将文件上传至你的服务器or虚拟机的`/usr/local/software`目录下

如果没有/software，自己建立一个：

```shell
mkdir /usr/local/software
```

## 4. 安装文件

```shell
rpm -ivh erlang-21.3-1.el7.x86_64.rpm  # 改成你自己的版本号
yum install socat -y
rpm -ivh rabbitmq-server-3.8.8-1.el7.noarch.rpm # 改成你自己的版本号
```

## 5. 常用命令

```shell
chkconfig rabbitmq-server on		# 添加开机启动RabbitMQ服务【配置时只需要这两条】
/sbin/service rabbitmq-server start # 启动服务【配置时只需要这两条】
/sbin/service rabbitmq-server status# 查看服务状态（绿色Active running就是已启动）
/sbin/service rabbitmq-server stop	# 停止服务

rabbitmqctl stop_app				# 关闭应用
rabbitmqctl reset					# 全部清除
rabbitmqctl start_app				# 重新启动
```

## 6. 开启web管理插件

```shell
rabbitmq-plugins enable rabbitmq_management
```

`如果是云服务器，请在阿里云（或其他云）官网+宝塔（如有）放行你的5672和15672端口！不要轻易关闭防火墙！`

默认账号为：guest，默认密码为：guest
但是只有本机才能用这个账号密码登陆，在其他机器访问服务器会出现`User can only log in via localhost`错误。
因此还需要配置一个新用户：

```shell
rabbitmqctl add_user 用户名 密码					# 添加新用户,密码请用强密码！
rabbitmqctl set_user_tags 用户名 administrator	# 设置用户角色
rabbitmqctl set_permissions -p "/" 用户名 ".*" ".*" ".*"	# 设置用户权限，这里表示该用户具有对/vhost1下所有资源的配置+读写权限。set_permissions [-p <vhostpath>] <user> <conf> <write> <read>
rabbitmqctl list_users							# 查看当前用户及其角色
```

------------

# 一、快速开始 `Hello World`

> [官方文档链接](https://www.rabbitmq.com/tutorials/tutorial-one-java.html)：https://www.rabbitmq.com/tutorials/tutorial-one-java.html

## 1.1 RabbitMQ 简介

RabbitMQ(`MQ:Message Queue`) 

- 是一个消息代理（`Message Broker`），用于接收和转发消息。【接收 -> 存储 -> 转发二进制数据】
- `生产Producing`：发送消息，发送消息的程序称为“生产者”。
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/c45305618ca448c59f8bc4142f2c0f67.png)
- `消费Consuming`：接受消息，接受消息的程序称为“消费者”。
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/dfa133b36c264afd880296207f474c33.png)
- `队列Queue`：消息队列是RabbitMQ里的“邮箱”，本质上是一个消息缓冲区`buffer`。生产者发送消息给队列，消息存储在队列中，转发到消费者处。队列只受到主机内存和磁盘大小的限制。
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/e67b05923c3c4a4ab5f7646f33b5d0c5.png)

## 1.2 示例程序：Hello World

实现：生产者发送一条程序，消费者接收消息并打印出来。
![在这里插入图片描述](https://img-blog.csdnimg.cn/201b3d4d3db340c493c1ffbb94bf6ced.png)

### 1. 建立Java Maven工程，在`pom.xml`中添加以下配置

（By the way，我用的是JDK8）

- Note：如果amqp-client找不到，检查一下你的网络设置（是不是用了公司内网），可以用手机热点试试。

```xml
    <dependencies>
<!--        rabbitmq 依赖客户端-->
        <dependency>
            <groupId>com.rabbitmq</groupId>
            <artifactId>amqp-client</artifactId>
            <version>5.8.0</version>
        </dependency>
<!--        操作文件流的一个依赖-->
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.6</version>
        </dependency>
<!--        slf4j日志-->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.36</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.10</version>
        </dependency>
<!--        junit测试-->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
        </dependency>
    </dependencies>
```


### 2. 发送消息

![在这里插入图片描述](https://img-blog.csdnimg.cn/9c957b850cb3415d8190365f3ba8e231.png)

```java
public class Send {
  private final static String QUEUE_NAME = "hello";
  public static void main(String[] argv) throws Exception {
  	// 工厂模式，建立连接
    ConnectionFactory factory = new ConnectionFactory();
    // 如果你是在主机上测试，只需要这一条
	// factory.setHost("localhost");
	// 如果是在本地访问服务器来测试，需要配置账号密码。
	// 也可以写一个properties文件来读取信息，后面还会集成进Utils里，这里先直接写死测试一下能不能通
    factory.setHost("xxx.xxx.xxx.xxx（这是服务器ip地址）");
    factory.setUsername("在安装时设置的管理员用户名");
    factory.setPassword("在安装时设置的管理员密码");
    
    // 建立连接和管道
    // 使用try()语句，connection和channel都实现了java.io.Closeable，所以不用显式地.close()关掉连接
	try (Connection connection = factory.newConnection();
	     Channel channel = connection.createChannel()) {
	     	// 参数一：声明我们要发送的队列是谁（QUEUE_NAME），其他参数这里先不用关注
		    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		    // 发送消息
			String message = "Hello World!";
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
			System.out.println(" [x] 发送消息 '" + message + "'");
	}
  }
}
```

> `channel.queueDeclare(param 1, param2, param3, param4, param5)`
> 生成一个队列
> 1.队列名称
> 2.队列里面的消息是否持久化 默认消息存储在内存中
> 3.该队列是否只供一个消费者进行消费 是否进行共享 true 可以多个消费者消费
> 4.是否自动删除 最后一个消费者端开连接以后 该队列是否自动删除 true 自动删除 
> 5.其他参数
>
> `channel.basicPublish("", QUEUE_NAME, null, message.getBytes());` 
> 发送一个消息
> 1.发送到哪个交换机 
> 2.路由的 key 是哪个 
> 3.其他的参数信息
> 4.发送消息的消息体

### 3. 接收消息

![在这里插入图片描述](https://img-blog.csdnimg.cn/a4e672434c7042079b0d320872d608aa.png)
生产者发送完就结束进程了，消费者则会一直监听消息。

```java
public class Recv {
  private final static String QUEUE_NAME = "hello";
  
  public static void main(String[] argv) throws Exception {
  //======连接配置与Send一样，后面会写进Utils里========//
  	// 工厂模式，建立连接
    ConnectionFactory factory = new ConnectionFactory();
    // 如果你是在主机上测试，只需要这一条
	// factory.setHost("localhost");
	// 如果是在本地访问服务器来测试，需要配置账号密码。
	// 也可以写一个properties文件来读取信息，后面还会集成进Utils里，这里先直接写死测试一下能不能通
    factory.setHost("xxx.xxx.xxx.xxx（这是服务器ip地址）");
    factory.setUsername("在安装时设置的管理员用户名");
    factory.setPassword("在安装时设置的管理员密码");
    
    // 建立连接和管道
    // 这里不用try()包裹起建立的语句，原因是：
    // 我们的目的是不断监听消息，如果用try直接收到一条就close了，则不能达到监听的效果
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    // 声明从哪个队列接受消息
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    System.out.println(" [*] 等待消息. To exit press CTRL+C");
    
    // 接收到信息回调接口，目的是当接收到一条信息时，进行一些操作，比如可以在控制台里打印出来，以告诉程序员收到了信息。
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
	    String message = new String(delivery.getBody(), "UTF-8");
	    System.out.println(" [x] 已收到 '" + message + "'");
    };
    // 取消接收的回调接口，目的是如在接收消息的时候队列被删除掉了，可以进行一些操作，例如告诉程序员接收被中断了。
    CancelCallback cancelCallback=(consumerTag) -> {
    	System.out.println("消息消费被中断"); 
    };
    
    // 管道接收消息
	channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback);
	}
}
```

### 4. 运行代码

1. 先在终端把rabbitmq打开
   ![在这里插入图片描述](https://img-blog.csdnimg.cn/bd3c517cf46942279263c9cc00b41e26.png)
2. 在idea里，先跑Revc再跑Send
   也可以通过命令行操作，在terminal里：

```shell
javac -cp amqp-client-5.8.0.jar Send.java Recv.java # 编译
java -cp .:amqp-client-5.8.0.jar:slf4j-api-1.7.36.jar Recv # 运行Recv
java -cp .:amqp-client-5.8.0.jar:slf4j-api-1.7.36.jar Send # 运行Send
```

**结果：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/e58a1743d8d945f2820ac2596392b9b4.png)

------------

# 二、 工作队列/任务队列 `Work Queues / Task Queues`

> [官方文档链接](https://www.rabbitmq.com/tutorials/tutorial-two-java.html)：https://www.rabbitmq.com/tutorials/tutorial-two-java.html

## 2.1 简介

![在这里插入图片描述](https://img-blog.csdnimg.cn/6e7fea153168461595640ea0d192d273.png)

**工作队列的核心思想：**

- 对于生产者（产生消息的人）：避免必须立刻执行“资源紧张”的任务。
- 对于消息队列：生产者想要做的“任务”会被封装成一个消息放在队列里。
- 对于消费者（处理任务的人）：当你有多个“工人”时，这些任务会被轮询分配给不同的工人。

这个思想也被应用于那些需要处理`不能在一个很短的HTTP请求窗口期间完成的复杂任务`的网页程序中。

## 2.2 示例程序

### 0. 把建立connection和channel的过程写在一个工具类里

建立一个Utils包，撰写一个RabbitUtil工具类。

```java
public class RabbitUtil {
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
```

在resources下，建立一个properties文件写入rabbitmq的配置信息。

```shell
rabbit.host=写入你的主机ip
rabbit.username=写入你的rabbitmq管理员名称
rabbit.password=写入你的rabbitmq管理员密码
```

### 1. 发送

我们用`Thread.sleep()`来模拟在现实中需要很长时间的复杂任务，打多少个`“.”`代表这个任务有多复杂。

基于上一节的`Send.java`，我们做一些修改：

```java
public class NewTask {
  private final static String QUEUE_NAME = "hello";
  public static void main(String[] argv) throws Exception {
  	// 建立连接和管道
    try (Channel channel = RabbitUtil.getChannel()){
   		// 参数一：声明我们要发送的队列是谁（QUEUE_NAME），其他参数这里先不用关注
	    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
	    // 发送消息
	    // 如果你是用shell测试的，用这条语句，用于放入参数：
		// String message = String.join(" ", argv); // ***主要改了这里***
		// 如果你是在idea里用控制台测试的，用这条语句：
		String message = new Scanner(System.in).nextLine();
		channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
		System.out.println(" [x] 发送消息 '" + message + "'");
    }
  }
}
```

### 2.接收

接收程序需要做的修改：

1. 模拟发送不同数量的`"."`来代表任务复杂度。
2. 接收到消息后开始做任务

在接收程序上，我们做一些修改：

```java
public class Worker {
  private final static String QUEUE_NAME = "hello";
  
  public static void main(String[] argv) throws Exception {
	  	// 建立连接和管道
	  	Channel channel = RabbitUtil.getChannel();
	    // 声明从哪个队列接受消息
	    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
	    System.out.println(" [*] 等待消息. To exit press CTRL+C");
	    
	    // 接收到信息回调接口，目的是当接收到一条信息时，进行一些操作，比如可以在控制台里打印出来，以告诉程序员收到了信息。
	    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
		    String message = new String(delivery.getBody(), "UTF-8");
		    System.out.println(" [x] 已收到 '" + message + "'");
		    // ***主要改了这里***
		    try{
		    	doWork(message);
		    } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
		    	System.out.println(" [x] 工作任务完成！");
		    }
	    };
	    // 取消接收的回调接口，目的是如在接收消息的时候队列被删除掉了，可以进行一些操作，例如告诉程序员接收被中断了。
	    CancelCallback cancelCallback=(consumerTag) -> {
	    	System.out.println("消息消费被中断"); 
	    };
	    // 这个参数后面会说, 详见2.3
	    boolean autoAck = true;
	    
	    // 管道接收消息
		channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, cancelCallback);
	}
	
	// 用于模拟工作任务，输入一个点就停顿一秒
	private static void doWork(String task) throws InterruptedException {
	    for (char ch: task.toCharArray()) {
	        if (ch == '.') Thread.sleep(1000);
	    }
	}
}
```

### 运行代码

在shell里运行的方法同上一章。


先配置让idea可以运行多个实例。（我的idea版本是2021.3，没有Allow Parallel run选项了，老版本idea请[参考文章](https://blog.csdn.net/qq_39387856/article/details/87170301?ops_request_misc=&request_id=&biz_id=102&utm_term=idea%20并行运行&utm_medium=distribute.pc_search_result.none-task-blog-2~all~sobaiduweb~default-1-87170301.142^v9^control,157^v4^control&spm=1018.2226.3001.4187)）
![在这里插入图片描述](https://img-blog.csdnimg.cn/9c5e3bec8bf245109fdce5241fdd6e51.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/bfd2258b90ce4094883af93692b470cc.png)

#### 测试结果

发送消息
![在这里插入图片描述](https://img-blog.csdnimg.cn/912a532140e047d58de1bf2366400129.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/bc55d4bedb23426b847254bb186bc252.png)

处理消息

![在这里插入图片描述](https://img-blog.csdnimg.cn/81ecf4969a8d4c89831d06ab5fac51ba.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/05d62d1b4935462680c20dcb8f037f89.png)


## 2.3 消息应答

### 1. 轮流调度 `Round-robin dispatching`

用工作队列的一个好处是可以轻松地进行并行工作。如果我们积攒了很多任务没做，这时只需要多加几个工人，可以很轻松地扩大处理规模`scale`。

默认情况下，RabbitMQ会把消息按顺序传给下一个消费者。平均来看，每个消费者拿到的信息数量都是相同的。这种分发信息的机制被称为轮流调度（轮询，`round-robin`）。

### 2. 消息确认 `Message Acknowledgements`

在我们现在写的这份代码里，RabbitMQ一把信息转发给消费者（工人）就会马上把这个任务在队列里删掉。
而完成一个任务需要一定的时间，那如果一个工人在做某项任务期间突然被打断了，我们就会丢失这个任务信息。不仅如此，我们还会丢掉所有交给这个工人但他还没完成的任务。

如果你不想让信息丢失，我们就要开启RabbitMQ的信息确认功能。消费者在接收到并处理完一个任务后，会给RabbitMQ发一个确认信息（`Acknowledgement, ACK`），告诉他任务已经完成了，可以删掉了。如果消费者没完成任务就死掉了（例如管道关闭了、连接丢失了、TCP连接断掉了），一段时间后RabbitMQ没收到确认信息ACK，就会知道给他的消息没有被处理，从而把这个消息再放进队列里，并让其他消费者去处理。
![在这里插入图片描述](https://img-blog.csdnimg.cn/166784e7acdb445bae9f7badb5c700cd.png)


默认情况下，RabbitMQ会等30分钟。你也可以用`rabbitmq.conf`中的参数`consumer_timeout`自定义超时时间。（[点此查看官方文档解释](https://www.rabbitmq.com/consumers.html#acknowledgement-timeout)）

默认情况下，自动应答功能是打开的。我们刚才的代码里`boolean autoAck = true;`把这个功能关掉了。

接下来，我们来测试自动应答功能，autoAck改为：`boolean autoAck = false;`。
结束掉一个在工作途中的worker进程，看一下最终的效果，消息会被重新分配给其他worker。

![在这里插入图片描述](https://img-blog.csdnimg.cn/55205d42ec0a4e8e96642ffddcb672e1.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/38b739cef5854dedaf8dc964c9512ca9.png)

注意：这里的信息确认ACK必须从收到信息的channel发回去。否则会出现`channel-level protocol exception`。

### 3. 消息持久化

如果RabbitMQ服务器挂掉了，消息也是会丢失的，除非你将`队列和消息`进行持久化（写入磁盘）。

第一步，修改`队列`声明的参数：

```java
	boolean durable = true;
	channel.queueDeclare("task_queue", durable, false, false, null);
```

注意，如果已经声明并使用了一个队列，那么不可以修改他的参数，只能重新换一个队列名称（生产者和消费者代码中的队列名称都要改）。

第二步，需要标记我们的`消息`是持久化的：

```java
import com.rabbitmq.client.MessageProperties;

channel.basicPublish("", "task_queue",
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes());
```

注意，虽然我们标记了消息是需要持久化的，但RabbitMQ接收到消息->持久化到磁盘仍然需要一定时间，这就意味着消息可能在缓存里，依然有丢失的可能。不过对于简单的任务队列这也够用了，如果还需要更强的保证消息不丢失，则需要使用“发布者确认”`publisher confirms`。【见2.4】

### 4. 公平分配

假设我们有两个工人，按顺序分配任务，如果奇数的任务很重偶数的任务很轻松，就会出现有一个工人累的要死，另一个却很闲的情况。任务量分配不均的原因是：RabbitMQ没有看每个工人完成的工作量（即，收到的ACK数）。

为了解决这个问题，可以使用`basicQos`（Channel Prefetch Setting）方法，即当工人做完一个任务再给他下一个，不要一次性给多个任务。

```java
int prefetchCount = 1;
channel.basicQos(prefetchCount);
```

### 综合代码

综上四个问题，我们修改代码如下：

生产者：

```java
public class NewTask {
    private final static String QUEUE_NAME = "task_queue";

    public static void main(String[] argv) throws Exception {
        // 建立连接和管道
        Channel channel = RabbitUtil.getChannel();
        // 参数一：声明我们要发送的队列是谁（QUEUE_NAME），其他参数这里先不用关注
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        // 发送消息
        String message = new Scanner(System.in).nextLine();
//            String message = String.join(" ", argv); // ***主要改了这里***
        channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        System.out.println(" [x] 发送消息 '" + message + "'");
    }
}
```

消费者：

```java
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
                 * 第二个参数（见下图）
                 * multiple 的 true 和 false 代表不同意思
                 *      true 代表批量应答【 channel 上未应答的消息】
                 *              比如说 channel 上有传送 tag 的消息 5,6,7,8 当前 tag 是 8
                 *              那么此时 5-8 的这些还未应答的消息都会被确认收到消息应答
                 *      false 同上面相比只会应答 tag=8 的消息 5,6,7 这三个消息依然不会被确认收到消息应答
                 *
                 */
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
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
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/965ccf01a52243239813862e0a0fb6a3.png)

> Forgotten acknowledgement
> 有一个常见的错误是忽略`basicAck`。当客户退出时，信息看起来是被随机地重新交付了，但RabbitMQ会吃掉越来越多的内存，因为它不能释放任何还没确认的信息。
> 如何debug这种错误:
> ``
> sudo rabbitmqctl list_queues name messages_ready``
> ``
> messages_unacknowledged
> ``

------------

## 2.4 发布确认

> [官方文档链接](https://www.rabbitmq.com/confirms.html)

生产者将channel设置成 `confirm` 模式，一旦channel进入 `confirm` 模式，所有在该channel上面发布的消息都将会被指派一个唯一的 ID(从 1 开始)，一旦消息被投递到所有匹配的队列之后，broker 就会发送一个确认ACK给生产者(包含消息的唯一 ID)，这就使得生产者知道消息已经正确到达目的队列了。

如果消息和队列是可持久化的，那么确认消息会在将消息`写入磁盘之后`发出，broker 回传给生产者的确认消息中 `delivery-tag` 域包含了确认消息的序列号，此外 broker 也可以设置`basic.ack` 的 `multiple` 域，表示到这个序列号之前的所有消息都已经得到了处理。

confirm 模式最大的好处在于他是`异步`的，一旦发布一条消息，生产者应用程序就可以在等信道返回确认的同时继续发送下一条消息，当消息最终得到确认之后，生产者应用便可以通过回调方法来处理该确认消息。

如果 RabbitMQ 因为自身内部错误导致消息丢失，就会发送一条 `nack` 消息， 生产者应用程序同样可以在回调方法中处理该` nack `消息。

### 1.  开启发布确认

发布确认默认是关闭的，如果要开启需要调用方法 confirmSelect。

```java
channel.confirmSelect();
```

### 2.  单个确认发布

发布一个消息之后只有它被确认发布，后续的消息才能继续发布。
`waitForConfirmsOrDie(long)`这个方法只有在消息`被确认`的时候才返回，如果在指定时间范围内这个消息没有被确认那么它将抛出异常。

**缺点: **发布速度特别慢，因为如果没有确认发布的消息就会阻塞所有后续消息的发布，这种方式最多提供每秒不超过数百条发布消息的吞吐量。

```java
    public static void publishMessageIndividually() throws Exception {
        try (Channel channel = RabbitMqUtils.getChannel()) {
            String queueName = UUID.randomUUID().toString();
            channel.queueDeclare(queueName, false, false, false, null); 
            //开启发布确认
            channel.confirmSelect();
            long begin = System.currentTimeMillis();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String message = i + "";
                channel.basicPublish("", queueName, null, message.getBytes());
				
				//服务端返回 false 或超时时间内未返回，生产者可以消息重发
                boolean flag = channel.waitForConfirms();
                if (flag) {
                    System.out.println("消息发送成功");
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("发布" + MESSAGE_COUNT + "个单独确认消息,耗时" + (end - begin) + "ms");
        }
    }
```

### 3. 批量确认发布

与单个等待确认消息相比，先发布一批消息然后一起确认可以极大地提高吞吐量。

**缺点: ** 当发生故障导致发布出现问题时，不知道是哪个消息出现问题了，我们必须将整个批处理保存在内存中，以记录重要的信息而后重新发布消息。这种方案仍然是同步的，也一样阻塞消息的发布。

```java
    public static void publishMessageBatch() throws Exception {
        try (Channel channel = RabbitMqUtils.getChannel()) {
            String queueName = UUID.randomUUID().toString();
            channel.queueDeclare(queueName, false, false, false, null); 
            //开启发布确认
            channel.confirmSelect();
			//批量确认消息大小
            int batchSize = 100; 
            //未确认消息个数
            int outstandingMessageCount = 0;
            long begin = System.currentTimeMillis();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String message = i + "";
                channel.basicPublish("", queueName, null, message.getBytes());
                outstandingMessageCount++;
                if (outstandingMessageCount == batchSize) {
                    channel.waitForConfirms();
                    outstandingMessageCount = 0;
                }
            }
			//为了确保还有剩余没有确认消息 再次确认 
			if (outstandingMessageCount > 0)
            {
                channel.waitForConfirms();
            }
            long end = System.currentTimeMillis();
            System.out.println("发布" + MESSAGE_COUNT + "个批量确认消息,耗时" + (end - begin) + "ms");
        }
    }
```

### 4. 异步确认

异步确认虽然编程逻辑比上两个要复杂，但是性价比最高，无论是可靠性还是效率都更好。

异步确认是利用`回调函数`来达到消息可靠性传递的，这个中间件也是通过函数回调来保证是否投递成功。
![在这里插入图片描述](https://img-blog.csdnimg.cn/a8d540b4fc5b4236ab73fa0dc6479081.png)

```java
    public static void publishMessageAsync() throws Exception {
        try (Channel channel = RabbitMqUtils.getChannel()) {
            String queueName = UUID.randomUUID().toString();
            channel.queueDeclare(queueName, false, false, false, null); //开启发布确认
            channel.confirmSelect();
            /**
             * 线程安全有序的一个哈希表，适用于高并发的情况 
             * 1.轻松的将序号与消息进行关联
             * 2.轻松批量删除条目 只要给到序列号
             * 3.支持并发访问
             */
            ConcurrentSkipListMap<Long,String> outstandingConfirms = new ConcurrentSkipListMap<> ();
            /**
             * 确认收到消息的一个回调
             * 1.消息序列号 
             * 2.true可以确认小于等于当前序列号的消息
             *	false 确认当前序列号消息
             */
            ConfirmCallback ackCallback = (sequenceNumber, multiple) -> {
                if (multiple) {
					//返回的是小于等于当前序列号的未确认消息 是一个 map
                    ConcurrentNavigableMap<Long,String> confirmed = outstandingConfirms.headMap (sequenceNumber, true);
					//清除该部分未确认消息 
					confirmed.clear();
                } else {
					//只清除当前序列号的消息 
					outstandingConfirms.remove(sequenceNumber);
                }
            }; ConfirmCallback nackCallback = (sequenceNumber, multiple) -> {
                String message = outstandingConfirms.get(sequenceNumber);
                System.out.println("发布的消息" + message + "未被确认，序列号" + sequenceNumber);
            };
            /**
             * 添加一个异步确认的监听器 
             * 1.确认收到消息的回调
             * 2.未收到消息的回调
             */
            channel.addConfirmListener(ackCallback, null);
            long begin = System.currentTimeMillis();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String message = "消息" + i;
                /**
                 * channel.getNextPublishSeqNo()获取下一个消息的序列号 
                 * 通过序列号与消息体进行一个关联
                 * 全部都是未确认的消息体
                 */
                outstandingConfirms.put(channel.getNextPublishSeqNo(), message);
                channel.basicPublish("", queueName, null, message.getBytes());
            }
            long end = System.currentTimeMillis();
            System.out.println("发布" + MESSAGE_COUNT + "个异步确认消息,耗时" + (end - begin) + "ms");
        }
    }
```

> 如何处理异步未确认信息？
> 最好的解决的解决方案就是把未确认的消息放到一个基于内存的能被发布线程访问的队列， 
> 比如说用 ConcurrentLinkedQueue 这个队列在 confirm callbacks 与发布线程之间进行消息的传递。

### 5. 三种发布速度的对比

- 单独发布消息
  同步等待确认，简单，但吞吐量非常有限。
- 批量发布消息
  批量同步等待确认，简单，合理的吞吐量，一旦出现问题但很难推断出是哪条消息出现了问题。
- 异步处理
  最佳性能和资源使用，在出现错误的情况下可以很好地控制，但是实现起来稍微难些

------------

# 三、发布/订阅 `Publish / Subscribe`

> [官方文档链接](https://www.rabbitmq.com/tutorials/tutorial-three-java.html)：https://www.rabbitmq.com/tutorials/tutorial-three-java.html

- 工作队列模式：一个消息给一个接收者
- 发布订阅模式：一个消息给多个接收者

------------

### 案例说明

**建立一个日志记录系统：**

1. 一个程序发送日志消息
2. 另一个程序接收消息并打印

- 每一个接收程序都会收到日志，那我们就可以让一个接收者把日志持久化到磁盘，另一个接收者把日志打印出来。
- 每个发布的日志消息都会被广播给所有接收者。


## 3.1 交换机 `Exchanges, X`

RabbitMQ消息机制的核心思想是：生产者不直接把消息发给队列（他甚至不知道消息会被发给哪个队列），而是把消息发给交换机。

交换机 会知道要把这个消息发给哪个/哪些队列或丢弃。-> 使用`exchange type`来声明（`exchange type`包括`direct`，`topic`，`headers`，`fanout`）

```java
/**
* logs是这次交换的名称
* fanout：广播，把收到的信息发给所有的接收者
**/
channel.exchangeDeclare("logs", "fanout"); 
```

>Tip: 查看交换方式的命令：
> ``
>sudo rabbitmqctl list_exchanges
>``


在之前写工作队列时，我们没有指定交换方式，却也发送成功了信息，是因为我们是用了`匿名交换 (Nameless exchange)`，也就是默认交换。
`channel.basicPublish("", "hello", null, message.getBytes());` 这里的`""`就是是用了默认交换方式：消息会发送给在`routingKey`里查到的对应的queue。

由此，我们可以以广播形式发布对应的信息了，即

```java
channel.basicPublish("logs", "", null, message.getBytes());
```

【后续有合并的代码】


## 3.2 临时队列 `Temporary Queues`

我们的日志记录系统需要监听所有的日志消息，而不是只是一小部分。另外，我们只关注现在的消息，而不是过时的消息。因此，我们需要完成两件事：

1. 任何时候我们连接到Rabbit时，他会给我们全新的空队列，并生成随机队列名。
2. 断开连接时，队列会自动删除。

我们用以下语句，可以生成一个不持久化的、特有的、自动删除的队列：

```java
String queueName = channel.queueDeclare().getQueue();
```

> 特有的 `Exclusive`：used by only one connection and the queue will be deleted when that connection closes
> 文档Link：https://www.rabbitmq.com/queues.html

## 3.3 绑定 `Bindings`

![在这里插入图片描述](https://img-blog.csdnimg.cn/0783565f0d024e389056bb76889454c8.png)

我们已经创建了一种扇出`fanout`交换方式和一个队列，接下俩我们要让交换机把消息传给队列，这个关系就叫做绑定`binding`。

```java
channel.queueBind(queueName, "logs", "");
```

> 列出所有的绑定：`rabbitmqctl list_bindings`

## 3.4 案例代码

![在这里插入图片描述](https://img-blog.csdnimg.cn/3a49d76d96f0441ab652745790a85f3c.png)

代码整体和前文差别不大，主要在于定义了“logs”交换方式。

发送者：

```java
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
```

接收者：

```java
public class ReceiveLogs {
    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception {
        // 获得一个channel
        Channel channel = RabbitUtil.getChannel();
        // 声明交换模式
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        // 获得队列名称
        String queueName = channel.queueDeclare().getQueue();
        // 绑定队列和交换机
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        System.out.println(" [*] 等待信息. To exit press CTRL+C");

        // 收到消息的回调接口
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] 收到信息 '" + message + "'");
        };
        // 取消发送的回调接口
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息消费被中断");
        };
        // 接收信息
        channel.basicConsume(queueName,true,deliverCallback, cancelCallback);
    }
}
```

### 结果

![在这里插入图片描述](https://img-blog.csdnimg.cn/32149a48a5c64e088ffb997031948ecc.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/863baa8040b442cf80a8ed2014e3ce61.png)


# 四、路由 `Routing（Direct模式）`

- 工作队列模式：一个消息给一个接收者
- 发布订阅模式：一个消息给多个接收者
- 路由模式：接收者接收一部分信息

------------

## 4.1 绑定 `Bindings`

复习一下上文创建绑定的方式：

```java
channel.queueBind(queueName, EXCHANGE_NAME, "");
```

这里的`""` 实际上是路由绑定键`routingKey`参数。

```java
channel.queueBind(queueName, EXCHANGE_NAME, "black");
```

## 4.2 直接交换方式 `Direct Exchange`

在第三章中的日志记录系统中，我们做一些改进：只把一部分重要的信息写进磁盘，但仍然打印所有的日志信息。

与上文使用`fanout`模式不同，这里我们使用`direct`交换模式。这种模式将消息发送给对应的队列，这个队列和交换机的绑定键`binding key`和这条消息的路由键`routing key`是匹配的。

![在这里插入图片描述](https://img-blog.csdnimg.cn/ed42af53bf27444093439dc7b95f5316.png)

如果现在有一条消息的路由键`routing key`是“orange”，那么他会被发给Q1 队列。

## 4.3 多重绑定 `Multiple Bindings`

![在这里插入图片描述](https://img-blog.csdnimg.cn/43ff6828ba46401e9fc055804d824726.png)

你也可以给交换机和多个队列用同一个键绑定。

## 4.4 日志系统代码

![在这里插入图片描述](https://img-blog.csdnimg.cn/e95750fde9904f62b182cfec4fd46098.png)

### 对于发送者

创建一个直接交换方式的交换机：

```java
channel.exchangeDeclare(EXCHANGE_NAME, "direct");
```

我们用log的严重程度作为路由键，如 `info` / `warning` / `error`。

```java
channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes());
```

### 对于接收者

我们用log的严重程度作为绑定键：

```java
String queueName = channel.queueDeclare().getQueue();

String[] severities = new String[]{"log", "warning", "error"};
for(String severity : severities){
  channel.queueBind(queueName, EXCHANGE_NAME, severity);
}
```

### 合并代码

#### 生产者

```java
public class EmitLogDirect {
    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] argv) throws Exception {
        try (Channel channel = RabbitUtil.getChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
			//创建多个 bindingKey
            Map<String, String> bindingKeyMap = new HashMap<>();
            bindingKeyMap.put("info", "普通 info 信息");
            bindingKeyMap.put("warning", "警告 warning 信息");
            bindingKeyMap.put("error", "错误 error 信息"); 
            //debug 没有消费这接收这个消息 就丢失了
            bindingKeyMap.put("debug", "调试 debug 信息");
            for (Map.Entry<String, String> bindingKeyEntry :
                    bindingKeyMap.entrySet()) {
                String bindingKey = bindingKeyEntry.getKey();
                String message = bindingKeyEntry.getValue();
                channel.basicPublish(EXCHANGE_NAME, bindingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("生产者发出消息:" + message);
            }
        }
    }
}
```

#### 消费者

- 一部分写入磁盘

```java
public class ReceiveLogsDirectSaveToDisk {
    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] argv) throws Exception {
        Channel channel = RabbitUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        String queueName = "disk";
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "error");
        System.out.println("等待接收消息........... ");
        
        // 收到消息的回调接口，将日志写入磁盘
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            message = "接收绑定键:" + delivery.getEnvelope().getRoutingKey() + ",消息:" + message;
            File file = new File("./rabbitmq_info.txt");
            FileUtils.writeStringToFile(file, message, "UTF-8");
            System.out.println("错误日志已经接收");
        };
        // 取消发送的回调接口
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息消费被中断");
        };
        channel.basicConsume(queueName, true, deliverCallback, cancelCallback);
    }
}
```

- 一部分直接打印

```java
public class ReceiveLogsDirectPrintOut {
    private static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] argv) throws Exception {
        Channel channel = RabbitUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        String queueName = "console";
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "info");
        channel.queueBind(queueName, EXCHANGE_NAME, "warning");
        System.out.println("等待接收消息........... ");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" 接收绑定键 :" + delivery.getEnvelope().getRoutingKey() + ", 消息:" + message);
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}

```

### 结果

![在这里插入图片描述](https://img-blog.csdnimg.cn/3ef6f5bdac8f4bce90e02ab6a19b7c16.png)

![在这里插入图片描述](https://img-blog.csdnimg.cn/2978e30d61e74124bb4befce0a36edeb.png)


再看看rabbitMQ管理系统：
![在这里插入图片描述](https://img-blog.csdnimg.cn/d02f6900af4343848c6df4fb7935b44a.png)

# 五、主题模式`Topics`

- 工作队列模式：一个消息给一个接收者
- 发布订阅模式（`fanout`）：一个消息给多个接收者
- 路由模式（`direct`）：接收者接收一部分信息
- 主题模式（`topics`）：区分发送主体

------------

之前我们的日志系统实现了根据不同信息传给不同的队列，现在我们需要对信息进一步筛选。例如，在Unix系统中，log可能有`info/warn/crit`的情况，这些log可能是从`auth/cron/kern..`传送来的，那么如果我们需要区分发送log的主体，仅接受来自cron的critical errors，就需要用到`topic`交换方式。

![在这里插入图片描述](https://img-blog.csdnimg.cn/daaa50b807ed4e2dbc4de850e2c6f496.png)


- **路由键** `routing key`
  发送给topic交换模式的交换机 的消息 不能用随意的`routing_key`，它的路由键必须是一系列用`"."`隔开的词语，例如`quick.orange.rabbit` / `stock.usd.nyse`。词语的数量可以随便你，但是总长度不能超过`255字节`。
- **绑定键** `binding key`
  绑定键和路由键是同一个格式，消息会被发送给能和它路由键匹配的绑定键线路。没有match的消息就会被丢掉。比如，`*.orange.*` / `*.*.rabbit` / `quick.orange.rabbit.#`。
  - **星号** `"*"`：代替一个词
  - **井号** `"#"`：代替零个或多个词


当队列的绑定键都是 `#`，topic exchange就和fanout exchange是一样的。
当队列的绑定键没有`*`和`#`时，topic exchange就和direct exchange是一样的。

## 示例代码

生产者

```java
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
```

消费者Q1

```java
public class ReveiveLogsTopicQ1 {
    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args)throws Exception{
        // 建立channel
        Channel channel = RabbitUtil.getChannel();

        // 声明交换
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        // 声明Q1队列与绑定关系
        String queueName = "Q1";
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME,"*.orange.*");

        System.out.println("等待接收消息。。匹配模式为\"*.orange.*\"");
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
```

消费者Q2

```java
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
```

### 结果

![在这里插入图片描述](https://img-blog.csdnimg.cn/f762196b886547c297afc551a276a887.png)


![在这里插入图片描述](https://img-blog.csdnimg.cn/114517fc1e334cb2bee646ed7b58da67.png)


# 六、死信队列

![请添加图片描述](https://img-blog.csdnimg.cn/3c46119d06144072bddec97754171dc1.png)

**死信**：由于某些原因（消息TTL过期、队列达到最大长度、消息被拒绝）导致队列中的消息无法被处理。
**RabbitMQ死信队列机制**：当消息消费发生异常时，将消息投入死信队列。（例如，用户下单成功但未在指定时间内支付 -> 消息自动失效）

## 代码模拟死信三种情况

## 6.1 消息TTL过期

代码结构图见上图

### 生产者

```java
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

```

### 消费者

消费者1，处理正常队列中的信息

```java
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

        String normalQueue = "normal_queue";
        // 将设置死信的参数params放进正常队列声明中
        channel.queueDeclare(normalQueue,false,false, false,params);
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

```


消费者2，处理死信队列中的信息

```java
public class Consumer02 {
    private static final String DEAD_EXCHANGE = "dead_exchange";

    public static void main(String[] args) throws Exception {
        // 建立channel
        Channel channel = RabbitUtil.getChannel();
        // 声明交换机
        channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);

        // 死信队列声明及绑定交换机
        String deadQueue = "dead-queue";
        channel.queueDeclare(deadQueue, false, false, false, null);
        channel.queueBind(deadQueue, DEAD_EXCHANGE, "lisi");

        System.out.println("等待接收死信队列信息。。。。");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Consumer02 接收到死信队列中的信息： " + message);
        };
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("接收失败");
        };

        channel.basicConsume(deadQueue, true, deliverCallback, cancelCallback);
    }
}
```

### 结果

（C1需要启动完先关闭，再打开生产者）
![在这里插入图片描述](https://img-blog.csdnimg.cn/14f2b74c13654035b84ba2c14df1f771.png)

此时再打开死信队列，死信队列里的消息被C2消费。
![在这里插入图片描述](https://img-blog.csdnimg.cn/b1bcdf3d1f3347c99eb58097f7e8647f.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/3d31944c03e4426eb706a6450262ca81.png)

## 6.2 队列达到最大长度

在6.1代码中修改两处地方：

1. 去掉生产者代码中的TTL语句
2. 在C1消费者代码中添加 `param.put("x-max-length", 6)`，设置正常队列的长度限制。

![在这里插入图片描述](https://img-blog.csdnimg.cn/b23ea8fe78d94af5969d409a18a4095c.png)

## 6.3 消息被拒

在6.2代码的基础上，修改C1消费者代码（生产者和C2消费者不变）：

1. 改为手动应答，修改`DeliverCallback`。
2. 删除`param.put("x-max-length", 6)`

```java
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            if (message.equals("info5")) {
                System.out.println("Consumer01 接收到消息" + message + "并拒绝签收该消息");
                //requeue 设置为 false 代表拒绝重新入队 该队列如果配置了死信交换机将发送到死信队列中
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            } else {
                System.out.println("Consumer01 接收到消息" + message);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

        boolean autoAck = false;

        channel.basicConsume(normalQueue, autoAck, deliverCallback, cancelCallback);
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/184f13f2e9d446e6b3f1c20f2ba543d2.png)

# 七、延迟队列

延迟队列：队列中的元素需要在指定时间取出和处理。例如，用户发起订单，十分钟内未支付则自动取消。

当数据量很大时，采取轮询的方式显然是不合理的，会给数据库带来很大压力。



![在这里插入图片描述](https://img-blog.csdnimg.cn/5bdb2920d4b941858e4424864b8f6890.png)

## 7.1 RabbitMQ中的TTL 

TTL，最大存活时间，表明消息或该队列中所有消息的最大存活时间。
有两种方式设置：

1. 针对每条信息设置TTL

```java
rabbitTemplate.convertAndSend("X", "XC", message, correlationData -> {
			correlationData.getMessageProperties().setExpiration(ttlTime);
			return correlationData;
})
```

2. 在创建队列时设置队列的`x-message-ttl`属性

```java
params.put("x-message-ttl", 5000);
return QueueBuilder.durable(QUEUE_A).withArguments(params).build();
```

如果设置了队列的 TTL 属性，那么一旦消息过期，就会被队列丢弃(如果配置了死信队列被丢到死信队列中)。
而第二种方式，消息即使过期，也不一定会被马上丢弃，因为消息是否过期是在即将投递到消费者之前判定的。
如果当前队列有严重的消息积压情况，则已过期的消息也许还能存活较长时间;另外，还需要注意的一点是，如果不设置 TTL，表示消息永远不会过期，如果将 TTL 设置为 0，则表示除非此时可以直接投递该消息到消费者，否则该消息将会被丢弃。

`延时队列核心  = 死信队列 + TTL`：TTL让消息延迟多久后成为死信，消费者一直处理死信队列里的信息就行。


## 7.2 整合SpringBoot

### 1. 添加依赖

Springboot版本：2.6.7
JDK：8

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

<!--        rabbitMQ依赖-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.76</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-boot-starter</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
            <version>3.0.0</version>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>25.1-jre</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
```

### 2. 修改配置文件 application.properties

```xml
spring.rabbitmq.host=你的主机ip
spring.rabbitmq.port=5672
spring.rabbitmq.username=你的rabbit用户名
spring.rabbitmq.password=你的rabbit密码
```

### 3. 添加swagger配置类

建立一个config包，SwaggerConfig类。

```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket webApiConfig() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("webApi")
                .apiInfo(webApiInfo())
                .select()
                .build();
    }

    private ApiInfo webApiInfo() {
        return new ApiInfoBuilder()
                .title("RabbitMQ 接口文档")
                .description("本文档描述了rabbitmq微服务接口定义")
                .version("1.0")
                .contact(new Contact("cherry", "http://xxxx.github.io/", "xxxx@qq.com"))
                .build();
    }
}
```

## 7.3 队列TTL

![代码架构图](https://img-blog.csdnimg.cn/daf74eda1de54717b043110535e94cd2.png)

创建两个队列QA和QB，两者队列TTL分别设置为10秒和40秒，然后再创建一个交换机X和死信交换机Y，它们的类型都是direct，创建一个死信队列QD。

根据以上架构图，配置队列、交换机、绑定。

```java
@Configuration
public class TtlQueueConfig {
    public static final String X_EXCHANGE = "X";
    public static final String QUEUE_A = "QA";
    public static final String QUEUE_B = "QB";
    
    public static final String Y_DEAD_LETTER_EXCHANGE = "Y";
    public static final String DEAD_LETTER_QUEUE = "QD";
    
    // 声明 XExchange
    @Bean("xExchange")
    public DirectExchange xExchange(){
        return new DirectExchange(X_EXCHANGE);
    }
    // 声明 YExchange
    @Bean("yExchange")
    public DirectExchange yExchange(){
        return new DirectExchange(Y_DEAD_LETTER_EXCHANGE);
    }
    
    // 声明队列A，ttl为10s，绑定到对应的死信交换机
    @Bean("queueA")
    public Queue queueA(){
        Map<String, Object> args = new HashMap<>(3);
        // 声明当前队列绑定的死信交换机
        args.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        // 声明当前队列的死信路由key
        args.put("x-dead-letter-routing-key","YD");
        // 声明队列的TTL
        args.put("x-message-ttl", 10000);
        
        return QueueBuilder.durable(QUEUE_A).withArguments(args).build();
    }
    // 声明队列A绑定X交换机
    @Bean
    public Binding queueaBindingX(@Qualifier("queueA") Queue queueA,
                                  @Qualifier("xExchange") DirectExchange xExchange){
        return BindingBuilder.bind(queueA).to(xExchange).with("XA");
    }

    // 声明队列B，ttl为40s，绑定到对应的死信交换机
    @Bean("queueB")
    public Queue queueB(){
        Map<String, Object> args = new HashMap<>(3);
        // 声明当前队列绑定的死信交换机
        args.put("x-dead-letter-exchange", Y_DEAD_LETTER_EXCHANGE);
        // 声明当前队列的死信路由key
        args.put("x-dead-letter-routing-key","YD");
        // 声明队列的TTL
        args.put("x-message-ttl", 40000);
        
        return QueueBuilder.durable(QUEUE_B).withArguments(args).build();
    }
    // 声明队列B绑定X交换机
    @Bean
    public Binding queuebBindingX(@Qualifier("queueB") Queue queueB, 
                                  @Qualifier("xExchange") DirectExchange xExchange){
        return BindingBuilder.bind(queueB).to(xExchange).with("XB");
    }
    
    // 声明死信队列QD
    @Bean("queueD")
    public Queue queueD(){
        return new Queue(DEAD_LETTER_QUEUE);
    }
    // 声明死信队列和Y交换机的绑定关系
    @Bean
    public Binding deadLetterBindingQAD(@Qualifier("queueD") Queue queueD,
                                        @Qualifier("yExchange") DirectExchange yExchange){
        return BindingBuilder.bind(queueD).to(yExchange).with("YD");
    }
}
```

### 测试

发起一个请求： http://localhost:8080/ttl/sendMsg/HelloCherry~

![在这里插入图片描述](https://img-blog.csdnimg.cn/a5b9eec739134fa59d362910854824a9.png)

## 7.4 延时队列优化

![在这里插入图片描述](https://img-blog.csdnimg.cn/629001e7f24b462ebba5cdfb1d58c983.png)

配置文件代码

```java
@Configuration
public class MsgTtlQueueConfig {
    private static final String Y_DEAD_LETTER_EXCHANGE = "Y";
    private static final String QUEUE_C = "QC";

    // 声明队列C 死信交换机
    @Bean("queueC")
    public Queue queueB(){
        Map<String, Object> args = new HashMap<>(3);
        // 声明当前队列绑定的死信交换机
        args.put("x-dead-letter-exchange",Y_DEAD_LETTER_EXCHANGE);
        // 声明当前队列的死信路由
        args.put("x-dead-letter-routing-key","YD");
        // 没有声明TTL属性
        return QueueBuilder.durable(QUEUE_C).withArguments(args).build();
    }

    // 声明队列B 绑定 X 交换机
    @Bean
    public Binding queueBindingX(@Qualifier("queueC") Queue queueC,
                                 @Qualifier("xExchange")DirectExchange xExchange){
        return BindingBuilder.bind(queueC).to(xExchange).with("XC");
    }
}

```

消息生产者代码

```java
    @GetMapping("sendExpirationMsg/{message}/{ttlTime}")
    public void sendMsg(@PathVariable String message, @PathVariable String ttlTime){
        rabbitTemplate.convertAndSend("X", "XC", message, correlationData ->{
            correlationData.getMessageProperties().setExpiration(ttlTime);
            return correlationData;
        });
        log.info("当前时间：{}, 发送一条时长{}毫秒TTL消息给队列C:{}", new Date(), ttlTime, message);
    }
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/de638308559e4f1d927fbcde0e7065e8.png)

在最开始的时候，就介绍过如果使用在消息属性上设置 TTL 的方式，消息可能并不会按时“死亡“，因为 RabbitMQ 只会检查第一个消息是否过期，如果过期则丢到死信队列。

如果第一个消息的延时时长很长，而第二个消息的延时时长很短，第二个消息并不会优先得到执行。


## 7.5 RabbitMQ插件实现延迟队列

下载插件，上传到`/usr/lib/rabbitmq/lib/rabbitmq_server-3.7.18/plugins`：
https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases

```shell
cd /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.18/plugins
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/88bcf3414e75408f83ea1ceee40ac050.png)

在我们自定义的交换机中，这是一种新的交换类型，该类型消息支持延迟投递机制 
消息传递后并不会立即投递到目标队列中，而是`存储在 mnesia(一个分布式数据系统)表`中，当达到投递时间时，才投递到目标队列中。


配置类文件代码

```java
@Configuration
public class DelayedQueueConfig {
    public static final String DELAYED_QUEUE_NAME = "delayed.queue";
    public static final String DELAYED_EXCHANGE_NAME = "delayed.exchange";
    public static final String DELAYED_ROUTING_KEY = "delayed.routingkey";

    @Bean
    public Queue delayedQueue(){
        return new Queue(DELAYED_QUEUE_NAME);
    }

    @Bean
    public CustomExchange delayedExchange(){
        // 自定义交换机类型
        Map<String, Object> args = new HashMap<>(3);
        args.put("x-delayed-type","direct");
        return new CustomExchange(DELAYED_EXCHANGE_NAME,"x-delayed-message", true, false, args);
    }

    @Bean
    public Binding bindingDelayedQueue(@Qualifier("delayedQueue") Queue queue,
                                       @Qualifier("delayedExchange") CustomExchange delayedExchange){
        return BindingBuilder.bind(queue).to(delayedExchange).with(DELAYED_ROUTING_KEY).noargs();
    }
}
```


- 消息生产者代码

```java
    public static final String DELAYED_EXCHANGE_NAME = "delayed.exchange";
    public static final String DELAYED_ROUTING_KEY = "delayed.routingkey";

    @GetMapping("sendDelayMsg/{message}/{delayTime}")
    public void sendMsg(@PathVariable String message, @PathVariable Integer delayTime){
        rabbitTemplate.convertAndSend(DELAYED_EXCHANGE_NAME, DELAYED_ROUTING_KEY, message, correlationData ->{
            correlationData.getMessageProperties().setDelay(delayTime);
            return correlationData;
        });
        log.info("当前时间：{}，发送一条延迟 {} 毫秒的信息给队列 delayed.queue：{}", new Date(), delayTime, message);
    }
```

- 消息消费者代码

```java
@Component
@Slf4j
public class ConsumerController {
    public static final String DELAYED_QUEUE_NAME = "delayed.queue";
    @RabbitListener(queues = DELAYED_QUEUE_NAME)
    public void receiveDelayedQueue(Message message){
        String msg = new String(message.getBody());
        log.info("当前时间：{}，收到延时队列的信息：{}",new Date(), msg);
    }
}
```

发起请求:
http://localhost:8080/ttl/sendDelayMsg/come on baby1/20000 
http://localhost:8080/ttl/sendDelayMsg/come on baby2/2000
![在这里插入图片描述](https://img-blog.csdnimg.cn/d874c379a61a45cc97bc7c5c25eb865d.png)

延时队列在需要延时处理的场景下非常有用，使用 RabbitMQ 来实现延时队列可以很好的利用 RabbitMQ 的特性，如:消息可靠发送、消息可靠投递、死信队列来保障消息至少被消费一次以及未被正确处理的消息不会被丢弃。
另外，通过 RabbitMQ 集群的特性，可以很好的解决单点故障问题，不会因为单个节点挂掉导致延时队列不可用或者消息丢失。


当然，延时队列还有很多其它选择，比如利用 Java 的 DelayQueue，利用 Redis 的 zset，利用 Quartz 或者利用 kafka 的时间轮，这些方式各有特点,看需要适用的场景。


# 八、发布确认高级

在生产环境中由于一些不明原因，导致 rabbitmq 重启，在 RabbitMQ 重启期间生产者消息投递失败， 导致消息丢失，需要手动处理和恢复。于是，我们开始思考，如何才能进行 RabbitMQ 的消息可靠投递呢? 特 别是在这样比较极端的情况，RabbitMQ 集群不可用的时候，无法投递的消息该如何处理呢？

## 8.1 发布确认SpringBoot版本

### 1. 确认机制方案

![在这里插入图片描述](https://img-blog.csdnimg.cn/eff3c9297757413abf32a7161190355e.png)

### 2. 代码架构图

![在这里插入图片描述](https://img-blog.csdnimg.cn/fa2b76a526334d35a2586c362a675c13.png)


### 3. 配置文件

配置文件中添加

```xml
spring.rabbitmq.publisher-confirm-type=correlated
```

- NONE：禁用发布确认模式，是默认值
- CORRELATED：发布消息成功到交换器后会触发回调方法
- SIMPLE：经测试有两种效果，其一效果和CORRELATED值一样会触发回调方法；其二在发布消息成功后使用`rabbitTemplate`调用`waitForConfirmsOrDie`方法等待broker节点返回发送结果，根据返回结果来判定下一步的逻辑。`waitForConfirmsOrDie`方法如果返回false则会关闭channel，则接下来无法发送消息到broker。

### 4. 添加配置类

```java
@Configuration
public class ConfirmConfig {
    public static final String CONFIRM_EXCHANGE_NAME = "confirm.exchange";
    public static final String CONFIRM_QUEUE_NAME = "confirm.queue";

    // 声明业务Exchange
    @Bean("confirmExchange")
    public DirectExchange confirmExchange(){
        return new DirectExchange(CONFIRM_EXCHANGE_NAME);
    }

    // 声明确认队列
    @Bean("confirmQueue")
    public Queue confirmQueue(){
        return QueueBuilder.durable(CONFIRM_QUEUE_NAME).build();
    }

    // 声明确认队列绑定关系
    @Bean
    public Binding queueBinding(@Qualifier("confirmQueue") Queue queue,
                                @Qualifier("confirmExchange") DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with("key1");
    }
}
```

### 5. 消息生产者

```java
@RestController
@RequestMapping("/confirm")
@Slf4j
public class Producer {
    public static final String CONFIRM_EXCHANGE_NAME = "confirm.exchange";
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MyCallBack myCallBack;

    // 依赖注入rabbitTemplate之后再设置它的回调对象
    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(myCallBack);
    }
    @GetMapping("sendMessage/{message}")
    public void sendMessage(@PathVariable String message){
        // 指定消息id为1
        CorrelationData correlationData1 = new CorrelationData("1");
        String routingKey = "key1";

        rabbitTemplate.convertAndSend(CONFIRM_EXCHANGE_NAME, routingKey, message+routingKey, correlationData1);

        CorrelationData correlationData2 = new CorrelationData("2");
        routingKey = "key2";
        rabbitTemplate.convertAndSend(CONFIRM_EXCHANGE_NAME, routingKey, message+routingKey, correlationData2);

        log.info("发送消息内容：{}", message);
    }
}
```


#### 回调接口

```java
@Component
@Slf4j
public class MyCallBack implements RabbitTemplate.ConfirmCallback {
    /**
     * 交换机不管是否收到消息的一个回调方法
     * CorrelationData  消息相关数据
     * ack              交换机是否收到消息
     * @param correlationData
     * @param b
     * @param s
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {
        String id = correlationData!=null? correlationData.getId() : "";
        if (b){
            log.info("交换机已经收到id为{}的消息",id);
        } else {
            log.info("交换机还未收到id为{}的消息，由于原因：{}", id, s);
        }
    }
}
```

### 6. 消息消费者

```java
@Component
@Slf4j
public class ConfirmConsumer {
    public static final String CONFIRM_QUEUE_NAME = "confirm.queue";
    @RabbitListener(queues = CONFIRM_QUEUE_NAME)
    public void receiveMsg(Message message){
        String msg = new String(message.getBody());
        log.info("接收到队列confirm.queue消息：{}", msg);
    }
}
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/0d66dd075bca44599883d68cf06db0fb.png)


## 8.2 回退消息

### 1. Mandatory参数

在仅开启了生产者确认机制的情况下，交换机接收到消息后，会直接给消息生产者发送确认消息。

- 如果发现该消息不可路由，那么消息会被直接丢弃，此时生产者是不知道消息被丢弃这个事件的。
- 那么如何让无法被路由的消息帮我想办法处理一下? 最起码通知我一声，我好自己处理啊。通过设置 mandatory 参数可以在当消息传递过程中`不可达目的地时`将消息`返回给生产者`。
  （“转接人工服务”）


### 2. 消息生产者代码

```java
@Slf4j
@Component
public class MessageProducer implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // rabbitTemplate 注入之后就设置该值
    @PostConstruct
    private void init() {
        /**
         * true: 交换机无法将消息进行路由时，会将该消息返回给生产者
         * false：如果发现消息无法进行路由，则直接丢弃
         */
        rabbitTemplate.setMandatory(true);
        // 设置回退消息交给谁处理
        rabbitTemplate.setReturnsCallback(this);
    }

    @GetMapping("sendMessage")
    public void sendMessage(String message) {
        // 让消息绑定一个ID值
        CorrelationData correlationData1 = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend("confirm.exchange", "key1", message + "key1", correlationData1);
        log.info("发送消息id为-{}，内容为-{}", correlationData1.getId(), message + "key1");
        CorrelationData correlationData2 = new CorrelationData(UUID.randomUUID().toString());
        log.info("发送消息id为-{}，内容为-{}", correlationData2.getId(), message + "key2");
    }


    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = correlationData != null ? correlationData.getId() : "";
        if (ack) {
            log.info("交换机收到消息确认成功, id:{}", id);
        } else {
            log.error("消息id：{} 未成功投递到交换机，原因是：{} ", id, cause);
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.info("消息：{}被服务器退回，退回原因：{}，交换机是：{}，路由key：{}",
                returnedMessage.getMessage().getBody(), returnedMessage.getReplyText(),
                returnedMessage.getExchange(), returnedMessage.getRoutingKey());
    }
}
```

#### 回调接口

```java
@Component
@Slf4j
public class MyCallBack implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {
    /**
     * 交换机不管是否收到消息的一个回调方法
     * CorrelationData  消息相关数据
     * ack              交换机是否收到消息
     * @param correlationData
     * @param b
     * @param s
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {
        String id = correlationData!=null? correlationData.getId() : "";
        if (b){
            log.info("交换机已经收到id为{}的消息",id);
        } else {
            log.info("交换机还未收到id为{}的消息，由于原因：{}", id, s);
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("消息{}, 被交换机{}退回，退回原因：{}，路由key：{} ",
                returnedMessage.getMessage().getBody(), returnedMessage.getExchange(),
                returnedMessage.getReplyText(), returnedMessage.getRoutingKey());
    }
}
```

### 3. 消息消费者

```java
@Component
@Slf4j
public class ConfirmConsumer {
    public static final String CONFIRM_QUEUE_NAME = "confirm.queue";
    @RabbitListener(queues = CONFIRM_QUEUE_NAME)
    public void receiveMsg(Message message){
        String msg = new String(message.getBody());
        log.info("接收到队列confirm.queue消息：{}", msg);
    }
}
```


## 8.3 备份交换机

使用`mandatory`参数和回退消息，我们可以处理无法投递的信息，但具体而言我们应该如何处理这些无法路由的信息呢？我们可以建立一个日志，发出警告然后人工处理他们。但是如果有很多台机器，这样做就会很麻烦。设置`mandatory`参数还会增加生产者的复杂度（因为需要额外写一些逻辑去处理这些返回的消息）。

之前的文章中，我们用死信队列处理失败的消息，但是不能路由的消息是不能进入到队列的，因此没法用死信队列。

这时我们就可以用`备份交换机`来处理这个问题。当一个交换机收到不可路由的消息时，他会把消息转发到备份交换机中，由备份交换机来转发和处理，通常备份交换机的交换方式为fanout，即将所有消息投递到与其绑定的所有队列中。

当然，我们也可以建立一个报警队列，用独立的消费者来监测和报警。

### 1. 代码架构图

![在这里插入图片描述](https://img-blog.csdnimg.cn/6389be28315b43528a2208e67cae7cef.png)


### 2. 修改配置类

```java
@Configuration
public class ConfirmConfig {
    public static final String CONFIRM_EXCHANGE_NAME = "confirm.exchange";
    public static final String CONFIRM_QUEUE_NAME = "confirm.queue";
    public static final String BACKUP_EXCHANGE_NAME = "backup.exchange";
    public static final String BACKUP_QUEUE_NAME = "backup.queue";
    public static final String WARNING_QUEUE_NAME = "warning.exchange";

    // 声明确认队列
    @Bean("confirmQueue")
    public Queue confirmQueue(){
        return QueueBuilder.durable(CONFIRM_QUEUE_NAME).build();
    }

    // 声明确认队列绑定关系
    @Bean
    public Binding queueBinding(@Qualifier("confirmQueue") Queue queue,
                                @Qualifier("confirmExchange") DirectExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with("key1");
    }

    // 声明备份Exchange
    @Bean("backupExchange")
    public FanoutExchange backupExchange(){
        return new FanoutExchange(BACKUP_EXCHANGE_NAME);
    }

    // 声明业务Exchange
    @Bean("confirmExchange")
    public DirectExchange confirmExchange(){
        ExchangeBuilder exchangeBuilder = ExchangeBuilder.directExchange(CONFIRM_EXCHANGE_NAME)
                .durable(true).withArgument("alternate-exchange", BACKUP_EXCHANGE_NAME); // 设置该交换机的备份交换机
        return (DirectExchange) exchangeBuilder.build();
    }

    // 声明警告队列
    @Bean("warningQueue")
    public Queue warningQueue(){
        return QueueBuilder.durable(WARNING_QUEUE_NAME).build();
    }

    // 声明报警队列绑定关系
    @Bean
    public Binding warningBinding(@Qualifier("warningQueue") Queue queue,
                                  @Qualifier("backupExchange") FanoutExchange backupExchange){
        return BindingBuilder.bind(queue).to(backupExchange);
    }

    // 声明备份队列
    @Bean("backQueue")
    public Queue backQueue(){
        return QueueBuilder.durable(BACKUP_QUEUE_NAME).build();
    }

    // 声明备份队列绑定关系
    @Bean
    public Binding backupBinding(@Qualifier("backQueue") Queue queue,
                                 @Qualifier("backupExchange") FanoutExchange backupExchange) {
        return BindingBuilder.bind(queue).to(backupExchange);
    }
}

```

### 3. 报警消费者

```java
@Component
@Slf4j
public class WarningConsumer {
    public static final String WARNING_QUEUE_NAME = "warning.queue";
    @RabbitListener(queues = WARNING_QUEUE_NAME)
    public void receiveWarningMsg(Message message){
        String msg = new String(message.getBody());
        log.error("报警发现不可路由消息：{}", msg);
    }
}
```

mandatory 参数与备份交换机可以一起使用的时候，如果两者同时开启，消息究竟何去何从?谁优先 级高，答案是`备份交换机优先级高`。


# 九、RabbitMQ其他知识点

## 9.1 幂等性

用户对于同一操作发起的一次请求或者多次请求的结果是一致的，不会因为多次点击而产生了副作用。 举个最简单的例子，那就是支付，用户购买商品后支付，支付扣款成功，但是返回结果的时候网络异常， 此时钱已经扣了，用户再次点击按钮，此时会进行第二次扣款，返回结果成功，用户查询余额发现多扣钱 了，流水记录也变成了两条。在以前的单应用系统中，我们只需要把数据操作放入事务中即可，发生错误 立即回滚，但是再响应客户端的时候也有可能出现网络中断或者异常等等。

消费者在消费 MQ 中的消息时，MQ 已把消息发送给消费者，消费者在给MQ 返回 ack 时网络中断， 故 MQ 未收到确认信息，该条消息会重新发给其他的消费者，或者在网络重连后再次发送给该消费者，但 实际上该消费者已成功消费了该条消息，造成消费者消费了重复的消息。

MQ 消费者的幂等性的解决一般使用全局 ID 或者写个唯一标识比如时间戳 或者 UUID 或者订单消费者消费 MQ 中的消息也可利用 MQ 的该 id 来判断，或者可按自己的规则生成一个全局唯一 id，每次消费消 息时用该 id 先判断该消息是否已消费过。

在海量订单生成的业务高峰期，生产端有可能就会重复发生了消息，这时候消费端就要实现幂等性， 这就意味着我们的消息永远不会被消费多次，即使我们收到了一样的消息。业界主流的幂等性有两种操作:
a. 唯一 ID+指纹码机制,利用数据库主键去重, 
b.利用 redis 的原子性去实现。


- 唯一 ID+指纹码机制
  指纹码:我们的一些规则或者时间戳加别的服务给到的唯一信息码,它并不一定是我们系统生成的，基本都是由我们的业务规则拼接而来，但是一定要保证唯一性，然后就利用查询语句进行判断这个 id 是否存 在数据库中,优势就是实现简单就一个拼接，然后查询判断是否重复;劣势就是在高并发时，如果是单个数 据库就会有写入性能瓶颈当然也可以采用分库分表提升性能，但也不是我们最推荐的方式。
- Redis原子性
  利用 redis 执行 setnx 命令，天然具有幂等性。从而实现不重复消费


## 9.2 优先级队列

在我们系统中有一个订单催付的场景，我们的客户在天猫下的订单,淘宝会及时将订单推送给我们，如果在用户设定的时间内未付款那么就会给用户推送一条短信提醒，很简单的一个功能对吧，但是，tmall 商家对我们来说，肯定是要分大客户和小客户的对吧，比如像苹果，小米这样大商家一年起码能给我们创 造很大的利润，所以理应当然，他们的订单必须得到优先处理，而曾经我们的后端系统是使用 redis 来存放的定时轮询，大家都知道 redis 只能用 List 做一个`简简单单的消息队列，并不能实现一个优先级的场景`，所以订单量大了后采用 RabbitMQ 进行改造和优化,如果发现是大客户的订单给一个相对比较高的优先级， 否则就是默认优先级。

### 如何添加

#### 1. 控制页面添加

![在这里插入图片描述](https://img-blog.csdnimg.cn/26e5fbf4f13a4713b42a65cd3a9d0b4b.png)

#### 2. 队列代码 添加优先级

```java
Map<String, Object> params = new HashMap();
params.put("x-max-priority", 10);
channel.queueDeclare("hello", true, false, false, params);
```

#### 3. 消息代码 添加优先级

```java
AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().priority(5).build();
```

#### 注意事项

要让队列实现优先级需要做的事情:

1. 队列需要设置为优先级队列，
2. 消息需要设置消息的优先级，
3. 消费者需要等待消息已经发送到队列中才去消费，这样才有机会对消息进行排序


### Practice

消息生产者

```java
public class Producer {
	private static final String QUEUE_NAME="hello";
	public static void main(String[] args) throws Exception {
		try (Channel channel = RabbitMqUtils.getChannel();) {
			//给消息赋予一个 priority 属性
			AMQP.BasicProperties properties = new
			AMQP.BasicProperties().builder().priority(5).build(); for (int i = 1; i <11; i++){
				String message = "info"+i;
				if(i==5){
					channel.basicPublish("", QUEUE_NAME, properties, message.getBytes());
				}else{
					channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
				}
				System.out.println("发送消息完成:" + message); 
			}
		} 
	}
}
```

消息消费者

```java
public class Consumer {
	private static final String QUEUE_NAME="hello";
	public static void main(String[] args) throws Exception{
		Channel channel = RabbitMqUtils.getChannel();
		//设置队列的最大优先级 最大可以设置到 255 官网推荐 1-10 如果设置太高比较吃内存和 CPU
		Map<String, Object> params = new HashMap(); params.put("x-max-priority", 10);
		channel.queueDeclare(QUEUE_NAME, true, false, false, params);
		System.out.println("消费者启动等待消费.............."); DeliverCallback deliverCallback=(consumerTag, delivery)->{ 
			String receivedMessage = new String(delivery.getBody());System.out.println("接收到消 息:"+receivedMessage);};
		channel.basicConsume(QUEUE_NAME,true,deliverCallback,(consumerTag)- >{
			System.out.println("消费者无法消费消息时调用，如队列被删除");
		}); 
	}
```

## 9.3 惰性队列

RabbitMQ 从 3.6.0 版本开始引入了惰性队列的概念。惰性队列`会尽可能的将消息存入磁盘中`，而在消费者消费到相应的消息时才会被加载到内存中，它的一个重要的设计目标是能够支持更长的队列，即支持更多的消息存储。当消费者由于各种各样的原因(比如消费者下线、宕机亦或者是由于维护而关闭等)而致使长时间内不能消费消息造成堆积时，惰性队列就很有必要了。

默认情况下，当生产者将消息发送到 RabbitMQ 的时候，队列中的消息会尽可能的存储在内存之中， 这样可以更加快速的将消息发送给消费者。即使是持久化的消息，在被写入磁盘的同时也会在内存中驻留 一份备份。当 RabbitMQ 需要释放内存的时候，会将内存中的消息换页至磁盘中，这个操作会耗费较长的 时间，也会阻塞队列的操作，进而无法接收新的消息。虽然 RabbitMQ 的开发者们一直在升级相关的算法， 但是效果始终不太理想，尤其是在消息量特别大的时候。

队列具备两种模式:default 和 lazy。默认的为 default 模式，在 3.6.0 之前的版本无需做任何变更。lazy 模式即为惰性队列的模式，可以通过调用 channel.queueDeclare 方法的时候在参数中设置，也可以通过 Policy 的方式设置，如果一个队列同时使用这两种方式设置的话，那么 Policy 的方式具备更高的优先级。 如果要通过声明的方式改变已有队列的模式的话，那么只能先删除队列，然后再重新声明一个新的。
在队列声明的时候可以通过“x-queue-mode”参数来设置队列的模式，取值为“default”和“lazy”。下面示 例中演示了一个惰性队列的声明细节:

```java
Map<String, Object> args = new HashMap<String, Object>(); 
args.put("x-queue-mode", "lazy"); 
channel.queueDeclare("myqueue", false, false, false, args);
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/d246567897fa4f64b37285c7408356d9.png)

在发送 1 百万条消息，每条消息大概占 1KB 的情况下，普通队列占用内存是 1.2GB，而惰性队列仅仅 占用 1.5MB。
