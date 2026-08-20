package me.kpavlov.finchly.awssns;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import me.kpavlov.finchly.queue.MessageAggregator;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import static org.assertj.core.api.Assertions.assertThat;

class SnsQueueIT {

    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.7.0"));

    @BeforeAll
    static void startContainer() {
        LOCALSTACK.start();
    }

    @AfterAll
    static void stopContainer() {
        LOCALSTACK.stop();
    }

    @Test
    void shouldDeliverPublishedMessagesThroughSqsSubscription() {
        final var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        final var region = Region.of(LOCALSTACK.getRegion());
        try (var sns = SnsClient.builder()
                        .endpointOverride(LOCALSTACK.getEndpoint())
                        .credentialsProvider(credentials)
                        .region(region)
                        .build();
                var sqs = SqsClient.builder()
                        .endpointOverride(LOCALSTACK.getEndpoint())
                        .credentialsProvider(credentials)
                        .region(region)
                        .build()) {
            final var suffix = UUID.randomUUID().toString();
            final var topicArn = sns.createTopic(CreateTopicRequest.builder()
                            .name("finchly-" + suffix)
                            .build())
                    .topicArn();
            final var queueUrl = sqs.createQueue(CreateQueueRequest.builder()
                            .queueName("finchly-" + suffix)
                            .build())
                    .queueUrl();
            final var queueArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build())
                    .attributes()
                    .get(QueueAttributeName.QUEUE_ARN);
            sns.subscribe(SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("sqs")
                    .endpoint(queueArn)
                    .attributes(Map.of("RawMessageDelivery", "true"))
                    .build());

            final var aggregator = new MessageAggregator<String>();
            final var subscriber = new SnsQueueSubscriber<>(sqs, queueUrl, value -> value, aggregator);
            final var publisher = new SnsQueuePublisher<String>(sns, topicArn, value -> value);
            subscriber.start();
            try {
                publisher.publishAll(List.of("one", "two", "three"));

                assertThat(aggregator.awaitMessages(Duration.ofSeconds(10), 3, value -> true))
                        .containsExactlyInAnyOrder("one", "two", "three");
            } finally {
                subscriber.stop();
            }
        }
    }
}
