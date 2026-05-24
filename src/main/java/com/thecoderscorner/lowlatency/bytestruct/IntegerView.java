package com.thecoderscorner.lowlatency.bytestruct;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class IntegerView implements ByteViewListener {
    private final AtomicReference<byte[]> underlyingData = new AtomicReference<>();
    private final AtomicInteger cached = new AtomicInteger();
    private final AtomicBoolean dataChanged = new AtomicBoolean(true);
    private final int locationOfInt;

    public IntegerView(byte[] data, int locationOfInt) {
        underlyingData.set(data);
        this.locationOfInt = locationOfInt;
    }

    public IntegerView(int locationOfInt) {
        this.locationOfInt = locationOfInt;
    }

    public int asInt() {
        if(underlyingData.get() == null) return 0;
        if(!dataChanged.get()) return cached.get();

        dataChanged.set(false);
        if(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            cached.set((Byte.toUnsignedInt(underlyingData.get()[locationOfInt]) << 24)
                   | (Byte.toUnsignedInt(underlyingData.get()[locationOfInt + 1]) << 16)
                   | (Byte.toUnsignedInt(underlyingData.get()[locationOfInt + 2]) << 8)
                   | Byte.toUnsignedInt(underlyingData.get()[locationOfInt + 3]));
        }
        else {
            cached.set(Byte.toUnsignedInt(underlyingData.get()[locationOfInt])
                    | (Byte.toUnsignedInt(underlyingData.get()[locationOfInt + 1]) << 8)
                    | (Byte.toUnsignedInt(underlyingData.get()[locationOfInt + 2]) << 16)
                    | (Byte.toUnsignedInt(underlyingData.get()[locationOfInt + 3]) << 24));
        }
        return cached.get();
    }

    public boolean booleanPartial(int bit) {
        return (asInt() & (1 << bit)) != 0;
    }

    public <T extends Enum> T enumPartial(int bit, int numBits, Class<T> enumClass) {
        int part = intPartial(bit, numBits);
        return enumClass.getEnumConstants()[part];
    }

    public int intPartial(int bit, int numBits) {
        return (asInt() >> bit) & ((1 << numBits) - 1);
    }

    public float asFloatFromBits() {
        return Float.intBitsToFloat(asInt());
    }

    @Override
    public void dataHasChanged(byte[] data) {
        underlyingData.set(data);
        dataChanged.set(true);
    }
}
