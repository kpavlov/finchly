# 📬 Finchly Queues

## Overview

The queue testing utilities help you write integration tests against message-driven systems (Kafka,
RabbitMQ, or any other broker) without hand-rolling a thread-safe buffer and a polling loop in every
test suite. They're split across three modules:

- **`finchly-core`** — transport-agnostic abstractions:
  - `MessageAggregator<T>` — a thread-safe, order-retaining in-memory store for received messages,
    with predicate-based lookup (`find`), removal (`extract`), and blocking waits (`awaitMessage`).
  - `QueueSubscriber<T>` — abstract base for a broker-specific receiver that pushes messages into an
    aggregator.
  - `QueuePublisher<T>` — abstract base for a broker-specific sender.
- **`finchly-kafka`** — `KafkaQueueSubscriber`/`KafkaQueuePublisher`, backed by `kafka-clients`.
- **`finchly-rabbitmq`** — `RabbitMqQueueSubscriber`/`RabbitMqQueuePublisher`, backed by `amqp-client`.

A typical test wires a real subscriber to a real broker (via Testcontainers), publishes messages, and
asserts on what the aggregator received — exercising the actual wire format and delivery semantics
instead of mocking the broker away.

## Quick Start

`MessageAggregator` works standalone, with no broker at all — useful for unit-testing your own
in-process message producers/consumers:

```java
MessageAggregator<String> aggregator = new MessageAggregator<>();

aggregator.push("order-created");
aggregator.push("order-shipped");

assertThat(aggregator.findAll(m -> true))
        .containsExactly("order-created", "order-shipped");
```

Wired to a real broker, a subscriber delivers into the aggregator and a publisher sends to the same
topic/queue:

```java
MessageAggregator<String> aggregator = new MessageAggregator<>();

QueueSubscriber<String> subscriber = new KafkaQueueSubscriber<>(consumer, List.of("orders"), aggregator);
subscriber.start();

QueuePublisher<String> publisher = new KafkaQueuePublisher<>(producer, "orders");
publisher.publish("order-created");

String received = aggregator.awaitMessage(Duration.ofSeconds(10), m -> m.equals("order-created"));
```

## Features

### Predicate-based lookup: `find` and `extract`

`find` inspects the store without touching it; `extract` atomically finds and removes the first (or
all) matches — useful when a later assertion in the same test needs to distinguish "already consumed"
messages from what's still pending:

```java
Optional<String> found = aggregator.find(m -> m.startsWith("order-"));
List<String> allMatching = aggregator.findAll(m -> m.startsWith("order-"));

Optional<String> removed = aggregator.extract(m -> m.equals("order-created"));
List<String> allRemoved = aggregator.extractAll(m -> m.startsWith("order-"));
```

### Blocking waits: `awaitMessage` and `awaitMessages`

Messages usually arrive asynchronously, so polling `find` in a loop is the most common thing a test
using this library needs to do. `awaitMessage`/`awaitMessages` do that polling for you (backed by
Awaitility — no `Thread.sleep`), and throw `ConditionTimeoutException` if nothing matches in time:

```java
// Wait for exactly one matching message (default 5s timeout)
String message = aggregator.awaitMessage(m -> m.equals("order-created"));

// Wait for at least 3 matching messages, with an explicit timeout
List<String> messages = aggregator.awaitMessages(Duration.ofSeconds(10), 3, m -> true);
```

Both accept an `extract` flag (default `false`) to atomically remove the awaited message(s) once
found — handy when a test waits for a message and then wants it gone from the store so a later
`findAll` only sees what arrived afterward:

```java
// Wait for the message, then remove it
String consumed = aggregator.awaitMessage(Duration.ofSeconds(5), true, m -> m.equals("order-created"));

// Wait for 2 matches, then remove exactly those 2
List<String> consumedTwo = aggregator.awaitMessages(Duration.ofSeconds(5), 2, true, m -> true);
```

### Thread safety and ordering

