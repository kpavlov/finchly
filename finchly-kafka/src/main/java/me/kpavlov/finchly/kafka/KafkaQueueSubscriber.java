package me.kpavlov.finchly.kafka;

import java.time.Duration;
import java.util.Collection;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import me.kpavlov.finchly.queue.MessageAggregator;
import me.kpavlov.finchly.queue.QueueSubscriber;

/**
 * {@link QueueSubscriber} backed by a Kafka {@link Consumer} (typically a {@link KafkaConsumer}).
 * Runs a background poll loop and delivers each record's value to the aggregator.
 *
 * @param <K> the record key type
 * @param <T> the deserialized message (record value) type
 */
public final class KafkaQueueSubscriber<K, T> extends QueueSubscriber<T> {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(200);

    private final Consumer<K, T> consumer;
    private final Collection<String> topics;
    private final Object lifecycleLock = new Object();
    private Thread pollThread;
    private volatile boolean running;

    public KafkaQueueSubscriber(
            final Consumer<K, T> consumer,
            final Collection<String> topics,
            final MessageAggregator<T> aggregator) {
        super(aggregator);
        this.consumer = consumer;
        this.topics = topics;
    }

    /**
     * Subscribes and starts the poll loop. A second call while already running is a no-op — it does
     * not re-subscribe or spawn another poll thread (the underlying consumer is not thread-safe).
     */
    @Override
    public void start() {
        synchronized (lifecycleLock) {
            if (running) {
                return;
            }
            consumer.subscribe(topics);
            running = true;
            pollThread = new Thread(this::pollLoop, "kafka-queue-subscriber-" + String.join(",", topics));
            pollThread.start();
        }
    }

    /**
     * Stops the poll loop and closes the consumer. A no-op if not currently running (including when
     * called before {@link #start()}).
     */
    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            if (!running) {
                return;
            }
            running = false;
            consumer.wakeup();
            try {
                pollThread.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            consumer.close();
        }
    }

    private void pollLoop() {
        try {
            while (running) {
                final ConsumerRecords<?, T> records = consumer.poll(POLL_TIMEOUT);
                records.forEach(record -> deliver(record.value()));
            }
        } catch (final WakeupException e) {
            // triggered by stop(), safe to exit the loop
        }
    }
}
