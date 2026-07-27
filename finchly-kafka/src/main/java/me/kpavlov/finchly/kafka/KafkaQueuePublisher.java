package me.kpavlov.finchly.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import me.kpavlov.finchly.queue.QueuePublisher;

/**
 * {@link QueuePublisher} backed by a Kafka {@link KafkaProducer}, publishing every message to a
 * single fixed topic and partition. Pinning to one partition (rather than leaving key/partition
 * unset, which would let Kafka's default partitioner scatter records across partitions) is what
 * makes {@link #publishAll} order guaranteed to be observed in the same order by a subscriber.
 *
 * @param <K> the record key type
 * @param <T> the message (record value) type
 */
public final class KafkaQueuePublisher<K, T> extends QueuePublisher<T> {

    private static final int PARTITION = 0;

    private final KafkaProducer<K, T> producer;
    private final String topic;

    public KafkaQueuePublisher(final KafkaProducer<K, T> producer, final String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void publish(final T message) {
        producer.send(new ProducerRecord<>(topic, PARTITION, null, message));
    }
}
