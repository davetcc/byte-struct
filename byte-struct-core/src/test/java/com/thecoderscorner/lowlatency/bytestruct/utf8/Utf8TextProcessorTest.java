package com.thecoderscorner.lowlatency.bytestruct.utf8;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

class Utf8TextProcessorTest {
    private final Deque<Integer> unicodeChars = new LinkedList<>();

    @Test
    void testUtf8EncoderUnicodeCodesDirect() {
        var textProcessor = new Utf8TextProcessor(unicodeChars::add, UnicodeEncodingMode.ENCMODE_UTF8);

        textProcessor.pushChar((byte)0b00100100);
        assertEquals(0x24, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.pushChar((byte) 0b11000010);
        textProcessor.pushChar((byte) 0b10100011);
        assertEquals(0xA3, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.pushChar((byte) 0b11100010);
        textProcessor.pushChar((byte) 0b10000010);
        textProcessor.pushChar((byte) 0b10101100);
        assertEquals(0x20AC, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());
    }

    @Test
    void testUtf8EncoderUnicodeBasicCase() {
        var textProcessor = new Utf8TextProcessor(unicodeChars::add, UnicodeEncodingMode.ENCMODE_UTF8);
        textProcessor.pushBytes("Hello".getBytes(StandardCharsets.UTF_8));

        assertEquals(72, getFromBufferOrError());
        assertEquals(101, getFromBufferOrError());
        assertEquals(108, getFromBufferOrError());
        assertEquals(108, getFromBufferOrError());
        assertEquals(111, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());
    }

    @Test
    void testUtf8EncoderUnicodeMulti() {
        var textProcessor = new Utf8TextProcessor(unicodeChars::add, UnicodeEncodingMode.ENCMODE_UTF8);
        textProcessor.pushBytes("Світ".getBytes(StandardCharsets.UTF_8)); // \xD0\xA1\xD0\xB2\xD1\x96\xD1\x82

        assertEquals(0x0421, getFromBufferOrError());
        assertEquals(0x0432, getFromBufferOrError());
        assertEquals(0x0456, getFromBufferOrError());
        assertEquals(0x0442, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());
    }

    @Test
    void testUtf8EncoderUnicodeOverlongNull() {
        var textProcessor = new Utf8TextProcessor(unicodeChars::add, UnicodeEncodingMode.ENCMODE_UTF8);
        textProcessor.pushChar((byte) 0xc0);
        textProcessor.pushChar((byte) 0x80);
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.reset();
        textProcessor.pushChar((byte) 0xe0);
        textProcessor.pushChar((byte) 0x80);
        textProcessor.pushChar((byte) 0x80);
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.reset();
        textProcessor.pushChar((byte) 0xf0);
        textProcessor.pushChar((byte) 0x80);
        textProcessor.pushChar((byte) 0x80);
        textProcessor.pushChar((byte) 0x80);
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());
    }

    @Test
    public void testBrokenSequenceContinueAsAscii() {
        Utf8TextProcessor textProcessor = new Utf8TextProcessor(unicodeChars::add, UnicodeEncodingMode.ENCMODE_UTF8);
        textProcessor.pushChar((byte) 0xc3);
        textProcessor.pushChar((byte) 'H');
        textProcessor.pushChar((byte) 'I');
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertEquals('H', getFromBufferOrError());
        assertEquals('I', getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.reset();
        textProcessor.pushChar((byte) 0xe1);
        textProcessor.pushChar((byte) 0x84);
        textProcessor.pushChar((byte) 'H');
        textProcessor.pushChar((byte) 'I');
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertEquals('H', getFromBufferOrError());
        assertEquals('I', getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.reset();
        textProcessor.pushChar((byte) 0xf1);
        textProcessor.pushChar((byte) 0x82);
        textProcessor.pushChar((byte) 0x81);
        textProcessor.pushChar((byte) 'A');
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertEquals('A', getFromBufferOrError());
        assertTrue(unicodeChars.isEmpty());

        textProcessor.reset();
        textProcessor.pushChar((byte) 0xf1);
        textProcessor.pushBytes("С".getBytes(StandardCharsets.UTF_8)); // Cyrillic letter
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertEquals(0x0421, getFromBufferOrError());

        textProcessor.reset();
        textProcessor.pushChar((byte) 0xf1);
        textProcessor.pushChar((byte) 0x81);
        textProcessor.pushBytes("С".getBytes(StandardCharsets.UTF_8)); // Cyrillic letter
        assertEquals(Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER, getFromBufferOrError());
        assertEquals(0x0421, getFromBufferOrError());
    }

    private int getFromBufferOrError() {
        if (!unicodeChars.isEmpty()) {
            return unicodeChars.pop();
        } else {
            return Utf8TextProcessor.TC_UNICODE_ERROR_CHARACTER;
        }
    }
}