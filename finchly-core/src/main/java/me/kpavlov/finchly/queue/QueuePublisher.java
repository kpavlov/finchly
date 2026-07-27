package me.kpavlov.finchly.queue;

import java.util.Collection;

/**
 * Abstract base for a broker-specific publisher. Subclasses know how to send a message to a
 * particular transport (Kafka, RabbitMQ, ...).
 *
 * @param <T> the message type
 */
public abstract class QueuePublisher<T> {

    /**
     * Publishes a single message.
     */
    public abstract void publish(T message);

    /**
     * Publishes each message in order.
     */
    public void publishAll(final Collection<T> messages) {
        messages.forEach(this::publish);
    }
}