All `MessageAggregator` operations are guarded by a single lock, so messages pushed concurrently
(e.g. from a broker's own poll thread while your test thread asserts) are never lost or corrupted, and
`findAll`/`extractAll` always return matches in arrival order.

## Kafka (`finchly-kafka`)

```xml
<dependency>
    <groupId>me.kpavlov.finchly</groupId>
    <artifactId>finchly-kafka</artifactId>
</dependency>
```

`KafkaQueueSubscriber<K, T>` wraps a `KafkaConsumer<K, T>` and runs a background poll loop;
`KafkaQueuePublisher<K, T>` wraps a `KafkaProducer<K, T>` and publishes to one fixed topic:

```java
KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
KafkaQueuePublisher<String, String> publisher = new KafkaQueuePublisher<>(producer, "orders");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
MessageAggregator<String> aggregator = new MessageAggregator<>();
KafkaQueueSubscriber<String, String> subscriber =
        new KafkaQueueSubscriber<>(consumer, List.of("orders"), aggregator);
subscriber.start();

publisher.publishAll(List.of("one", "two", "three"));

List<String> received = aggregator.awaitMessages(Duration.ofSeconds(10), 3, m -> true);
assertThat(received).containsExactly("one", "two", "three");

subscriber.stop(); // stops the poll loop and closes the consumer
producer.close();
```

The integration test (`finchly-kafka/src/test/java/.../KafkaQueueIT.java`) runs this exact flow
against a real broker started with Testcontainers'
[`RedpandaContainer`](https://testcontainers.com/modules/redpanda/) — Kafka-API-compatible and much
faster to boot than a full Kafka container.

## RabbitMQ (`finchly-rabbitmq`)

```xml
<dependency>
    <groupId>me.kpavlov.finchly</groupId>
    <artifactId>finchly-rabbitmq</artifactId>
</dependency>
```

`RabbitMqQueueSubscriber<T>`/`RabbitMqQueuePublisher<T>` wrap a RabbitMQ `Channel` directly, taking a
serializer/deserializer function so any message type can be used, not just `byte[]`:

```java
Channel channel = connection.createChannel();
channel.queueDeclare("orders", false, false, true, null);

RabbitMqQueuePublisher<String> publisher = new RabbitMqQueuePublisher<>(
        channel, "", "orders", message -> message.getBytes(StandardCharsets.UTF_8));

MessageAggregator<String> aggregator = new MessageAggregator<>();
RabbitMqQueueSubscriber<String> subscriber = new RabbitMqQueueSubscriber<>(
        channel, "orders", body -> new String(body, StandardCharsets.UTF_8), aggregator);
subscriber.start();

publisher.publishAll(List.of("one", "two", "three"));

List<String> received = aggregator.awaitMessages(Duration.ofSeconds(10), 3, m -> true);
assertThat(received).containsExactly("one", "two", "three");

subscriber.stop(); // cancels the consumer
```

The integration test (`finchly-rabbitmq/src/test/java/.../RabbitMqQueueIT.java`) runs this against
Testcontainers' [`RabbitMQContainer`](https://testcontainers.com/modules/rabbitmq/).

## Best Practices

### 1. Use a unique topic/queue name per test

Prevents cross-test interference when tests run in parallel or a container is reused across a test
class:

```java
String topic = "orders-" + UUID.randomUUID();
```

### 2. Always stop the subscriber

`start()` launches a background thread (Kafka) or registers a consumer callback (RabbitMQ) — leaving
it running leaks resources across tests. Stop it in `@AfterEach`:

```java
@AfterEach
void tearDown() {
    subscriber.stop();
}
```

### 3. Prefer `awaitMessage(s)` over manual polling or sleeps

A broker delivers asynchronously; asserting on `aggregator.find(...)` immediately after `publish(...)`
is a race. Use `awaitMessage`/`awaitMessages` (or `find`/`extract` only after an `await*` call has
already confirmed arrival) instead of `Thread.sleep`.

### 4. Reach for `extract` when order of consumption matters

If a test needs to assert "this message was consumed, and here's what's left," extracting removes the
ambiguity that a `find`-only assertion leaves behind.

### 5. Manage container lifecycle explicitly

The integration tests start/stop `RedpandaContainer`/`RabbitMQContainer` directly (via `@BeforeAll`/
`@AfterAll`) rather than through the Testcontainers JUnit extension, keeping the dependency footprint
to just the container modules themselves.
