package me.kpavlov.finchly.awssqs;

import java.util.function.Function;

import me.kpavlov.finchly.queue.QueuePublisher;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * {@link QueuePublisher} backed by an AWS SQS {@link SqsClient}, publishing serialized messages to
 * a fixed queue.
 *
 * @param <T> the message type
 */
public final class SqsQueuePublisher<T> extends QueuePublisher<T> {

    private final SqsClient client;
    private final String queueUrl;
    private final Function<T, String> serializer;

    public SqsQueuePublisher(
            final SqsClient client, final String queueUrl, final Function<T, String> serializer) {
        this.client = client;
        this.queueUrl = queueUrl;
        this.serializer = serializer;
    }

    @Override
    public void publish(final T message) {
        client.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(serializer.apply(message))
                .build());
    }
}
