package me.kpavlov.finchly.rabbitmq;

import java.io.IOException;
import java.util.function.Function;

import com.rabbitmq.client.Channel;

import me.kpavlov.finchly.queue.QueuePublisher;

/**
 * {@link QueuePublisher} backed by a RabbitMQ {@link Channel}, publishing every message (serialized
 * via {@code serializer}) to a fixed exchange/routing key.
 *
 * @param <T> the message type
 */
public final class RabbitMqQueuePublisher<T> extends QueuePublisher<T> {

    private final Channel channel;
    private final String exchange;
    private final String routingKey;
    private final Function<T, byte[]> serializer;

    public RabbitMqQueuePublisher(
            final Channel channel,
            final String exchange,
            final String routingKey,
            final Function<T, byte[]> serializer) {
        this.channel = channel;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.serializer = serializer;
    }

    @Override
    public void publish(final T message) {
        try {
            channel.basicPublish(exchange, routingKey, null, serializer.apply(message));
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to publish message to " + exchange + "/" + routingKey, e);
        }
    }
}
