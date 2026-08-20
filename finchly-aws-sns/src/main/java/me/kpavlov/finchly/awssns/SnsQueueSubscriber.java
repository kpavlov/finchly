package me.kpavlov.finchly.awssns;

import java.util.function.Function;

import me.kpavlov.finchly.queue.MessageAggregator;
import me.kpavlov.finchly.queue.QueueSubscriber;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * {@link QueueSubscriber} for an SNS topic whose messages are delivered to an SQS subscription.
 * The SNS subscription must have {@code RawMessageDelivery} enabled so each SQS message body is the
 * original published value rather than an SNS JSON envelope.
 *
 * @param <T> the deserialized message type
 */
public final class SnsQueueSubscriber<T> extends QueueSubscriber<T> {

    private static final int MAX_MESSAGES = 10;
    private static final int WAIT_TIME_SECONDS = 1;

    private final SqsClient client;
    private final String queueUrl;
    private final Function<String, T> deserializer;
    private final Object lifecycleLock = new Object();
    private volatile boolean running;
    private Thread pollThread;

    public SnsQueueSubscriber(
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
        synchronized (lifecycleLock) {
            if (running) {
                return;
            }
            running = true;
            pollThread = new Thread(this::pollLoop, "sns-queue-subscriber");
            pollThread.start();
        }
    }

    /** Stops the poll loop. A no-op if the subscriber is not running. */
    @Override
    public void stop() {
        synchronized (lifecycleLock) {
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
