package com.thecoderscorner.lowlatency.bytestruct.utf8;

import java.util.function.IntConsumer;

/**
 * This class keeps hold of a capturing handler that can record the values of a UTF-8 string
 * into a codepoints array. It can also act as a somewhat general purpose listener for the processor.
 * It has a reset method to put it back into an initial state.
 */
public class CapturingHandler implements IntConsumer {
    private int[] data;
    private int currentPos = 0;

    /**
     * reset back to its initial state.
     * @param data provide the array to reset to
     */
    public void reset(int[] data) {
        this.currentPos = 0;
        this.data = data;
    }

    /**
     * Accepts an input argument and stores it in the data array if there is space.
     * @param value the input argument
     */
    @Override
    public void accept(int value) {
        if (currentPos < data.length) {
            data[currentPos++] = value;
        }
    }

    /**
     * Accepts a byte array and processes it using the Utf8TextProcessor, resetting the handler before processing.
     * @param bytes the byte array to process
     * @param offset the starting offset in the byte array
     * @param length the number of bytes to process
     * @param processor the Utf8TextProcessor to use for processing
     */
    public void receiveUtf8From(byte[] bytes, int offset, int length, Utf8TextProcessor processor) {
        reset(data);
        processor.setHandler(this);
        processor.pushBytes(bytes, offset, length);
    }
}
