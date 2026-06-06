package com.thecoderscorner.lowlatency.bytestruct.pool;

import com.thecoderscorner.lowlatency.bytestruct.BaseMessage;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines helper methods around the creation of message pools.
 */
public class MessagePools {
    /**
     * Suited only to single producer and consumer cases, this uses the well known
     * SCSP (Single Consumer Single Producer) circular buffer pattern. The slots are
     * all initially filled up, and then drained as messages are taken. They are returned
     * to the pool when released.
     *
     * Example:
     *    var pool = MessagePools.ofSingleConsumerProducer(10, MyMessage::new);
     *
     * Thread safety:
     * This is a thread safe pool, but only for a single producer and consumer.
     *
     * @param size
     * @return a message pool suited for SCSP cases
     * @param <T> any class extending BaseMessage
     */
    public static <T extends BaseMessage> MessagePool<T> ofSingleConsumerProducer(int size, Supplier<T> createFn) {
        return new ScspBackedMessagePool<>(size, createFn);
    }
}
