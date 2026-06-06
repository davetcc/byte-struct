package com.thecoderscorner.lowlatency.bytestruct.pool;

import com.thecoderscorner.lowlatency.bytestruct.BaseMessage;

import java.util.function.Supplier;

/**
 * Suited only to single producer and consumer cases, this uses the well known
 * SCSP (Single Consumer Single Producer) circular buffer pattern. The slots are
 * all initially filled up, and then drained as messages are taken. They are returned
 * to the pool when released.
 *
 * Thread safety:
 * This is a thread safe pool, but only for a single producer and consumer.
 *
 * @param <T> any class extending BaseMessage
 */
public class ScspBackedMessagePool<T extends BaseMessage> implements MessagePool<T> {
    private final BaseMessage[] poolSlots;
    private volatile int poolHead = 0;
    private volatile int poolTail = 0;

    public ScspBackedMessagePool(int size, Supplier<T> createFn) {
        poolSlots = new BaseMessage[size];
        for(int i = 0; i < size - 1; i++) {
            poolSlots[i] = createFn.get();
        }
        poolTail = size - 1;
    }

    @Override
    public int capacity() {
        return poolSlots.length - 1;
    }

    @Override
    public int available() {
        return ((poolTail - poolHead) + poolSlots.length) % poolSlots.length;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T tryTake() {
        var head = poolHead;
        if(head == poolTail) {
            return null;
        }
        T message = (T) poolSlots[head];
        poolHead = (head + 1) % poolSlots.length;
        return message;
    }

    @Override
    public void release(T message) {
        var tail = poolTail;
        if(((tail + 1) % poolSlots.length) == poolHead) {
            throw new IllegalStateException("POSSIBLE CORRUPTION - Pool is full, cannot release message");
        }

        poolSlots[tail] = message;
        poolTail = (tail + 1) % poolSlots.length;
    }
}
