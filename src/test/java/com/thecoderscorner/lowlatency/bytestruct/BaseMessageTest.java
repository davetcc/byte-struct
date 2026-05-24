package com.thecoderscorner.lowlatency.bytestruct;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BaseMessageTest {

    @Test
    public void testSimulatorMsgCopying() {
        byte[] simData = new byte[50];
        byte[] strData = "Hello World".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(strData, 0, simData, 0, strData.length);
        simData[strData.length] = 0;
        simData[32] = 0x22;
        simData[33] = (byte) 0x99;
        simData[34] = 0x10;
        simData[40] = (byte) 0xff;
        simData[41] = (byte) 0xfd;
        simData[42] = (byte) 0xca;
        SimulatorMsg msg = new SimulatorMsg();
        msg.copyDataFromRawData(simData);
        assertEquals("Hello World", msg.getUtf8View().toString());
        assertEquals(0x109922, msg.getLongView().asLong());
        assertEquals(0xCAFDFF, msg.getIntView().asInt());

        SimulatorMsg msg1 = new SimulatorMsg();
        msg1.copyDataFromAnother(msg);
        assertArrayEquals(msg.getUnderlyingData(), msg1.getUnderlyingData());

        SimulatorMsg msg2 = new SimulatorMsg();
        msg2.byteArrayDidChange(msg1.getUnderlyingData());
        assertArrayEquals(msg.getUnderlyingData(), msg2.getUnderlyingData());
    }

    class SimulatorMsg extends BaseMessage {
        private final Utf8View utf8View = DataViews.ofUtf8View(0, 32);
        private final LongView longView = DataViews.ofLongView(32);
        private final IntegerView intView = DataViews.ofIntView(40);

        public Utf8View getUtf8View() {
            return utf8View;
        }

        public LongView getLongView() {
            return longView;
        }

        public IntegerView getIntView() {
            return intView;
        }

        public SimulatorMsg() {
            super(44);
            addByteViewListeners(intView, longView, utf8View);
        }
    }
}