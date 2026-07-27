package me.kpavlov.finchly.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.redpanda.RedpandaContainer;

import me.kpavlov.finchly.queue.MessageAggregator;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaQueueIT {

    private static final RedpandaContainer REDPANDA =
            new RedpandaContainer("redpandadata/redpanda:v26.1.14");

    @BeforeAll
    static void startContainer() {
        REDPANDA.start();
    }

    @AfterAll
    static void stopContainer() {
        REDPANDA.stop();
    }

    private final String topic = "finchly-" + UUID.randomUUID();
    private final MessageAggregator<String> aggregator = new MessageAggregator<>();

    private KafkaProducer<String, String> producer;
    private KafkaQueueSubscriber<String, String> subscriber;

    @AfterEach
    void tearDown() {
        if (subscriber != null) {
            subscriber.stop();
        }
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void shouldDeliverPublishedMessagesToAggregatorInOrder() {
        // Given
        final var bootstrapServers = REDPANDA.getBootstrapServers();

        final var producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new KafkaProducer<>(producerProps);
        KafkaQueuePublisher<String, String> publisher = new KafkaQueuePublisher<>(producer, topic);

        final var consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "finchly-test");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        subscriber = new KafkaQueueSubscriber<>(consumer, List.of(topic), aggregator);
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

    @Test
    void shouldPreserveOrderAcrossMultiPartitionTopic() throws Exception {
        // Given: a topic with multiple partitions, proving publishAll doesn't scatter records
        // across partitions (which would make cross-partition arrival order undefined)
        final var bootstrapServers = REDPANDA.getBootstrapServers();
        final var multiPartitionTopic = "finchly-multi-" + UUID.randomUUID();

        final var adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (Admin admin = Admin.create(adminProps)) {
            admin.createTopics(List.of(new NewTopic(multiPartitionTopic, 3, (short) 1)))
                    .all()
                    .get(10, TimeUnit.SECONDS);
        }

        final var producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new KafkaProducer<>(producerProps);
        final var publisher = new KafkaQueuePublisher<String, String>(producer, multiPartitionTopic);

        final var consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "finchly-test-multi");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        subscriber = new KafkaQueueSubscriber<>(consumer, List.of(multiPartitionTopic), aggregator);
        subscriber.start();

        // When
        publisher.publishAll(List.of("alpha", "beta", "gamma", "delta", "epsilon"));

        // Then: despite 3 partitions, every record was pinned to partition 0, so order is preserved
        final var received = aggregator.awaitMessages(Duration.ofSeconds(10), 5, m -> true);
        assertThat(received).containsExactly("alpha", "beta", "gamma", "delta", "epsilon");
    }
}
