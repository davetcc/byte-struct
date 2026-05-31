package com.thecoderscorner.lowlatency.bytestruct;

import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class LongViewTest {
    enum TestEnum {
        ZERO, ONE, TWO, THREE
    }

    @Test
    void shouldReadZero() {
        byte[] data = nativeBytesFor(0x0000000000000000L);

        var view = new LongView(data, 0);

        assertEquals(0x0000000000000000L, view.asLong());
        // test caching
        assertEquals(0x0000000000000000L, view.asLong());
    }

    @Test
    void shouldReadMinusOneFromAllBitsSet() {
        byte[] data = nativeBytesFor(0xFFFFFFFFFFFFFFFFL);

        var view = new LongView(data, 0);

        assertEquals(0xFFFFFFFFFFFFFFFFL, view.asLong());
        // test caching
        assertEquals(0xFFFFFFFFFFFFFFFFL, view.asLong());
    }

    @Test
    void testTheDataChangingAfterRead() {
        byte[] data = nativeBytesFor(0x123456789ABCDEF0L);

        var view = new LongView(data, 0);

        assertEquals(0x123456789ABCDEF0L, view.asLong());
        data[0] = (byte)0xFF;
        assertEquals(0x123456789ABCDEF0L, view.asLong()); // still cached
        view.dataHasChanged(data);
        assertEquals(0x123456789abcdeffL, view.asLong()); // now updated
    }

    @Test
    void shouldReadMaximumPositiveLong() {
        byte[] data = nativeBytesFor(0x7FFFFFFFFFFFFFFFL);

        var view = new LongView(data, 0);

        assertEquals(0x7FFFFFFFFFFFFFFFL, view.asLong());
    }

    @Test
    void shouldReadMinimumNegativeLong() {
        byte[] data = nativeBytesFor(0x8000000000000000L);

        var view = new LongView(data, 0);

        assertEquals(0x8000000000000000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInLeastSignificantByte() {
        byte[] data = nativeBytesFor(0x0000000000000080L);

        var view = new LongView(data, 0);

        assertEquals(0x0000000000000080L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInSecondByte() {
        byte[] data = nativeBytesFor(0x0000000000008000L);

        var view = new LongView(data, 0);

        assertEquals(0x0000000000008000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInThirdByte() {
        byte[] data = nativeBytesFor(0x0000000000800000L);

        var view = new LongView(data, 0);

        assertEquals(0x0000000000800000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInFourthByte() {
        byte[] data = nativeBytesFor(0x0000000080000000L);

        var view = new LongView(data, 0);

        assertEquals(0x0000000080000000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInFifthByte() {
        byte[] data = nativeBytesFor(0x0000008000000000L);

        var view = new LongView(data, 0);

        assertEquals(0x0000008000000000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInSixthByte() {
        byte[] data = nativeBytesFor(0x0000800000000000L);

        var view = new LongView(data, 0);

        assertEquals(0x0000800000000000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInSeventhByte() {
        byte[] data = nativeBytesFor(0x0080000000000000L);

        var view = new LongView(data, 0);

        assertEquals(0x0080000000000000L, view.asLong());
    }

    @Test
    void shouldReadHighBitSetInMostSignificantByte() {
        byte[] data = nativeBytesFor(0x8000000000000000L);

        var view = new LongView(data, 0);

        assertEquals(0x8000000000000000L, view.asLong());
    }

    @Test
    void shouldReadMixedBytePattern() {
        byte[] data = nativeBytesFor(0x123456789ABCDEFL);

        var view = new LongView(data, 0);

        assertEquals(0x123456789ABCDEFL, view.asLong());
    }

    @Test
    void shouldReadMixedBytePatternWithSignedLookingBytes() {
        byte[] data = nativeBytesFor(0x12ABCDEF89ABCDEFL);

        var view = new LongView(data, 0);

        assertEquals(0x12ABCDEF89ABCDEFL, view.asLong());
    }

    @Test
    void shouldReadFromOffsetInsideLargerArray() {
        byte[] longBytes = nativeBytesFor(0x123456789ABCDEFL);
        byte[] data = {
                0x55,
                0x66,
                longBytes[0],
                longBytes[1],
                longBytes[2],
                longBytes[3],
                longBytes[4],
                longBytes[5],
                longBytes[6],
                longBytes[7],
                0x77,
                0x00
        };

        var view = new LongView(data, 2);

        assertEquals(0x123456789ABCDEFL, view.asLong());
    }

    @Test
    void shouldReadPartialBoolLong() {
        byte[] data = nativeBytesFor(0x8411141112345678L);

        var view = new LongView(data, 0);

        assertFalse(view.booleanPartial(0));
        assertFalse(view.booleanPartial(1));
        assertTrue(view.booleanPartial(3));
        assertEquals(0x84, view.longPartial(56, 8));
        assertEquals(0x1234, view.longPartial(16, 16));
    }

    @Test
    void shouldReadPartialEnumLong() {
        byte[] data = nativeBytesFor(TestEnum.ONE.ordinal());

        var view = new LongView(data, 0);

        assertEquals(TestEnum.ONE, view.enumPartial(0, 8, TestEnum.class));
    }

    @Test
    void shouldReadDoubleFromBits() {
        long bits = Double.doubleToRawLongBits(12345.6789D);
        byte[] data = nativeBytesFor(bits);

        var view = new LongView(data, 0);

        assertEquals(12345.6789D, view.asDoubleFromBits());
        // test force caching
        assertEquals(12345.6789D, view.asDoubleFromBits());
    }

    @Test
    void testHashcodeAndEquals() {
        var view = new LongView(nativeBytesFor(0x12345678), 0);
        var view2 = new LongView(nativeBytesFor(0x12345678), 0);
        assertEquals(view, view2);
        assertEquals(view.hashCode(), view2.hashCode());

        var view3 = new LongView(nativeBytesFor(0x12345679), 0);
        assertNotEquals(view, view3);
        assertNotEquals(view.hashCode(), view3.hashCode());
    }

    private static byte[] nativeBytesFor(long value) {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return new byte[] {
                    (byte) (value >>> 56),
                    (byte) (value >>> 48),
                    (byte) (value >>> 40),
                    (byte) (value >>> 32),
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
                (byte) (value >>> 24),
                (byte) (value >>> 32),
                (byte) (value >>> 40),
                (byte) (value >>> 48),
                (byte) (value >>> 56)
        };
    }
}