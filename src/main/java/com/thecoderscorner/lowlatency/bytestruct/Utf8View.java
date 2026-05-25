package com.thecoderscorner.lowlatency.bytestruct;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Using a UTF-8 view maps onto an existing data structure, providing a view into the data without copying. It is
 * obviously slower in CPU cycles than a direct copy, but avoids the overhead of copying the data. Use this when
 * the requirement is to avoid copying data, but may still need to manipulate it.
 *
 * If you are going to work with the underlying data a lot, consider using Utf8String instead as that takes a copy
 * of the data.
 */
public class Utf8View implements Comparable<Utf8View>, ByteViewListener {
    private final AtomicReference<byte[]> dataPoints = new AtomicReference<>();
    private final AtomicBoolean dataChanged = new AtomicBoolean(true);
    private final int[] intCodePoints;
    private final int offset;
    private final int length;

    public Utf8View(byte[] data, int offset, int length) {
        this.dataPoints.set(data);
        this.offset = offset;
        this.length = length;
        this.intCodePoints = new int[length];
    }

    public Utf8View(int offset, int length) {
        this.offset = offset;
        this.length = length;
        this.intCodePoints = new int[length];
    }

    @Override
    public void dataHasChanged(byte[] newData) {
        this.dataPoints.set(newData);
        this.dataChanged.set(true);
    }

    public String toString() {
        int[] codePoints = asCodePoints();
        return new String(codePoints, 0, strlen(codePoints));
    }

    private int strlen(int[] codePoints) {
        for(int i=0; i<codePoints.length; ++i) {
            if(codePoints[i] == 0) return i;
        }
        return codePoints.length;
    }

    public int[] asCodePoints() {
        if(!dataChanged.get()) return intCodePoints;
        var processor = ThreadLocalProcessor.getProcessorForThread();
        var capturingHandler = ThreadLocalProcessor.getCapturingHandlerForThread();
        capturingHandler.reset(intCodePoints);
        capturingHandler.receiveUtf8From(dataPoints.get(), offset, length, processor);
        return intCodePoints;
    }

    @Override
    public int compareTo(Utf8View other) {
        // check the quick exits for equality - ie, both null, or both the same reference.
        if(other == null) return 1;
        if(dataPoints.get() == null && other.dataPoints.get() == null) return 0;
        if(dataPoints.get() == other.dataPoints.get()) return 0;

        // now expand the two strings into copy buffers and compare them as UTF-8, we don't compare the bytes
        // directly, instead we compare code points. This is safer.
        int[] otherCodePoints = other.asCodePoints();
        for(int i=0; i<otherCodePoints.length; ++i) {
            if(intCodePoints[i] < otherCodePoints[i]) return -1;
            if(intCodePoints[i] > otherCodePoints[i]) return 1;
            if(intCodePoints[i] == 0) return 0;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if(o == this) return true;
        var other = ((Utf8View) o).asCodePoints();
        var ours = asCodePoints();
        for(int i=0; i<ours.length; i++) {
            if(other[i] != ours[i]) return false;
            if(other[i] == 0) return true;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        var points = asCodePoints();
        for(int i=0; i<points.length; i++) {
            hash = 31 * hash + points[i];
        }
        return hash;
    }
}
