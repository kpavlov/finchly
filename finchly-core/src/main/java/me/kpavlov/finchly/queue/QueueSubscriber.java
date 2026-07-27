package me.kpavlov.finchly.queue;

/**
 * Abstract base for a broker-specific subscriber/receiver. Subclasses know how to listen to a
 * particular transport (Kafka, RabbitMQ, ...) and call {@link #deliver} for each message received.
 *
 * @param <T> the message type
 */
public abstract class QueueSubscriber<T> {

    protected final MessageAggregator<T> aggregator;

    protected QueueSubscriber(final MessageAggregator<T> aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * Starts listening for messages. Delivered messages are pushed to the aggregator via
     * {@link #deliver}.
     */
    public abstract void start();

    /**
     * Stops listening and releases any transport resources (connections, threads).
     */
    public abstract void stop();

    /**
     * Called by subclasses for each message received from the transport.
     */
    protected void deliver(final T message) {
        aggregator.push(message);
    }
}
