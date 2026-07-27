package me.kpavlov.finchly.queue;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.awaitility.Awaitility;

/**
 * Thread-safe, order-retaining in-memory store for messages received by a {@link QueueSubscriber}.
 *
 * <p>Tests push messages here (directly, or via a {@link QueueSubscriber}) and then assert on them
 * using {@link #find} (non-removing) or {@link #extract} (find-and-remove), optionally waiting for
 * asynchronous delivery with {@link #awaitMessage}.
 *
 * @param <T> the message type
 */
public final class MessageAggregator<T> {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_POLL_DELAY = Duration.ofMillis(50);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(100);

    private final Deque<T> messages = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Appends a message, preserving arrival order.
     */
    public void push(final T message) {
        lock.lock();
        try {
            messages.addLast(message);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the first message matching {@code predicate}, without removing it.
     */
    public Optional<T> find(final Predicate<T> predicate) {
        lock.lock();
        try {
            return messages.stream().filter(predicate).findFirst();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns all messages matching {@code predicate}, in arrival order, without removing them.
     */
    public List<T> findAll(final Predicate<T> predicate) {
        lock.lock();
        try {
            final var result = new ArrayList<T>();
            for (final var message : messages) {
                if (predicate.test(message)) {
                    result.add(message);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically finds and removes the first message matching {@code predicate}.
     */
    public Optional<T> extract(final Predicate<T> predicate) {
        lock.lock();
        try {
            final var iterator = messages.iterator();
            while (iterator.hasNext()) {
                final var message = iterator.next();
                if (predicate.test(message)) {
                    iterator.remove();
                    return Optional.of(message);
                }
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically finds and removes all messages matching {@code predicate}, in arrival order.
     */
    public List<T> extractAll(final Predicate<T> predicate) {
        lock.lock();
        try {
            final var result = new ArrayList<T>();
            final var iterator = messages.iterator();
            while (iterator.hasNext()) {
                final var message = iterator.next();
                if (predicate.test(message)) {
                    result.add(message);
                    iterator.remove();
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocks (polling, never sleeping the calling thread directly) until a message matching
     * {@code predicate} arrives, or throws {@link org.awaitility.core.ConditionTimeoutException}
     * after {@code timeout}. When {@code extract} is {@code true} the message is atomically removed;
     * otherwise it is left in place.
     */
    public T awaitMessage(final Duration timeout, final boolean extract, final Predicate<T> predicate) {
        return Awaitility.await()
                .atMost(timeout)
          .pollDelay(DEFAULT_POLL_DELAY)
          .pollInterval(DEFAULT_POLL_INTERVAL)
          .ignoreExceptions()
                .until(() -> extract ? extract(predicate) : find(predicate), Optional::isPresent)
                .orElseThrow();
    }

    /**
     * Same as {@link #awaitMessage(Duration, boolean, Predicate)} with {@code extract} defaulted to
     * {@code false} (the message is not removed).
     */
    public T awaitMessage(final Duration timeout, final Predicate<T> predicate) {
        return awaitMessage(timeout, false, predicate);
    }

    /**
     * Same as {@link #awaitMessage(Duration, boolean, Predicate)} using a default 5-second timeout.
     */
    public T awaitMessage(final boolean extract, final Predicate<T> predicate) {
        return awaitMessage(DEFAULT_TIMEOUT, extract, predicate);
    }

    /**
     * Same as {@link #awaitMessage(Duration, boolean, Predicate)} using a default 5-second timeout
     * and {@code extract} defaulted to {@code false}.
     */
    public T awaitMessage(final Predicate<T> predicate) {
        return awaitMessage(DEFAULT_TIMEOUT, false, predicate);
    }

    /**
     * Blocks until at least {@code count} messages matching {@code predicate} have arrived, or
     * throws {@link org.awaitility.core.ConditionTimeoutException} after {@code timeout}. Returns
     * the first {@code count} matches, in arrival order. When {@code extract} is {@code true} those
     * messages are atomically removed; otherwise they are left in place.
     */
    public List<T> awaitMessages(final Duration timeout, final int count, final boolean extract,
            final Predicate<T> predicate) {
        if (extract) {
            final var reserved = new AtomicReference<List<T>>();
            Awaitility.await()
                    .atMost(timeout)
                    .until(() -> tryReserve(count, predicate, reserved));
            return reserved.get();
        }
        final var matches = Awaitility.await()
                .atMost(timeout)
                .until(() -> findAll(predicate), list -> list.size() >= count);
        return matches.subList(0, count);
    }

    /**
     * Under a single lock hold, checks whether {@code count} matches are available and, only if so,
     * atomically removes and stores them into {@code reserved}. Never removes a partial batch.
     */
    private boolean tryReserve(final int count, final Predicate<T> predicate, final AtomicReference<List<T>> reserved) {
        lock.lock();
        try {
            if (messages.stream().filter(predicate).count() < count) {
                return false;
            }
            reserved.set(extractUpTo(count, predicate));
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Same as {@link #awaitMessages(Duration, int, boolean, Predicate)} with {@code extract}
     * defaulted to {@code false} (messages are not removed).
     */
    public List<T> awaitMessages(final Duration timeout, final int count, final Predicate<T> predicate) {
        return awaitMessages(timeout, count, false, predicate);
    }

    /**
     * Same as {@link #awaitMessages(Duration, int, boolean, Predicate)} using a default 5-second
     * timeout.
     */
    public List<T> awaitMessages(final int count, final boolean extract, final Predicate<T> predicate) {
        return awaitMessages(DEFAULT_TIMEOUT, count, extract, predicate);
    }

    /**
     * Same as {@link #awaitMessages(Duration, int, boolean, Predicate)} using a default 5-second
     * timeout and {@code extract} defaulted to {@code false}.
     */
    public List<T> awaitMessages(final int count, final Predicate<T> predicate) {
        return awaitMessages(DEFAULT_TIMEOUT, count, false, predicate);
    }

    private List<T> extractUpTo(final int count, final Predicate<T> predicate) {
        lock.lock();
        try {
            final var result = new ArrayList<T>();
            final var iterator = messages.iterator();
            while (iterator.hasNext() && result.size() < count) {
                final var message = iterator.next();
                if (predicate.test(message)) {
                    result.add(message);
                    iterator.remove();
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Number of messages currently stored.
     */
    public int size() {
        lock.lock();
        try {
            return messages.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all stored messages.
     */
    public void clear() {
        lock.lock();
        try {
            messages.clear();
        } finally {
            lock.unlock();
        }
    }
}
