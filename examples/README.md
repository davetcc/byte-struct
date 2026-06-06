# Examples and test harness for byte-struct

In this package are examples and test harnesses that demonstrate the library and how to use it.

```
examples/
    hello-byte-struct
    trading-sim
        MockTradingSimulator
        JavaTradingSim
        tradingsim-ui
```

## Hello Byte Struct

This is the simplest example that demonstrates how to use byte-struct. It shows how to create a byte-struct message, copy data into it, and use POJO like getters.

## Trading Sim example

This example is a very simple price conflation service. It is probably the most complete of all examples, this has a C++ producer, a Java consumer->producer, and a React UI.

It uses Panama Foreign function/memory API so requires at least Java 22 (25 is recommended).

### JavaTradingSim

This is the example you see in the YouTube video, it has three components:

* C++ producer with SPSC ringbuffer and a simple C API that Java can consume.
* Java application that reads from the C API using Project Panama Foreign Memory API. It stores the received data in byte-struct messages, and conflates them without converting to string. It serves the conflated price using Web-Flux.
* A simple React UI that subscribes for prices and presents them in a reactive grid view. It also has a reactive page for the current server statistics. 

### Building the C++ library

Firstly build the C++, I personally load the CMake project in the root directory into CLion, but it should work with anything that is CMake compliant.

It has been tested on both `macOS/XCode/ARM64` and `Windows/MSVC/x64`.

Once you've built it, the `dynlib/so/dll` will be in the cmake build directory. Note the location for later.

### Building the Java code

The java application is a small Spring Boot application, it has maven wrappers and operates pretty much like any other maven based build. It uses web-flux and by default will open a webserver on port 8080.

The only configuration change you'll most likely need to make is to `application-<profile>.properties`, where the C++ library location needs to be set in property: `trading.lib.path`.

## Building the Typescript React UI

There's a simple React UI that acts as an easy way to both see the prices updating live, and also to see the statistics. As with any React app you simply install the app and then run it.

