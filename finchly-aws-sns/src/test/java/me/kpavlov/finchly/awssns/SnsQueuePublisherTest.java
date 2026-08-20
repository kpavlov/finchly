package me.kpavlov.finchly.awssns;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SnsQueuePublisherTest {

    @Test
    void shouldPublishSerializedMessageToTopic() {
        final var client = mock(SnsClient.class);
        final var publisher = new SnsQueuePublisher<>(
                client, "arn:aws:sns:eu-west-1:123456789012:orders", Object::toString);

        publisher.publish(42);

        final var request = ArgumentCaptor.forClass(PublishRequest.class);
        verify(client).publish(request.capture());
        assertThat(request.getValue().topicArn()).isEqualTo("arn:aws:sns:eu-west-1:123456789012:orders");
        assertThat(request.getValue().message()).isEqualTo("42");
    }
}
