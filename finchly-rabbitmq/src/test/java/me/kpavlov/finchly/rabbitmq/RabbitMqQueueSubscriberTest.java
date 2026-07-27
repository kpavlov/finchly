package me.kpavlov.finchly.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import me.kpavlov.finchly.queue.MessageAggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitMqQueueSubscriberTest {

    @Test
    void startShouldBeIdempotent() throws IOException {
        // Given
        final var channel = mock(Channel.class);
        when(channel.basicConsume(anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class)))
                .thenReturn("tag-1");
        final var subscriber = new RabbitMqQueueSubscriber<>(
                channel, "queue", body -> new String(body, StandardCharsets.UTF_8), new MessageAggregator<String>());

        // When: started twice
        subscriber.start();
        subscriber.start();

        // Then: only consumed once
        verify(channel, times(1))
                .basicConsume(anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class));
    }

    @Test
    void stopBeforeStartShouldBeNoOp() throws IOException {
        // Given
        final var channel = mock(Channel.class);
        final var subscriber = new RabbitMqQueueSubscriber<>(
                channel, "queue", body -> new String(body, StandardCharsets.UTF_8), new MessageAggregator<String>());

        // When / Then
        assertThatCode(subscriber::stop).doesNotThrowAnyException();
        verify(channel, never()).basicCancel(anyString());
    }

    @Test
    void successfulDeliveryShouldAckAndPushToAggregator() throws IOException {
        // Given
        final var channel = mock(Channel.class);
        final var callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(anyString(), anyBoolean(), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("tag-1");
        final var aggregator = new MessageAggregator<String>();
        final var subscriber = new RabbitMqQueueSubscriber<>(
                channel, "queue", body -> new String(body, StandardCharsets.UTF_8), aggregator);
        subscriber.start();

        // When
        callbackCaptor.getValue().handle("tag-1", delivery(42L, "hello"));

        // Then
        assertThat(aggregator.find(m -> m.equals("hello"))).contains("hello");
        verify(channel).basicAck(42L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void failingDeserializationShouldNackAndNotPushToAggregator() throws IOException {
        // Given
        final var channel = mock(Channel.class);
        final var callbackCaptor = ArgumentCaptor.forClass(DeliverCallback.class);
        when(channel.basicConsume(anyString(), anyBoolean(), callbackCaptor.capture(), any(CancelCallback.class)))
                .thenReturn("tag-1");
        final var aggregator = new MessageAggregator<String>();
        final var subscriber = new RabbitMqQueueSubscriber<>(channel, "queue", body -> {
            throw new IllegalArgumentException("boom");
        }, aggregator);
        subscriber.start();

        // When
        callbackCaptor.getValue().handle("tag-1", delivery(7L, "bad"));

        // Then
        assertThat(aggregator.size()).isZero();
        verify(channel).basicNack(7L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private static Delivery delivery(final long deliveryTag, final String body) {
        final var envelope = new Envelope(deliveryTag, false, "", "queue");
        return new Delivery(envelope, null, body.getBytes(StandardCharsets.UTF_8));
    }
}
