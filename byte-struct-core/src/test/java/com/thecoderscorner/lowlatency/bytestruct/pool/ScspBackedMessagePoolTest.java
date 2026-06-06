package com.thecoderscorner.lowlatency.bytestruct.pool;

import com.thecoderscorner.lowlatency.bytestruct.BaseMessage;
import com.thecoderscorner.lowlatency.bytestruct.DataViews;
import com.thecoderscorner.lowlatency.bytestruct.IntegerView;
import com.thecoderscorner.lowlatency.bytestruct.LongView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScspBackedMessagePoolTest {

    private MessagePool<MyMessage> pool;

    @BeforeEach
    void setUp() {
        pool = MessagePools.ofSingleConsumerProducer(10, MyMessage::new);
        assertNotNull(pool);
    }

    @Test
    void testSingleConsumerProducerPool() {
        assertEquals(9, pool.capacity());
        assertThat(pool.capacity()).isEqualTo(pool.available());
        MyMessage taken = pool.tryTake();
        assertThat(taken).isInstanceOf(MyMessage.class);
        assertThat(pool.capacity() - 1).isEqualTo(pool.available());
        pool.release(taken);
        assertThat(pool.capacity()).isEqualTo(pool.available());
    }

    @Test
    void testConsumingEverythingDownToNull() {
        for (int i = 0; i < pool.capacity(); i++) {
            MyMessage taken = pool.tryTake();
            assertThat(taken).isInstanceOf(MyMessage.class);
        }
        MyMessage taken = pool.tryTake();
        assertThat(taken).isNull();
    }

    @Test
    void inlineTestOfConsumingAndProducing() {
        for(int i=0; i<500; ++i) {
            MyMessage taken = pool.tryTake();
            assertThat(taken).isInstanceOf(MyMessage.class);
            assertThat(pool.capacity() - 1).isEqualTo(pool.available());
            pool.release(taken);
            assertThat(pool.capacity()).isEqualTo(pool.available());
        }
    }

    @Test
    void threadedTestOfConsumingAndProducing() throws InterruptedException {
        Executor executor = Executors.newSingleThreadExecutor();
        AtomicInteger received = new AtomicInteger(0);
        while(received.get() < 10000) {
            var msg = pool.tryTake();
            if(msg == null) {
                Thread.yield();
            } else {
                executor.execute(() -> {
                    received.incrementAndGet();
                    pool.release(msg);
                });
            }
        }
    }

    class MyMessage extends BaseMessage {
        private final IntegerView value1 = DataViews.ofIntView(0);
        private final LongView value2 = DataViews.ofLongView(4);
        public MyMessage() {
            super(12);
            addByteViewListeners(value1, value2);
        }

        public int getValue1() {
            return value1.asInt();
        }

        public long getValue2() {
            return value2.asLong();
        }
    }
}