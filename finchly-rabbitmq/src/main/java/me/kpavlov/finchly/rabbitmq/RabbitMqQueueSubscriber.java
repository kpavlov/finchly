package me.kpavlov.finchly.rabbitmq;

import java.io.IOException;
import java.util.function.Function;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import me.kpavlov.finchly.queue.MessageAggregator;
import me.kpavlov.finchly.queue.QueueSubscriber;

/**
 * {@link QueueSubscriber} backed by a RabbitMQ {@link Channel}, consuming from a single queue and
 * delivering each message body (deserialized via {@code deserializer}) to the aggregator.
 *
 * @param <T> the deserialized message type
 */
public final class RabbitMqQueueSubscriber<T> extends QueueSubscriber<T> {

    private final Channel channel;
    private final String queue;
    private final Function<byte[], T> deserializer;
    private final Object lifecycleLock = new Object();
    private String consumerTag;

    public RabbitMqQueueSubscriber(
            final Channel channel,
            final String queue,
            final Function<byte[], T> deserializer,
            final MessageAggregator<T> aggregator) {
        super(aggregator);
        this.channel = channel;
        this.queue = queue;
        this.deserializer = deserializer;
    }

    /**
     * Starts consuming, acknowledging each message only after it has been deserialized and
     * delivered to the aggregator; a message that fails deserialization is negatively acknowledged
     * (not requeued, to avoid a poison-message redelivery loop) rather than silently acknowledged
     * and lost. A second call while already consuming is a no-op.
     */
    @Override
    public void start() {
        synchronized (lifecycleLock) {
            if (consumerTag != null) {
                return;
            }
            final DeliverCallback onMessage = (tag, delivery) -> {
                try {
                    deliver(deserializer.apply(delivery.getBody()));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (final RuntimeException e) {
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                }
            };
            final CancelCallback onCancel = tag -> { };
            try {
                consumerTag = channel.basicConsume(queue, false, onMessage, onCancel);
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to start consuming from queue " + queue, e);
            }
        }
    }

    /**
     * Cancels the consumer. A no-op if not currently consuming (including when called before
     * {@link #start()}).
     */
    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            if (consumerTag == null) {
                return;
            }
            try {
                channel.basicCancel(consumerTag);
                consumerTag = null;
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to cancel consumer " + consumerTag, e);
            }
        }
    }
}
