package com.thecoderscorner.lowlatency.bytestruct;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class LongView implements ByteViewListener {
    private final AtomicReference<byte[]> underlyingData = new AtomicReference<>();
    private final AtomicLong cached = new AtomicLong();
    private final AtomicBoolean dataChanged = new AtomicBoolean(true);
    private final int locationOfInt;

    public LongView(byte[] data, int locationOfInt) {
        underlyingData.set(data);
        this.locationOfInt = locationOfInt;
    }

    public LongView(int locationOfInt) {
        this.locationOfInt = locationOfInt;
    }

    public long asLong() {
        if(underlyingData.get() == null) return 0;
        if(!dataChanged.get()) return cached.get();

        dataChanged.set(false);
        if(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            cached.set((Byte.toUnsignedLong(underlyingData.get()[locationOfInt]) << 56)
                   + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 1]) << 48)
                   + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 2]) << 40)
                   + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 3]) << 32)
                   + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 4]) << 24)
                   + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 5]) << 16)
                   + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 6]) << 8)
                   + Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 7]));
        } else {
            cached.set(Byte.toUnsignedLong(underlyingData.get()[locationOfInt])
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 1]) << 8)
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 2]) << 16)
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 3]) << 24)
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 4]) << 32)
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 5]) << 40)
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 6]) << 48)
                    + (Byte.toUnsignedLong(underlyingData.get()[locationOfInt + 7]) << 56));
        }
        return cached.get();
    }

    public boolean booleanPartial(int bit) {
        return (asLong() & (1L << bit)) != 0;
    }

    public <T extends Enum> T enumPartial(int bit, int numBits, Class<T> enumClass) {
        int part = longPartial(bit, numBits);
        return enumClass.getEnumConstants()[part];
    }

    public int longPartial(int bit, int numBits) {
        return (int) ((asLong() >> bit) & ((1L << numBits) - 1));
    }

    public double asDoubleFromBits() {
        return Double.longBitsToDouble(asLong());
    }

    @Override
    public void dataHasChanged(byte[] data) {
        underlyingData.set(data);
        dataChanged.set(true);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if(o == this) return true;
        LongView other = (LongView) o;
        return other.asLong() == asLong();
    }

    @Override
    public int hashCode() {
        var x = asLong();
        x = ((x >> 30) ^ x) * 0x45d9f3b;
        x = ((x >> 27) ^ x) * 0x45d9f3b;
        x = (x >> 31) ^ x;
        return (int)x;
    }

    @Override
    public String toString() {
        return "LongView{" + " locationOfInt=" + locationOfInt + '}';
    }
}
