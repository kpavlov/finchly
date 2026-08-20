package me.kpavlov.finchly.awssns;

import java.util.function.Function;

import me.kpavlov.finchly.queue.QueuePublisher;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * {@link QueuePublisher} backed by an AWS SNS {@link SnsClient}, publishing serialized messages to
 * a fixed topic.
 *
 * @param <T> the message type
 */
public final class SnsQueuePublisher<T> extends QueuePublisher<T> {

    private final SnsClient client;
    private final String topicArn;
    private final Function<T, String> serializer;

    public SnsQueuePublisher(
            final SnsClient client, final String topicArn, final Function<T, String> serializer) {
        this.client = client;
        this.topicArn = topicArn;
        this.serializer = serializer;
    }

    @Override
    public void publish(final T message) {
        client.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(serializer.apply(message))
                .build());
    }
}
