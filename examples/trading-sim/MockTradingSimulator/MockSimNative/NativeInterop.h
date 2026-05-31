//
// Created by Dave Cherry on 23/05/2026.
//

#ifndef MOCKTRADINGSIMULATOR_NATIVEINTEROP_H
#define MOCKTRADINGSIMULATOR_NATIVEINTEROP_H
#include "TradingSystem.h"

// On Windows we need to export all the functions we may use.
#ifdef _WIN32
  #ifdef MOCKSIMNATIVE_EXPORTS
    #define MOCKSIMNATIVE_API __declspec(dllexport)
  #else
    #define MOCKSIMNATIVE_API __declspec(dllimport)
  #endif
#else
  #define MOCKSIMNATIVE_API
#endif

extern "C" {
/// Initialise the trading system
/// @return true if successful, otherwise false
MOCKSIMNATIVE_API bool tradingInit();
/// Starts a static iterator, such that subsequent calls to nextStaticItem will return the next static message
/// This should only be called once as close to start up of the service as possible
/// @return true if successful, otherwise false.
MOCKSIMNATIVE_API bool startStaticIterator();
/// Iterate through each static message as discussed in the call above, each call receives an item of static and
/// it will return false when complete. This should be called from the same thread as startStaticIterator
/// @param toCopyInto an area of memory at least as large as a static message
/// @return true if a message was available, otherwise false
MOCKSIMNATIVE_API bool nextStaticItem(StaticMessage *toCopyInto);
/// This receives a price message from the trading system, it will block until a message is available, this shouls be
/// called as quickly as possible, delaying this could cause missed prices.
/// @param toCopyInto an area of memory at least as large as a price message
/// @return true if a message was available, otherwise false
MOCKSIMNATIVE_API bool acquireNextPrice(PriceMessage* toCopyInto);
}

#endif //MOCKTRADINGSIMULATOR_NATIVEINTEROP_H
