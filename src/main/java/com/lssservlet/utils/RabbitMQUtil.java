package com.lssservlet.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownSignalException;

public class RabbitMQUtil {
    public static void main(String[] args) throws Exception {
        new RabbitMQUtil();
    }

    public RabbitMQUtil() throws Exception {
        Subscriber subscriber1 = new Subscriber("localhost", "admin", "Justekpwd", "logs", "fanout") {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                System.out.println("s1 handleDelivery:" + new String(body, "UTF-8"));
            }
        };
        subscriber1.start();

        Subscriber subscriber2 = new Subscriber("localhost", "admin", "Justekpwd", "logs", "fanout") {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                System.out.println("s2 handleDelivery:" + new String(body, "UTF-8"));
            }
        };
        subscriber2.start();

        Publisher publisher1 = new Publisher("localhost", "admin", "Justekpwd");
        Long index = 0l;
        while (!TaskManager.isStop()) {
            JsonObject data = new JsonObject();
            data.put("user", "user" + (++index));
            data.put("id", System.currentTimeMillis() + "");
            data.put("created_time", System.currentTimeMillis());
            data.put("timezone", "Asia/Hong_Kong");
            data.put("user_name", "Ramon Li");
            data.put("merchant_name", "7-11");
            publisher1.sendMessageToQueue("printfailed", data.toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static abstract class EndPoint {

        protected Channel channel;
        protected Connection connection;
        protected ConnectionFactory factory;

        public EndPoint(String host, String username, String password) throws IOException, TimeoutException {
            factory = new ConnectionFactory();
            factory.setHost(host);
            if (username != null) {
                factory.setUsername(username);
                if (password != null)
                    factory.setPassword(password);
            }
            reconnect();
        }

        public void reconnect() throws IOException, TimeoutException {
            connection = factory.newConnection();
            channel = connection.createChannel();
        }

        public void close() throws IOException, TimeoutException {
            this.channel.close();
            this.connection.close();
        }
    }

    public static class Publisher extends EndPoint {
        private ConcurrentHashMap<String, String> declareExchangeInfo = new ConcurrentHashMap<String, String>();
        private ConcurrentHashMap<String, String> declareQueueInfo = new ConcurrentHashMap<String, String>();
        private final String type = "fanout";
        private AMQP.BasicProperties exchange_properties = null;

        public Publisher(String host, String username, String password) throws IOException, TimeoutException {
            super(host, username, password);
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            exchange_properties = builder.expiration("20000").build();
        }

        public void sendMessageToExchange(String exchange, String text) throws IOException {
            // SerializationUtils.serialize(object)
            if (declareExchangeInfo.get(exchange) == null) {
                synchronized (declareExchangeInfo) {
                    if (declareExchangeInfo.get(exchange) == null) {
                        channel.exchangeDeclare(exchange, type);
                        declareExchangeInfo.put(exchange, type);
                    }
                }
            }
            channel.basicPublish(exchange, "", exchange_properties, text.getBytes(Charset.forName("UTF-8")));
        }

        public void sendMessageToQueue(String name, String text) throws IOException {
            // SerializationUtils.serialize(object)
            if (declareQueueInfo.get(name) == null) {
                synchronized (declareQueueInfo) {
                    if (declareQueueInfo.get(name) == null) {
                        channel.queueDeclare(name, true, false, false, null);
                        declareQueueInfo.put(name, "");
                    }
                }
            }
            channel.basicPublish("", name, MessageProperties.PERSISTENT_TEXT_PLAIN,
                    text.getBytes(Charset.forName("UTF-8")));
        }
    }

    public static class Subscriber extends EndPoint implements Consumer {
        protected String exchange;
        protected String endPointName;

        public Subscriber(String host, String username, String password, String exchange, String type)
                throws IOException, TimeoutException {
            super(host, username, password);
            this.exchange = exchange;
            channel.exchangeDeclare(exchange, type);
            endPointName = channel.queueDeclare().getQueue();
            channel.queueBind(endPointName, exchange, "");
        }

        public void start() {
            try {
                // start consuming messages. Auto acknowledge messages.
                channel.basicConsume(endPointName, false, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleConsumeOk(String consumerTag) {
            System.out.println("handleConsumeOk:" + consumerTag);
        }

        @Override
        public void handleCancelOk(String consumerTag) {
            System.out.println("handleCancelOk:" + consumerTag);
        }

        @Override
        public void handleCancel(String consumerTag) throws IOException {
            System.out.println("handleCancel:" + consumerTag);
        }

        @Override
        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
            System.out.println("handleShutdownSignal:" + consumerTag);
        }

        @Override
        public void handleRecoverOk(String consumerTag) {
            System.out.println("handleRecoverOk:" + consumerTag);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            System.out.println("handleDelivery:" + new String(body, "UTF-8"));
        }
    }

    public static class QueueConsumer extends EndPoint implements Consumer {
        protected String endPointName;

        public QueueConsumer(String host, String username, String password, String name)
                throws IOException, TimeoutException {
            super(host, username, password);
            endPointName = name;
            channel.queueDeclare(endPointName, true, false, false, null);
        }

        public void start() {
            try {
                // start consuming messages. Auto acknowledge messages.
                channel.basicConsume(endPointName, true, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleConsumeOk(String consumerTag) {
            System.out.println("handleConsumeOk:" + consumerTag);
        }

        @Override
        public void handleCancelOk(String consumerTag) {
            System.out.println("handleCancelOk:" + consumerTag);
        }

        @Override
        public void handleCancel(String consumerTag) throws IOException {
            System.out.println("handleCancel:" + consumerTag);
        }

        @Override
        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
            System.out.println("handleShutdownSignal:" + consumerTag);
        }

        @Override
        public void handleRecoverOk(String consumerTag) {
            System.out.println("handleRecoverOk:" + consumerTag);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
            System.out.println("handleDelivery:" + new String(body, "UTF-8"));
        }
    }
}
