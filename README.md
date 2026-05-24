## ByteStruct - low latency access to C++ style struct data in Java 

Provides access to C++ style struct data in Java without requiring memory allocation in the main loop.

Licence: Apache 2.0

This class works by providing a set of views into a byte array, allowing for efficient and type-safe access to 
C++ struct data in Java, with minimal overhead and no runtime dependencies. It presently requires Java 25 because
it is envisaged that it will be mainly used along with the Foreign Memory API, which is only available in Java 22
and later.

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

Then we can use for example a native method handle with an arena and populate from that

    try(var arena = Arena.ofConfined()) {
        // you'd normally try to hold on to these for as long as possible
        var data = arena.allocate(256);
        PriceMessage priceMessage = new PriceMessage();
        
        // get our struct data into the buffer (example only)
        getPriceFromCppCodeHandle.invokeExact(data);
        MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, dest, 0, dest.length);
 
        // use the data (logging it as an example)
        log.info("Acquired price: {} - {}: {} at {}", priceMessage.getTicker().toString(),
               priceMessage.getSource().toString(), priceMessage.getTickPrice().asInt(),
               Instant.ofEpochMilli(priceMessage.getMillisEpoch().asLong()));
    }

You can also split up `IntegerView` and `LongView` into partial fields as follows:

    anIntView.booleanPartial(bit) - get the boolean (0=false, 1=true) from a bit
    anIntView.intPartial(startBit, numBits) - get the integer value from a bit range
    anIntView.enumPartial(startBit, numBits, MyEnum.class) - maps to the provided enum by ordinal
    
## Provided by TheCodersCorner.com / Dave Cherry.

Dave Cherry/TheCodersCorner.com invest a lot of time and resources into making this open source product, and I hope you'll
find useful. We strongly believe in open-source as you'll see from both [tcmenu repositories](https://github.com/tcmenu)
and my own repos (here).

Dave Cherry is a senior software engineer with over 30 years experience in C++, embedded systems and Java development. 
He is the author of tcMenu, a popular open-source menu system for embedded systems, and has contributed to many open-source
projects. As alias DaveTCC he has a user in many forums and communities.

See my profile on LinkedIn: https://www.linkedin.com/in/davejcherry/
