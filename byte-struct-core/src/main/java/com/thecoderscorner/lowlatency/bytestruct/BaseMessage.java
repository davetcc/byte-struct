package com.thecoderscorner.lowlatency.bytestruct;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for messages that can be viewed as a byte array. Provides methods for adding listeners and copying data.
 * All byte view messages should extend this class.
 */
public class BaseMessage {
    private final List<ByteViewListener> listeners = new CopyOnWriteArrayList<>();
    protected final AtomicReference<byte[]> data;

    public BaseMessage(int dataLen) {
        this.data = new AtomicReference<>(new byte[dataLen]);
    }

    /**
     * Add one or more ByteViewListeners to the message. These will be notified when the underlying data changes.
     * @param listeners the listeners to add
     */
    public void addByteViewListeners(ByteViewListener... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
        existingArrayChanged();
    }

    /**
     * Copy over the byte array from another BaseMessage instance. It must be exactly the same class type and size.
     * @param other the other BaseMessage instance to copy from
     */
    public void copyDataFromAnother(BaseMessage other) {
        if(!other.getClass().equals(getClass())) {
            throw new IllegalArgumentException("copy attempt between " + getClass() + " and " + other.getClass());
        }

        if(other.getUnderlyingData().length != data.get().length) {
            throw new IllegalArgumentException("Array size mismatch, not copying");
        }

        System.arraycopy(other.getUnderlyingData(), 0, data.get(), 0, data.get().length);
        existingArrayChanged();
    }

    /**
     * Get the underlying data array that the views are using.
     * @return the underlying data
     */
    public byte[] getUnderlyingData() {
        return data.get();
    }

    /**
     * This indicates that the original underlying data has changed, and listeners should be notified.
     */
    public void existingArrayChanged() {
        for(int i=0; i<listeners.size(); i++) {
            listeners.get(i).dataHasChanged(data.get());
        }
    }

    /**
     * The underlying byte array will be replaced with the newBytes array, listeners will be notified of the change.
     * @param newBytes must be of equivalent length to the original, or this call will fail with an exception
     */
    public void byteArrayDidChange(byte[] newBytes) {
        if(newBytes.length != data.get().length) {
            throw new IllegalArgumentException("Array size mismatch, not copying");
        }

        System.arraycopy(newBytes, 0, data.get(), 0, data.get().length);
        for(int i=0; i<listeners.size(); i++) {
            listeners.get(i).dataHasChanged(newBytes);
        }
    }

    /**
     * Copy the data from a raw byte array, for example to take
     * the value from a memory segment, for safe longer term storage.
     * @param d the raw array, must be at least as long as ours.
     */
    public void copyDataFromRawData(byte[] d) {
        if(d.length < data.get().length) {
            throw new IllegalArgumentException("Array size too small, not copying");
        }
        System.arraycopy(d, 0, data.get(), 0, data.get().length);
        existingArrayChanged();
    }


    /**
     * Completely detach this object from any listeners and clear the underlying data, makes GC's life much easier.
     */
    public void clear() {
        listeners.clear();
        data.set(null);
    }
}
