# Mock Trading System

This is a mock implementation of a trading system that I use to test my `byte-struct` library. This library is used to
parse struct based C++ messages while avoiding allocation.

The flow of the application is:

```
C++ generate price ->
    C++ Circular buffer -> 
        Panama Java price acquire -> 
            Conflate Prices -> 
                Price Distributor -> 
                    Web Flux -> 
                        React Web UI
```

## Where it has been tested

* It has been compiled against macOS, ARM64V8 AKA Apple Silicon using XCode Tools.
* It has been compiled on Windows using MSVC 26 Toolchain.
* Soon I will ensure it also builds and runs on Linux gcc x86_64.

## Performance

I've tested the performance of the application and found that it can handle a high volume of price updates with 
minimal overhead, and no allocation in the critical flows.

License: Apache 2.0

## This project's purpose

1. It allows me to test `byte-struct` functionality and performance.
2. It serves as a learning tool for trading system development.
3. It is a demonstration of my skills, showing that I can build a low-latency, high-throughput trading system. This was built in about 4 days so it has some unhandled edge cases.

## How to run this?

1. Clone the repository: `git clone https://github.com/davetcc/mock-trading-simulator.git`
2. Navigate to the project directory: `cd mock-trading-simulator`
3. Clone the `byte-struct` repository into the root directory: `git clone https://github.com/davetcc/byte-struct.git`
4. Build the `byte-struct` project: `cd byte-struct && mvn clean install`
5. Load the C++ project `CMakeLists.txt` file in the root directory into an IDE and built.
6. Open the Java project directory `JavaTradingSim` in an IDE and build.



