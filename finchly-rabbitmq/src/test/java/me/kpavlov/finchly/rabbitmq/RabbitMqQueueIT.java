package me.kpavlov.finchly.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import me.kpavlov.finchly.queue.MessageAggregator;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqQueueIT {

    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-alpine");

    @BeforeAll
    static void startContainer() {
        RABBITMQ.start();
    }

    @AfterAll
    static void stopContainer() {
        RABBITMQ.stop();
    }

    private final String queue = "finchly-" + UUID.randomUUID();
    private final MessageAggregator<String> aggregator = new MessageAggregator<>();

    private Connection connection;
    private RabbitMqQueueSubscriber<String> subscriber;

    @AfterEach
    void tearDown() throws IOException {
        if (subscriber != null) {
            subscriber.stop();
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void shouldDeliverPublishedMessagesToAggregatorInOrder() throws IOException, TimeoutException {
        // Given
        final var factory = new ConnectionFactory();
        factory.setHost(RABBITMQ.getHost());
        factory.setPort(RABBITMQ.getAmqpPort());
        factory.setUsername(RABBITMQ.getAdminUsername());
        factory.setPassword(RABBITMQ.getAdminPassword());
        connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // exclusive=true: durable=false + exclusive=false ("transient_nonexcl_queues") is a
        // deprecated combination RabbitMQ 4.x rejects by default
        channel.queueDeclare(queue, false, true, true, null);

        final var publisher = new RabbitMqQueuePublisher<String>(
            channel, "", queue, message -> message.getBytes(StandardCharsets.UTF_8));
        subscriber = new RabbitMqQueueSubscriber<>(
            channel, queue, body -> new String(body, StandardCharsets.UTF_8), aggregator);
        subscriber.start();

        // When
        publisher.publishAll(List.of("one", "two", "three"));

        // Then
        final var received = aggregator.awaitMessages(Duration.ofSeconds(10), 3, m -> true);
        assertThat(received).containsExactly("one", "two", "three");

        // and: find/extract work against the same store
        assertThat(aggregator.find(m -> m.equals("two"))).contains("two");
        assertThat(aggregator.extract(m -> m.equals("two"))).contains("two");
        assertThat(aggregator.findAll(m -> true)).containsExactly("one", "three");
    }
}
