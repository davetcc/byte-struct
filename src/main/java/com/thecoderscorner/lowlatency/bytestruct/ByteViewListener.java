package com.thecoderscorner.lowlatency.bytestruct;

/**
 * This is implemented by all the view classes so that they can be notified when the underlying data has changed.
 */
public interface ByteViewListener {
    /**
     * Called when the underlying data has changed. The callee should update the underlying data
     * @param data the new data
     */
    void dataHasChanged(byte[] data);
}
