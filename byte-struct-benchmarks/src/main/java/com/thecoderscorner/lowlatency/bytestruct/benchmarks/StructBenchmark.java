package com.thecoderscorner.lowlatency.bytestruct.benchmarks;

import com.thecoderscorner.lowlatency.bytestruct.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public class StructBenchmark {
    private static final int NUMBER_OF_NUMERICS = 50;
    private final Random rand = new Random();
    private final List<byte[]> numericMessages = new ArrayList<>();
    private final byte[] stringMessage = new byte[48];
    private NumericMessage msg = new NumericMessage();
    private int currentPos = 0;
    private Utf8DualMessage msgStr = new Utf8DualMessage();

    @Setup
    public void setup() {
        for(int i=0; i < NUMBER_OF_NUMERICS; i++) {
            numericMessages.add(randomlyCreateNumericMessage());
        }
        byte[] dataBytes1 = "123456789012345".getBytes();
        System.arraycopy(dataBytes1, 0, stringMessage, 0, dataBytes1.length);
        byte[] dataBytes2 = "123456789012345678901234567890".getBytes();
        System.arraycopy(dataBytes2, 0, stringMessage, 16, dataBytes2.length);
    }

    private byte[] randomlyCreateNumericMessage() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(12);
        byteBuffer.asIntBuffer().put(rand.nextInt());
        byteBuffer.asLongBuffer().put(rand.nextLong());
        return byteBuffer.array();
    }

    @Benchmark
    public void testNumericFromRawCopyNonLazy(Blackhole blackhole) {
        currentPos++;
        msg.copyDataFromRawData(numericMessages.get(currentPos % NUMBER_OF_NUMERICS));
        blackhole.consume(msg.getIntegerView().asInt());
        blackhole.consume(msg.getLongView().asLong());
    }

    @Benchmark
    public void testNumericFromRawCopyLazy(Blackhole blackhole) {
        currentPos++;
        msg.copyDataFromRawData(numericMessages.get(currentPos % NUMBER_OF_NUMERICS));
        blackhole.consume(msg.getIntegerView().asInt());
    }

    @Benchmark
    public void testNumericFromRawPtrChgNonLazy(Blackhole blackhole) {
        currentPos++;
        msg.byteArrayDidChange(numericMessages.get(currentPos % NUMBER_OF_NUMERICS));
        blackhole.consume(msg.getIntegerView().asInt());
        blackhole.consume(msg.getLongView().asLong());
    }

    @Benchmark
    public void testNumericFromRawPtrChgLazy(Blackhole blackhole) {
        currentPos++;
        msg.byteArrayDidChange(numericMessages.get(currentPos % NUMBER_OF_NUMERICS));
        blackhole.consume(msg.getIntegerView().asInt());
    }

    @Benchmark
    public void testUtf8CopyNotLazyFromRaw(Blackhole blackhole) {
        msgStr.copyDataFromRawData(stringMessage);
        blackhole.consume(msgStr.getKeyText().asCodePoints());
        blackhole.consume(msgStr.getViewText().asCodePoints());
    }

    @Benchmark
    public void testUtf8CopyLazyFromRaw(Blackhole blackhole) {
        msgStr.copyDataFromRawData(stringMessage);
        blackhole.consume(msgStr.getKeyText().asCodePoints());
    }

    @Benchmark
    public void testUtf8PtrChgNotLazyFromRaw(Blackhole blackhole) {
        msgStr.byteArrayDidChange(stringMessage);
        blackhole.consume(msgStr.getKeyText().asCodePoints());
        blackhole.consume(msgStr.getViewText().asCodePoints());
    }

    @Benchmark
    public void testUtf8PtrChgLazyFromRaw(Blackhole blackhole) {
        msgStr.byteArrayDidChange(stringMessage);
        blackhole.consume(msgStr.getKeyText().asCodePoints());
    }

    static class Utf8DualMessage extends BaseMessage {
        private final Utf8View keyText = DataViews.ofUtf8View(0, 16);
        private final Utf8View viewText = DataViews.ofUtf8View(16, 32);

        public Utf8View getKeyText() {
            return keyText;
        }

        public Utf8View getViewText() {
            return viewText;
        }

        public Utf8DualMessage() {
            super(48);
            addByteViewListeners(keyText, viewText);
        }
    }

    static class NumericMessage extends BaseMessage {
        private final IntegerView integerView = DataViews.ofIntView(0);
        private final LongView longView = DataViews.ofLongView(4);

        public IntegerView getIntegerView() {
            return integerView;
        }

        public LongView getLongView() {
            return longView;
        }

        public NumericMessage() {
            super(12);
            addByteViewListeners(integerView, longView);
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(StructBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}