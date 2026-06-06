package com.thecoderscorner.lowlatency.bytestruct.pool;

import com.thecoderscorner.lowlatency.bytestruct.BaseMessage;

/// A message pool interface that provides a mechanism for managing and recycling messages.
/// Once a message is taken from a pool, you should expect the data bytes are full of garbage.
///
/// The process is as follows:
///
/// 1. take a message from the pool
/// 2. populate its data using one of the provided methods
/// 3. store and use the message
/// 4. release the message back to the pool
///
public interface MessagePool<T extends BaseMessage> {
    /**
     * gets the available capacity of the pool
     * @return capacity of the pool
     */
    int capacity();

    /**
     * gets the available items in the pool
     * @return available items the pool
     */
    int available();

    /**
     * Try to take an item off the pool, if none available return null
     * @return message or null if none available
     */
    T tryTake();

    /**
     * Try to take an item off the pool, if none available throw exception
     * @return the message object
     * @throws IllegalStateException if no message available
     */
    default T takeOrThrow() {
        T t = tryTake();
        if(t == null) {
            throw new IllegalStateException("No messages available in pool");
        }
        return t;
    }

    /**
     * Gives a message back to the pool. For performance there are no safety checks.
     * You can occasionally run checkDuplicates() to ensure no duplicates are in the pool.
     * @param message the message to release
     * @throws IllegalStateException if message already in pool
     */
    void release(T message);
}
