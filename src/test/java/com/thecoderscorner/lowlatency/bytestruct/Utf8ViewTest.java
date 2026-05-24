package com.thecoderscorner.lowlatency.bytestruct;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class Utf8ViewTest {
    private final String UNICODE_TEST_STRING = "Світ € $";
    private final byte[] UNICODE_BYTES = UNICODE_TEST_STRING.getBytes(StandardCharsets.UTF_8);

    @Test
    void testUtf8ViewAsCodePoints() {
        Utf8View view = new Utf8View(UNICODE_BYTES, 0, UNICODE_BYTES.length);
        int[] codePoints = view.asCodePoints();
        
        int[] expected = UNICODE_TEST_STRING.codePoints().toArray();
        assertArrayEquals(expected, Arrays.copyOfRange(codePoints, 0, expected.length));
        assertEquals(0, codePoints[expected.length + 1]);
    }

    @Test
    void testUtf8ViewToString() {
        Utf8View view = new Utf8View(UNICODE_BYTES, 0, UNICODE_BYTES.length);
        assertEquals(UNICODE_TEST_STRING, view.toString());
    }

    @Test
    void testUtf8ViewCompare() {
        Utf8View view1 = new Utf8View(UNICODE_BYTES, 0, UNICODE_BYTES.length);
        Utf8View view2 = new Utf8View(UNICODE_BYTES, 0, UNICODE_BYTES.length);
        
        assertEquals(0, view1.compareTo(view2));
        
        String otherString = "Світ € A";
        byte[] otherBytes = otherString.getBytes(StandardCharsets.UTF_8);
        Utf8View view3 = new Utf8View(otherBytes, 0, otherBytes.length);
        
        assertTrue(view1.compareTo(view3) < 0);
        assertTrue(view3.compareTo(view1) > 0);
    }

    @Test
    void testUtf8ViewWithOffsetAndLength() {
        // "Світ" is at the beginning.
        // Let's take just "Світ"
        byte[] justSvitBytes = "Світ".getBytes(StandardCharsets.UTF_8);
        Utf8View view = new Utf8View(UNICODE_BYTES, 0, justSvitBytes.length);
        assertEquals("Світ", view.toString());
    }

    @Test
    void testTheCacheGetsUpdatedOnChange() {
        Utf8View view = new Utf8View(UNICODE_BYTES, 0, UNICODE_BYTES.length);
        byte[] replacementBytes = "Abcfe\0           ".getBytes(StandardCharsets.UTF_8);

        view.dataHasChanged(replacementBytes);
        assertEquals("Abcfe", view.toString());
    }
}