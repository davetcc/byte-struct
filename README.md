# ByteStruct - low latency access to C++ struct data in Java 

[![Maven Build](https://github.com/davetcc/byte-struct/actions/workflows/maven.yml/badge.svg)](https://github.com/thecoderscorner/byte-struct/actions/workflows/maven.yml)

## Why ByteStruct Exists

Java gives you powerful low‑level primitives (MemorySegment, VarHandle, Panama), but it still doesn’t give you the one thing real‑time systems actually need:

**A reusable, zero‑allocation struct view over binary data.**

If you’re processing high‑volume binary messages and you can’t afford object churn, GC noise, or schema compilers, ByteStruct fills the gap. It gives you a stable, long‑lived view over any binary buffer — on‑heap or off‑heap — without allocating, generating code, or depending on external tools

Use ByteStruct when:

* You need zero‑allocation access to binary fields.
* You want predictable GC behaviour in a real‑time pipeline.
* You’re working with dynamic or evolving layouts where schema compilers are a burden.
* You want a simple, explicit struct view without code generation or external tools.
* You’re decoding messages that will be stored or reused with minimal copying.

In summary, byte-struct is not the fastest library out there, and was never deigned to be. Rather, consider it as a series of components that make reading C-struct style byte arrays easier. It is ideal for market‑data feeds, telemetry, IoT sensor acquisition, IPC, off‑heap storage, and any throughput‑sensitive system.

## Performance Characteristics

ByteStruct is designed as a zero‑allocation, structured view over binary data.

It is not just a decoder: it provides POJO‑like field semantics, lazy evaluation, and stable storage for decoded values.

This means the performance numbers below include both **decode and storage costs, not just raw parsing.**

### Stated aims of the library are:

* Zero‑allocation access to binary messages
* Stable, reusable struct views that behave like POJOs
* Lazy evaluation of all fields, deferred until they are used (e.g., UTF‑8)
* Automatic invalidation when the underlying buffer changes
* Comparable and hashable UTF‑8 views (Utf8View)
* Predictable performance suitable for real‑time systems
                           
This makes ByteStruct suitable for market‑data pipelines, telemetry, IPC, and any workload where messages are read frequently and allocations must be avoided.

### UTF‑8 Storage Model

UTF‑8 fields are decoded into an internal pre-allocated int[] codepoint array lazily when first used. This is more work than a simple ASCII fast‑path decoder, providing correctness, safety, and reusability.:

* Stable, POJO‑like semantics
* Fast comparable, equality, and hashing
* No intermediate String or char[] allocations
* Full support for the entire Unicode range
* Correct handling of multi‑byte sequences using a state-machine

### Benchmark Results

Benchmarks were run using JMH on a modern laptop JVM. You can see the code behind them in the `byte-struct-benchmarks` project.

Each test includes:

* The numeric tests used two numeric fields, a long and an int.
* The UTF-8 tests used two UTF-8 fields, a 16 byte and a 32 byte.
* Both a lazy (read one of the fields) and non-lazy (read both fields)

```
Benchmark                                        Mode  Cnt    Score   Error  Units
StructBenchmark.testNumericFromRawCopyLazy       avgt    5   12.091 ± 0.100  ns/op
StructBenchmark.testNumericFromRawCopyNonLazy    avgt    5   21.141 ± 0.307  ns/op
StructBenchmark.testNumericFromRawPtrChgLazy     avgt    5   15.959 ± 0.325  ns/op
StructBenchmark.testNumericFromRawPtrChgNonLazy  avgt    5   23.172 ± 0.202  ns/op
StructBenchmark.testUtf8CopyLazyFromRaw          avgt    5   49.344 ± 0.478  ns/op
StructBenchmark.testUtf8CopyNotLazyFromRaw       avgt    5  115.199 ± 3.001  ns/op
StructBenchmark.testUtf8PtrChgLazyFromRaw        avgt    5   51.315 ± 0.236  ns/op
StructBenchmark.testUtf8PtrChgNotLazyFromRaw     avgt    5  118.652 ± 5.311  ns/op
```

## Examples, benchmarks, and test harness

The [trading sim and other examples](examples/README.md) are packaged within this repository. The example used in the YouTube introduction/demonstration and benchmarks are directly buildable and available there.

## How it is used

This library provides a `BaseMessage` class that can process data in C++ struct format, with minimal runtime allocation 
in the main loop. To access the data in the message we simply create views that look into a byte array. These views allow
for efficient and type-safe access to C++ struct data in Java, with minimal overhead and no runtime dependencies.

It should work with Java 21 upward, but as it's generally designed to work with Project Panama foreign memory API, I'd 
imagine that it will be mainly used with versions 22 and later.

The UTF-8 parser is compliant and allocates no memory at runtime beyond initial creation, it was built originally
to support tcMenu, but has been extracted into a standalone library for general use. It has been battle tested there by
a huge number of library users, and it light enough to run on an 8-bit AVR microcontroller with 32K FLASH and 2K of RAM.
Further it will only lazy evaluate the UTF-8 encoding when the first request for the data is made.

`Utf8View` also properly implements `hashCode`, `equals` and `Comparable` meaning you can use as a key in any containers from Utf8View without risking memory allocation. For example:

    ConcurrentMap myMap = new ConcurrentHashMap<Utf8View, PriceMessage>();

There is a [demonstration project repository](https://github.com/davetcc/MockTradingSimulator) that doubles as my test-harness.

## Using C++ structs in your java code

In the simplest case, you'd create a class extending `BaseMessage` that has some views in it, and a structure size.
Once you've allocated the class, you can avoid memory allocation all together in the main processing loop.

In C++ land we have:

    struct PriceMessage {
        const char ticker[32]; //0..31  32
        const char symbol[16]; //32..48 16
        uint64_t millisEpoch;  //48..56 8
        uint32_t priceTicks;   //56..60 4
    };  

In Java land we create:

    byte[] myData = // some data in fixed struct format.

    class PriceMessage extends BaseMessage {
        private final Utf8View ticker = new Utf8View(0, 32);
        private final Utf8View source = new Utf8View(32, 16);
        private final LongView millisEpoch = new LongView(48);
        private final IntegerView tickPrice = new IntegerView(64);

        public PriceMessage() {
            super(68);
            addByteViewListeners(ticker, source, millisEpoch, tickPrice);
        }
    }

Here as an example, we use a native method handle with an arena and populate our message from that:

    try(var arena = Arena.ofConfined()) {
        // you'd normally try to hold on to these for as long as possible
        var data = arena.allocate(256);
        PriceMessage priceMessage = new PriceMessage();
        
        // get our struct data into the buffer (example only)
        getPriceFromCppCodeHandle.invokeExact(data);
        MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, dest, 0, dest.length);
 
        // use the data (logging it as an example, obviously don't do this in production)
        log.info("Acquired price: {} - {}: {} at {}", priceMessage.getTicker().toString(),
               priceMessage.getSource().toString(), priceMessage.getTickPrice().asInt(),
               Instant.ofEpochMilli(priceMessage.getMillisEpoch().asLong()));
    }

You can also split up `IntegerView` and `LongView` into partial fields as follows allowing for bit structs like in C:

In C++ your struct looks like:

```
struct TradingInformation {
    uint32_t isTradable: 1;      //0
    uint32_t isPreMarket: 1;     //1
    uint32_t flaggedAsBlocked: 1;//2
    uint32_t tradableVenue : 6;  //3-8
    uint32_t productType : 6;    //9-14
    uint32_t ticksPerPoint : 16; //15-31
};

class StaticMessage {
private:
    char ticker[32];
    TradingInformation tradeInfo;
public:
    StaticMessage() = default;
    StaticMessage(const StaticMessage& other) = default;
    StaticMessage& operator=(const StaticMessage& other) = default;
    StaticMessage(const char* ticker, TradingInformation tradeInfo)
        : ticker(), tradeInfo(tradeInfo) {
        strncpy(this->ticker, ticker, sizeof(this->ticker) - 1);
        this->ticker[sizeof(this->ticker) - 1] = '\0';
    }

    [[nodiscard]] const char* getTicker() const { return ticker; }
    [[nodiscard]] uint32_t getTicksPerPoint() const { return tradeInfo.ticksPerPoint; }
    [[nodiscard]] ProductType getProductType() const { return static_cast<ProductType>(tradeInfo.productType); }
    [[nodiscard]] TradableVenue getTradableVenue() const { return static_cast<TradableVenue>(tradeInfo.tradableVenue); }
    [[nodiscard]] bool isTradable() const { return static_cast<ProductType>(tradeInfo.isTradable); }
    [[nodiscard]] bool isPreMarket() const { return static_cast<ProductType>(tradeInfo.isPreMarket); }
    [[nodiscard]] bool isFlaggedAsBlocked() const { return static_cast<ProductType>(tradeInfo.flaggedAsBlocked); }
};
```

In Java your class looks like:

```
public class StaticMessage extends BaseMessage {
    @Getter
    private final Utf8View ticker = DataViews.ofUtf8View(0, 32);
    private final IntegerView tradeInfo = DataViews.ofIntView(32);

    public StaticMessage() {
        super(36);
        addByteViewListeners(ticker, tradeInfo);
    }

    public boolean isTradeable() { return tradeInfo.booleanPartial(0); }
    public boolean isPreMarket() { return tradeInfo.booleanPartial(1); }
    public boolean isBlocked() { return tradeInfo.booleanPartial(2); }
    public TradeableVenue getTradeableVenue() { return tradeInfo.enumPartial(3, 6, TradeableVenue.class); }
    public ProductType getProductType() { return tradeInfo.enumPartial(9, 6, ProductType.class); }
    public int getTicksPerPoint() { return tradeInfo.intPartial(15, 16); }
}
```

## Using the UTF-8 Unicode encoder standalone

There is a stream based UTF-8 encoder that can be used standalone. This is useful if you want to decode strings into
an int array without using the `Message` class. The encoder can be used as below:

    // create a text processor that can process UTF-8 encoded text, 
    // it is not thread safe, create one per thread.
    var textProcessor = new Utf8TextProcessor(anIntConsumer, UnicodeEncodingMode.ENCMODE_UTF8);
    // Important, reset the processor to start processing a new string
    textProcessor.reset();
    // push a UTF-8 encoded character, this will decode the character and call the consumer with the unicode value
    textProcessor.pushChar((byte) 0xf1);
    textProcessor.pushChar((byte) 0x81);

**When to use this?** Either in systems that require reduced allocation or when dealing with C++ structs.

In regular systems where memory allocation is not an issue do not use this class. For example, in tcMenu designer.
I don't even use these classes myself because it is not low latency, it is high throughput; and therefore does not need this
extra complexity.

## ByteStruct is provided by Dave Cherry / TheCodersCorner.com.

I invest a significant amount of time and energy into building open‑source libraries that are used in production by many
companies and hobbyists alike. I hope you find this project useful. You can see the wider ecosystem in both the 
[tcMenu repositories](https://github.com/tcmenu) and my [own projects](https://github.com/davetcc) here on GitHub.

### About the author

Dave Cherry is a senior software engineer with over 30 years of experience across C++, embedded systems, and Java.
He works in financial services technology and is the creator of tcMenu, a widely‑used open‑source menu/UI framework 
for embedded devices. He has contributed to numerous open‑source projects over the years and is active in many technical
communities under the alias **DaveTCC**.

See my profile on LinkedIn: https://www.linkedin.com/in/davejcherry/
