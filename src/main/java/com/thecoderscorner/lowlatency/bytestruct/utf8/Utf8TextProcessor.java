package com.thecoderscorner.lowlatency.bytestruct.utf8;

import java.util.function.IntConsumer;

import static com.thecoderscorner.lowlatency.bytestruct.utf8.UnicodeEncodingMode.ENCMODE_EXT_ASCII;
import static com.thecoderscorner.lowlatency.bytestruct.utf8.Utf8TextProcessor.DecoderState.*;

/**
 * Processes incoming UTF-8 encoded data and decodes it into Unicode characters via a callback mechanism. It allocates
 * no memory and uses a statemachine model to process the data. It is a reasonable complete implementation that handles
 * most common UTF-8 encoding scenarios and handles errors correctly.
 *
 * If the UTF-8 processing fails because the stream is extended ASCII, in most cases it will continue and process the
 * stream as extended ASCII. This is a common scenario when dealing with legacy systems that use extended ASCII.
 *
 * Based heavily on the tcUnicodeHelper native embedded library. This class is intentionally not thread safe and is
 * normally wrapped in a thread local to ensure thread safety.
 */
public class Utf8TextProcessor {
    /**
     * An error code indicating an invalid character encountered during UTF-8 decoding. At this point the stream will
     * reset state and start processing again.
     */
    public static final int TC_UNICODE_ERROR_CHARACTER = 0xffffffff;

    /**
     * Internal states of the decoder, not used externally
     */
    enum DecoderState {
        WAITING_BYTE_0, WAITING_BYTE_1, WAITING_BYTE_2, WAITING_BYTE_3, UTF_CHAR_FOUND
    }

    private DecoderState decoderState = DecoderState.WAITING_BYTE_0;
    private int currentUtfChar = 0;
    private int extraCharsNeeded = 0;
    private IntConsumer handler;
    private final UnicodeEncodingMode encodingMode;

    public Utf8TextProcessor(IntConsumer handler, UnicodeEncodingMode encodingMode) {
        this.handler = handler;
        this.encodingMode = encodingMode;
    }

    void setHandler(IntConsumer handler) {
        this.handler = handler;
    }

    public void pushChar(byte data) {
        if (encodingMode == ENCMODE_EXT_ASCII) {
            handler.accept((char) data);
            return;
        }

        if (data == (byte)0xFE || data == (byte)0xFF) {
            utf8Error("Invalid sequence", (byte)0);
        }

        if (decoderState == DecoderState.WAITING_BYTE_0) {
            processChar0(data);
        } else if (decoderState == WAITING_BYTE_1) {
            if ((data & 0xc0) == 0x80) {
                int uni = data & 0x3F;
                if (extraCharsNeeded == 1) {
                    currentUtfChar |= uni;
                    decoderState = UTF_CHAR_FOUND;
                } else {
                    int shiftAmount = extraCharsNeeded == 3 ? 12 : 6;
                    currentUtfChar |= (uni << shiftAmount);
                    decoderState = WAITING_BYTE_2;
                }
            } else {
                utf8Error("B1 is incorrect", data);
            }
        } else if (decoderState == WAITING_BYTE_2) {
            if ((data & 0xc0) == 0x80) {
                int uni = data & 0x3F;
                if (extraCharsNeeded == 2) {
                    decoderState = UTF_CHAR_FOUND;
                    currentUtfChar |= uni;
                } else {
                    currentUtfChar |= uni << 6;
                    decoderState = WAITING_BYTE_3;
                }
            } else {
                utf8Error("B2 is incorrect", data);
            }
        } else if (decoderState == WAITING_BYTE_3) {
            if ((data & 0xc0) == 0x80) {
                int uni = data & 0x3F;
                currentUtfChar |= uni;
                decoderState = UTF_CHAR_FOUND;
            } else {
                utf8Error("B3 is incorrect", data);
            }
        }

        // we only go ahead and register the character when a character was found.
        if (decoderState == UTF_CHAR_FOUND) {
            decoderState = WAITING_BYTE_0;
            if (couldSequenceBeSmaller()) {
                utf8Error("Sequence could be smaller", (byte)0);
            } else {
                handler.accept((char)currentUtfChar);
            }
        }
    }

    private void utf8Error(String errorFound, byte lastCode) {
        decoderState = WAITING_BYTE_0;
        extraCharsNeeded = 0;
        currentUtfChar = 0;
        handler.accept(TC_UNICODE_ERROR_CHARACTER);
        if (lastCode != 0) {
            processChar0(lastCode);
            if (decoderState == UTF_CHAR_FOUND) {
                decoderState = WAITING_BYTE_0;
                handler.accept(lastCode);
            }
        }
    }

    public void pushBytes(byte[] arr) {
        for (var by : arr)
            pushChar(by);
    }

    public void pushBytes(byte[] arr, int offset, int length) {
        var end = offset + length;
        for(int i=offset; i < end; ++i) {
            pushChar(arr[i]);
        }
    }

    void processChar0(byte data) {
        if ((data & 0x80) == 0) {
            currentUtfChar = data;
            extraCharsNeeded = 0;
            decoderState = UTF_CHAR_FOUND;
        } else if ((data & 0b11100000) == 0b11000000) {
            currentUtfChar = (data & 0x1F) << 6;
            decoderState = WAITING_BYTE_1;
            extraCharsNeeded = 1;
        } else if ((data & 0b11110000) == 0b11100000) {
            currentUtfChar = (data & 0x0F) << 12;
            decoderState = WAITING_BYTE_1;
            extraCharsNeeded = 2;
        } else if ((data & 0b11111000) == 0b11110000) {
            currentUtfChar = (data & 0x07) << 18;
            decoderState = WAITING_BYTE_1;
            extraCharsNeeded = 3;
        } else {
            throw new Utf8TextInvalidException("Invalid UTF-8 character sequence");
        }
    }

    private boolean couldSequenceBeSmaller() {
        if(currentUtfChar < 0x80) {
            return extraCharsNeeded != 0;
        } else if(currentUtfChar < 0x800 ) {
            return extraCharsNeeded > 1;
        } else if(currentUtfChar < 0x10000) {
            return extraCharsNeeded > 2;
        }

        return false;
    }

    public void reset() {
        // clear the text buffer
        extraCharsNeeded = 0;
        currentUtfChar = 0;
        decoderState = WAITING_BYTE_0;
    }

}
