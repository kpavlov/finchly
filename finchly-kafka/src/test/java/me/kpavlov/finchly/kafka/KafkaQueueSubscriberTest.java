package me.kpavlov.finchly.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.junit.jupiter.api.Test;

import me.kpavlov.finchly.queue.MessageAggregator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaQueueSubscriberTest {

    @Test
    void startShouldBeIdempotent() {
        // Given
        final var consumer = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
        final var subscriber =
                new KafkaQueueSubscriber<>(consumer, List.of("topic"), new MessageAggregator<String>());

        // When: started twice
        subscriber.start();
        subscriber.start();
        subscriber.stop();

        // Then: only one poll thread was ever created, so a single stop() fully closes the consumer
        assertThatThrownBy(() -> consumer.poll(java.time.Duration.ofMillis(10)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stopBeforeStartShouldBeNoOp() {
        // Given
        final var consumer = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
        final var subscriber =
                new KafkaQueueSubscriber<>(consumer, List.of("topic"), new MessageAggregator<String>());

        // When / Then
        assertThatCode(subscriber::stop).doesNotThrowAnyException();
    }
}
