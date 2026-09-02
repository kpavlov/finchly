package me.kpavlov.finchly.awssqs;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SqsQueuePublisherTest {

    @Test
    void shouldPublishSerializedMessageToQueue() {
        final var client = mock(SqsClient.class);
        final var publisher = new SqsQueuePublisher<>(
                client, "https://sqs.eu-west-1.amazonaws.com/123456789012/orders", Object::toString);

        publisher.publish(42);

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(request.capture());
        assertThat(request.getValue().queueUrl()).isEqualTo("https://sqs.eu-west-1.amazonaws.com/123456789012/orders");
        assertThat(request.getValue().messageBody()).isEqualTo("42");
    }
}
