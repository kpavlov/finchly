# Finchly Docs

Documentation index for Finchly's integration testing utilities. See the [project README](../README.md)
for build instructions and an overview of the whole library.

| Doc | Module(s) | What it covers |
|---|---|---|
| [TestEnvironment](TestEnvironment.md) | `finchly-kotlin` | Reading environment variables and `.env` files with fallback defaults. |
| [Wiremock](Wiremock.md) | `wiremock` | `BaseWiremock`, a Kotlin wrapper around WireMock for mocking HTTP services. |
| [Queues](Queues.md) | `finchly-core`, `finchly-kafka`, `finchly-rabbitmq` | `MessageAggregator`, `QueueSubscriber`/`QueuePublisher`, and Kafka/RabbitMQ integration testing with Testcontainers. |
