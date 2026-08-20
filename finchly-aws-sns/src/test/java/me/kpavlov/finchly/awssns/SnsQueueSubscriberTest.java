package me.kpavlov.finchly.awssns;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import me.kpavlov.finchly.queue.MessageAggregator;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SnsQueueSubscriberTest {

    @Test
    void shouldDeliverAndDeleteSuccessfullyDeserializedMessage() {
        final var client = mock(SqsClient.class);
        when(client.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response("hello", "receipt"))
                .thenReturn(emptyResponse());
        final var aggregator = new MessageAggregator<String>();
        final var subscriber = new SnsQueueSubscriber<>(client, "queue-url", value -> value, aggregator);

        subscriber.start();
        assertThat(aggregator.awaitMessage(Duration.ofSeconds(2), value -> value.equals("hello"))).isEqualTo("hello");
        subscriber.stop();

        verify(client, timeout(1000).atLeastOnce()).receiveMessage(any(ReceiveMessageRequest.class));
        verify(client).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldLeaveMessageWhenDeserializationFails() {
        final var client = mock(SqsClient.class);
        when(client.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response("bad", "receipt"))
                .thenReturn(emptyResponse());
        final var subscriber = new SnsQueueSubscriber<String>(client, "queue-url", value -> {
            throw new IllegalArgumentException("boom");
        }, new MessageAggregator<>());

        subscriber.start();
        verify(client, timeout(1000).atLeastOnce()).receiveMessage(any(ReceiveMessageRequest.class));
        subscriber.stop();

        verify(client, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void stopBeforeStartShouldBeNoOp() {
        final var subscriber = new SnsQueueSubscriber<>(
                mock(SqsClient.class), "queue-url", value -> value, new MessageAggregator<String>());

        assertThatCode(subscriber::stop).doesNotThrowAnyException();
    }

    private static ReceiveMessageResponse response(final String body, final String receiptHandle) {
        return ReceiveMessageResponse.builder()
                .messages(Message.builder().body(body).receiptHandle(receiptHandle).build())
                .build();
    }

    private static ReceiveMessageResponse emptyResponse() {
        return ReceiveMessageResponse.builder().messages(List.of()).build();
    }
}
