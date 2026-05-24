# ByteStruct - low latency access to C++ struct data in Java 

[![Maven Build](https://github.com/davetcc/byte-struct/actions/workflows/maven.yml/badge.svg)](https://github.com/thecoderscorner/byte-struct/actions/workflows/maven.yml)

Provides access to C++ struct data in Java without requiring memory allocation in the main loop.

Licence: Apache 2.0

This library provides a `BaseMessage` class that can process data in C++ struct format, with minimal runtime allocation 
in the main loop. To access the data in the message we simply create views that look into a byte array. These views allow
for efficient and type-safe access to C++ struct data in Java, with minimal overhead and no runtime dependencies. 

It should work with Java 21 upward, but as it's generally designed to work with Project Panama foreign memory API, I'd 
imagine that it will be mainly used with versions 22 and later.

The UTF-8 parser is compliant and allocates no memory at runtime beyond initial creation, it was built originally
to support tcMenu, but has been extracted into a standalone library for general use. It has been battle tested there by
a huge number of library users, and it light enough to run on an 8-bit AVR microcontroller with 32K FLASH and 2K of RAM.
Further it will only lazy evaluate the UTF-8 encoding when the first request for the data is made.  

## What are we optimizing for?

I've spent a good few years with one foot in the finance market, and another in the embedded domain. Whenever we optimize,
we have have to ask what exactly we are trying to optimize for. For example, sometimes its preferential to have a bit higher
CPU activity but less memory churn, and that's exactly what this library is designed to do.

The general idea behind the project is that on start up the messages would get created, for a price system as an example 
they'd go into a map by ticker or other key, and then they'd be updated against the key. These classes are designed for
cases where either the objects can be pooled, and repeatedly given out, or situations such as price data where the
existing data is updated.

It would not be particularly efficient to use this library for a situation where the message objects need to be 
created frequently.

## Using C++ structs in your java code

In the simplest case, you'd create a class extending `BaseMessage` that has some views in it, and a structure size.
Once you've allocated the class, you can avoid memory allocation all together in the main processing loop.

In C++ land we have:

    @alignas(4)
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
 
        // use the data (logging it as an example)
        log.info("Acquired price: {} - {}: {} at {}", priceMessage.getTicker().toString(),
               priceMessage.getSource().toString(), priceMessage.getTickPrice().asInt(),
               Instant.ofEpochMilli(priceMessage.getMillisEpoch().asLong()));
    }

You can also split up `IntegerView` and `LongView` into partial fields as follows allowing for bit structs like in C:

    anIntView.booleanPartial(bit) - get the boolean (0=false, 1=true) from a bit
    anIntView.intPartial(startBit, numBits) - get the integer value from a bit range
    anIntView.enumPartial(startBit, numBits, MyEnum.class) - maps to the provided enum by ordinal
    
## Provided by TheCodersCorner.com / Dave Cherry.

Dave Cherry/TheCodersCorner.com invest a lot of time and resources into making this open source product, and I hope you'll
find it useful. We strongly believe in open-source as you'll see from both [tcmenu repositories](https://github.com/tcmenu)
and my own repos (here).

Dave Cherry is a senior software engineer with over 30 years experience in C++, embedded systems and Java development. 
He works in financial services IT and is also the author of tcMenu, a popular open-source menu/UI system for embedded 
systems. He has also contributed to many open-source projects. As alias DaveTCC he has a user in many forums and communities.

See my profile on LinkedIn: https://www.linkedin.com/in/davejcherry/
