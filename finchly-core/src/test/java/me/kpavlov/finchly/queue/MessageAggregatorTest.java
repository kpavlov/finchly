package me.kpavlov.finchly.queue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageAggregatorTest {

    @Test
    void shouldRetainInsertionOrder() {
        // Given
        final var aggregator = new MessageAggregator<String>();

        // When
        aggregator.push("first");
        aggregator.push("second");
        aggregator.push("third");

        // Then
        assertThat(aggregator.findAll(m -> true)).containsExactly("first", "second", "third");
    }

    @Test
    void findShouldNotRemoveMatchedMessage() {
        // Given
        final var aggregator = new MessageAggregator<String>();
        aggregator.push("hello");

        // When
        final var found = aggregator.find(m -> m.equals("hello"));

        // Then
        assertThat(found).contains("hello");
        assertThat(aggregator.size()).isEqualTo(1);
    }

    @Test
    void extractShouldRemoveOnlyMatchedMessage() {
        // Given
        final var aggregator = new MessageAggregator<String>();
        aggregator.push("keep");
        aggregator.push("remove-me");

        // When
        final var extracted = aggregator.extract(m -> m.equals("remove-me"));

        // Then
        assertThat(extracted).contains("remove-me");
        assertThat(aggregator.findAll(m -> true)).containsExactly("keep");
    }

    @Test
    void shouldRetainAllMessagesUnderConcurrentPush() {
        // Given
        final var aggregator = new MessageAggregator<Integer>();
        final var messageCount = 200;
      try (ExecutorService executor = Executors.newFixedThreadPool(8)) {

        // When
        final var futures = IntStream.range(0, messageCount)
          .<Future<?>>mapToObj(i -> executor.submit(() -> aggregator.push(i)))
          .toList();
        for (final var future : futures) {
          try {
            future.get();
          } catch (final Exception e) {
            throw new AssertionError(e);
          }
        }
        executor.shutdown();
      }

      // Then
        assertThat(aggregator.size()).isEqualTo(messageCount);
        assertThat(aggregator.findAll(m -> true)).hasSize(messageCount).doesNotHaveDuplicates();
    }

    @Test
    void awaitMessageShouldTimeOutWhenPredicateNeverMatches() {
        // Given
        final var aggregator = new MessageAggregator<String>();

        // When / Then
        assertThatThrownBy(() -> aggregator.awaitMessage(Duration.ofMillis(200), m -> false))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    @Test
    void awaitMessagesShouldReturnRequestedCountOnceAvailable() {
        // Given
        final var aggregator = new MessageAggregator<String>();
        aggregator.push("a");
        aggregator.push("b");
        aggregator.push("c");

        // When
        final var found = aggregator.awaitMessages(Duration.ofSeconds(1), 2, m -> true);

        // Then
        assertThat(found).containsExactly("a", "b");
        assertThat(aggregator.size()).isEqualTo(3);
    }

    @Test
    void awaitMessagesShouldTimeOutWhenNotEnoughMatchesArrive() {
        // Given
        final var aggregator = new MessageAggregator<String>();
        aggregator.push("only-one");

        // When / Then
        assertThatThrownBy(() -> aggregator.awaitMessages(Duration.ofMillis(200), 2, m -> true))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    @Test
    void awaitMessageWithExtractShouldRemoveTheMessage() {
        // Given
        final var aggregator = new MessageAggregator<String>();
        aggregator.push("keep");
        aggregator.push("consume-me");

        // When
        final var awaited = aggregator.awaitMessage(Duration.ofSeconds(1), true, m -> m.equals("consume-me"));

        // Then
        assertThat(awaited).isEqualTo("consume-me");
        assertThat(aggregator.findAll(m -> true)).containsExactly("keep");
    }

    @Test
    void awaitMessagesWithExtractShouldRemoveExactlyTheAwaitedCount() {
        // Given
        final var aggregator = new MessageAggregator<String>();
        aggregator.push("a");
        aggregator.push("b");
        aggregator.push("c");

        // When
        final var awaited = aggregator.awaitMessages(Duration.ofSeconds(1), 2, true, m -> true);

        // Then
        assertThat(awaited).containsExactly("a", "b");
        assertThat(aggregator.findAll(m -> true)).containsExactly("c");
    }
}
