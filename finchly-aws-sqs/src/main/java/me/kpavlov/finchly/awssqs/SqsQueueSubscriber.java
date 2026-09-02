package me.kpavlov.finchly.awssqs;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import me.kpavlov.finchly.queue.MessageAggregator;
import me.kpavlov.finchly.queue.QueueSubscriber;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * {@link QueueSubscriber} for an SQS queue.
 *
 * @param <T> the deserialized message type
 */
public final class SqsQueueSubscriber<T> extends QueueSubscriber<T> {

    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 1;

    private final SqsClient client;
    private final String queueUrl;
    private final Function<String, T> deserializer;
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private volatile boolean running;
    private Thread pollThread;

    public SqsQueueSubscriber(
            final SqsClient client,
            final String queueUrl,
            final Function<String, T> deserializer,
            final MessageAggregator<T> aggregator) {
        super(aggregator);
        this.client = client;
        this.queueUrl = queueUrl;
        this.deserializer = deserializer;
    }

    /** Starts a background SQS long-poll loop. A second call while running is a no-op. */
    @Override
    public void start() {
        lifecycleLock.writeLock().lock();
        try {
            if (running) {
                return;
            }
            running = true;
            pollThread = new Thread(this::pollLoop, "sqs-queue-subscriber");
            pollThread.start();
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    /** Stops the poll loop. A no-op if the subscriber is not running. */
    @Override
    public void stop() {
        lifecycleLock.writeLock().lock();
        try {
            if (!running) {
                return;
            }
            running = false;
            pollThread.interrupt();
            try {
                pollThread.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    private void pollLoop() {
        while (running) {
            final var request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(MAX_MESSAGES)
                    .waitTimeSeconds(WAIT_TIME_SECONDS)
                    .build();
            final var response = receive(request);
            if (response == null) {
                return;
            }
            for (final Message message : response.messages()) {
                if (!running) {
                    return;
                }
                consume(message);
            }
        }
    }

    private ReceiveMessageResponse receive(final ReceiveMessageRequest request) {
        try {
            return client.receiveMessage(request);
        } catch (final RuntimeException e) {
            if (!running) {
                return null;
            }
            throw e;
        }
    }

    private void consume(final Message message) {
        try {
            deliver(deserializer.apply(message.body()));
            client.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (final RuntimeException ignored) {
            // Leave a message that cannot be deserialized in SQS for its visibility timeout/DLQ.
        }
    }
}
