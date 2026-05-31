//
// Created by Dave Cherry on 23/05/2026.
//

#include "NativeInterop.h"

std::atomic<TradingSystem*> tradingSystem = nullptr;

std::map<std::string_view, StaticMessage>::const_iterator staticIterator;

extern "C" {

/// Called from Java back into the C++ domain to receive the next available price. The price message memory provided
/// will be copied into the provided pointer. The function returns true if a price was available, false otherwise.
/// @param toCopyInto the structure to copy into, must exactly match the PriceMessage struct.
/// @return true if successful, false if no price available
bool acquireNextPrice(PriceMessage* toCopyInto) {
    auto ts = tradingSystem.load(std::memory_order::acquire);
    if (!ts) return false;
    ts->getNextPrice(*toCopyInto);
    return true; //at the moment there is no failure case, it spin wait forever.
}

bool tradingInit() {
    tradingSystem.store(new TradingSystem(), std::memory_order_release);
    return true;
}

bool startStaticIterator() {
    auto ts = tradingSystem.load(std::memory_order::acquire);
    if (!ts) return false;
    staticIterator = ts->getMap().begin();
    return true;
}

bool nextStaticItem(StaticMessage* toCopyInto) {
    const auto ts = tradingSystem.load(std::memory_order::acquire);
    if (!ts) return false;
    if (staticIterator == ts->getMap().end()) return false;
    *toCopyInto = staticIterator->second;
    ++staticIterator;
    return true;
}
} //extern c
