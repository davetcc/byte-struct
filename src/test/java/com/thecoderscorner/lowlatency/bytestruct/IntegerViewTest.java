package com.thecoderscorner.lowlatency.bytestruct;

import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class IntegerViewTest {
    enum TestEnum {
        ZERO, ONE, TWO, THREE
    }

    @Test
    void shouldReadZero() {
        byte[] data = nativeBytesFor(0x00000000);

        var view = new IntegerView(data, 0);

        assertEquals(0x00000000, view.asInt());
    }

    @Test
    void shouldReadMinusOneFromAllBitsSet() {
        byte[] data = nativeBytesFor(0xFFFFFFFF);

        var view = new IntegerView(data, 0);

        assertEquals(0xFFFFFFFF, view.asInt());
    }

    @Test
    void shouldReadMaximumPositiveInteger() {
        byte[] data = nativeBytesFor(0x7FFFFFFF);

        var view = new IntegerView(data, 0);

        assertEquals(0x7FFFFFFF, view.asInt());
    }

    @Test
    void shouldReadMinimumNegativeInteger() {
        byte[] data = nativeBytesFor(0x80000000);

        var view = new IntegerView(data, 0);

        assertEquals(0x80000000, view.asInt());
    }

    @Test
    void shouldReadHighBitSetInLeastSignificantByte() {
        byte[] data = nativeBytesFor(0x00000080);

        var view = new IntegerView(data, 0);

        assertEquals(0x00000080, view.asInt());
    }

    @Test
    void shouldReadHighBitSetInSecondByte() {
        byte[] data = nativeBytesFor(0x00008000);

        var view = new IntegerView(data, 0);

        assertEquals(0x00008000, view.asInt());
    }

    @Test
    void shouldReadHighBitSetInThirdByte() {
        byte[] data = nativeBytesFor(0x00800000);

        var view = new IntegerView(data, 0);

        assertEquals(0x00800000, view.asInt());
    }

    @Test
    void shouldReadHighBitSetInMostSignificantByte() {
        byte[] data = nativeBytesFor(0x80000000);

        var view = new IntegerView(data, 0);

        assertEquals(0x80000000, view.asInt());
    }

    @Test
    void shouldReadMixedBytePattern() {
        byte[] data = nativeBytesFor(0x12345678);

        var view = new IntegerView(data, 0);

        assertEquals(0x12345678, view.asInt());
    }

    @Test
    void shouldReadMixedBytePatternWithSignedLookingBytes() {
        byte[] data = nativeBytesFor(0x12ABCD78);

        var view = new IntegerView(data, 0);

        assertEquals(0x12ABCD78, view.asInt());
    }

    @Test
    void shouldReadFromOffsetInsideLargerArray() {
        byte[] integerBytes = nativeBytesFor(0x12345678);
        byte[] data = {
                0x55,
                0x66,
                integerBytes[0],
                integerBytes[1],
                integerBytes[2],
                integerBytes[3],
                0x77,
                0x00
        };

        var view = new IntegerView(data, 2);

        assertEquals(0x12345678, view.asInt());
    }

    @Test
    void testTheDataChangingAfterRead() {
        byte[] data = nativeBytesFor(0x12345678);

        var view = new IntegerView(data, 0);

        assertEquals(0x12345678, view.asInt());
        data[0] = (byte)0xFF;
        assertEquals(0x12345678, view.asInt()); // still cached
        view.dataHasChanged(data);
        assertEquals(0x123456ff, view.asInt()); // now updated
    }

    @Test
    void shouldReadPartialBoolInt() {
        byte[] data = nativeBytesFor(0x84111411);

        var view = new IntegerView(data, 0);

        assertTrue(view.booleanPartial(0));
        assertFalse(view.booleanPartial(1));
        assertEquals(0x84, view.intPartial(24, 8));
        assertEquals(0x84, view.intPartial(24, 8));
    }

    @Test
    void shouldReadPartialEnumInt() {
        byte[] data = nativeBytesFor(TestEnum.ONE.ordinal());

        var view = new IntegerView(data, 0);

        assertEquals(TestEnum.ONE, view.enumPartial(0, 8, TestEnum.class));
    }

    private static byte[] nativeBytesFor(int value) {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return new byte[] {
                    (byte) (value >>> 24),
                    (byte) (value >>> 16),
                    (byte) (value >>> 8),
                    (byte) value
            };
        }

        return new byte[] {
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)
        };
    }
}