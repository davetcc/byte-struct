## ByteStruct - low latency access to C++ style struct data in Java 

Provides access to C++ style struct data in Java without requiring memory allocation in the main loop.

Licence: Apache 2.0

This class works by providing a set of views into a byte array, allowing for efficient and type-safe access to 
C++ struct data in Java, with minimal overhead and no runtime dependencies. It presently requires Java 25 because
it is envisaged that it will be mainly used along with the Foreign Memory API, which is only available in Java 22
and later.

Examples will shortly be provided.

Dave Cherry/TheCodersCorner.com invest a lot of time and resources into making this open source product that I hope you'll
find useful. We strongly believe in open-source as you'll see from both [tcmenu repositories](https://github.com/tcmenu)
and my own repos (here).

Dave Cherry is a senior software engineer with over 30 years experience in C++, embedded systems and Java development. 
He is the author of tcMenu, a popular open-source menu system for embedded systems, and has contributed to many open-source
projects. As alias DaveTCC he has a user in many forums and communities.
